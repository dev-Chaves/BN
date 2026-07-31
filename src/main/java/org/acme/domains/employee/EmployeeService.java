package org.acme.domains.employee;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.account.Account;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.company.CompanyRepository;
import org.acme.domains.employee.dto.CreateEmployeeRequest;
import org.acme.domains.employee.dto.EmployeeResponse;
import org.acme.domains.employee.dto.UpdateEmployeeRequest;
import org.acme.domains.manager.Manager;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.shared.domain.CPF;
import org.acme.domains.shared.enums.Role;
import org.acme.domains.shared.security.TenantGuard;
import org.acme.domains.subscription.CompanyBenefitAssignmentService;
import org.acme.domains.redemption.RedemptionTokenRepository;
import org.jboss.logging.Logger;

import java.util.List;

import static org.acme.domains.employee.EmployeeStatus.ACTIVE;
import static org.acme.domains.employee.EmployeeStatus.DISABLED;

@ApplicationScoped
public class EmployeeService {
    private static final Logger LOG = Logger.getLogger(EmployeeService.class);

    private final CompanyRepository companyRepository;

    private final AccountRepository accountRepository;

    private final ManagerRepository managerRepository;

    private final EmployeeRepository employeeRepository;

    private final TenantGuard tenantGuard;

    private final CompanyBenefitAssignmentService companyBenefitAssignmentService;
    private final RedemptionTokenRepository redemptionTokenRepository;

    public EmployeeService(
            CompanyRepository companyRepository,
            AccountRepository accountRepository,
            ManagerRepository managerRepository,
            EmployeeRepository employeeRepository,
            TenantGuard tenantGuard,
            CompanyBenefitAssignmentService companyBenefitAssignmentService,
            RedemptionTokenRepository redemptionTokenRepository
    ) {
        this.companyRepository = companyRepository;
        this.accountRepository = accountRepository;
        this.managerRepository = managerRepository;
        this.employeeRepository = employeeRepository;
        this.tenantGuard = tenantGuard;
        this.companyBenefitAssignmentService = companyBenefitAssignmentService;
        this.redemptionTokenRepository = redemptionTokenRepository;
    }

    @WithTransaction
    public Uni<EmployeeResponse> createEmployee(CreateEmployeeRequest request, String managerEmail, Long companyId) {

        return accountRepository.findByEmail(request.email())
                .flatMap(existing -> {
                    if (existing != null) {
                        LOG.warnf("Email already exists email=%s", request.email());
                        return Uni.createFrom().failure(new IllegalStateException("Email already in use"));
                    }
                    return accountRepository.findByCPF(request.cpf());
                })
                .flatMap(existing -> {
                    if (existing != null) return Uni.createFrom().failure(new IllegalStateException("CPF already in use"));
                    return validateManager(managerEmail);
                })
                .flatMap(manager -> tenantGuard.verifyManagerCompanyAccess(manager, companyId))
                .onItem().transform(company -> {
                    Account account = Account.builder(request.name(), CPF.of(request.cpf()),
                            BcryptUtil.bcryptHash(request.password()), request.email(), Role.USER).build();
                    return Employee.builder(request.name(), company, account).build();
                })
                .call(employee -> accountRepository.persist(employee.getAccount()))
                .call(employeeRepository::persist).onItem().transform(this::toResponse);
    }

    @WithTransaction
    public Uni<EmployeeResponse> disabledEmployee(Long id, String managerEmail) {

        return validateManager(managerEmail)
                .flatMap(manager ->
                        employeeRepository.findById(id).onItem().ifNull().failWith(()-> new NotFoundException("Employee not Found"))
                                .flatMap(employee -> tenantGuard.verifyManagerEmployeeAccess(manager, employee))
                                .flatMap(this::validateNotDisabled)
                                .flatMap(employee -> {
                                    employee.disable();
                                    return redemptionTokenRepository.revokeActiveByEmployee(employee.id)
                                            .flatMap(ignored -> employeeRepository.persist(employee));
                                })
                                .map(this::toResponse)
                );

    }

    @WithTransaction
    public Uni<EmployeeResponse> activateEmployee(Long id, String managerEmail) {
        return validateManager(managerEmail)
                .flatMap(manager ->
                        employeeRepository.findById(id).onItem().ifNull().failWith(() -> new NotFoundException("Employee not Found"))
                                .flatMap(employee -> tenantGuard.verifyManagerEmployeeAccess(manager, employee))
                                .flatMap(this::validateNotActive)
                                .flatMap(employee -> {
                                    employee.active();
                                    return employeeRepository.persist(employee);
                                })
                                .flatMap(employee -> companyBenefitAssignmentService
                                        .assignActiveCompanyBenefits(employee)
                                        .replaceWith(employee))
                                .map(this::toResponse)
                );
    }

    @WithTransaction
    public Uni<EmployeeResponse> updateEmployee(Long id, UpdateEmployeeRequest request, String managerEmail) {
        return validateManager(managerEmail)
                .flatMap(manager ->
                        employeeRepository.findById(id).onItem().ifNull().failWith(() -> new NotFoundException("Employee not Found"))
                                .flatMap(employee -> tenantGuard.verifyManagerEmployeeAccess(manager, employee))
                                .map(employee -> {
                                    employee.update(request.name());
                                    return employee;
                                })
                                .flatMap(employeeRepository::persist)
                                .map(this::toResponse)
                );
    }

    @WithSession
    public Uni<List<EmployeeResponse>> listByTenant(String managerEmail, Long companyId) {
        return listByTenant(managerEmail, companyId, 0, 50);
    }

    @WithSession
    public Uni<List<EmployeeResponse>> listByTenant(String managerEmail, Long companyId, int page, int size) {
        return validateManager(managerEmail)
                .flatMap(manager -> tenantGuard.verifyManagerCompanyAccess(manager, companyId))
                .flatMap(company -> employeeRepository.findByCompanyId(company.id, normalizePage(page), normalizeSize(size)))
                .map(employees -> employees.stream().map(this::toResponse).toList());
    }

    private int normalizePage(int page) { return Math.max(0, page); }
    private int normalizeSize(int size) { return Math.max(1, Math.min(size, 100)); }

    private Uni<Manager> validateManager(String managerEmail) {
        return managerRepository.findByEmail(managerEmail).onItem().ifNull().failWith(() -> {
            LOG.warnf("Manager not found managerEmail=%s", managerEmail);
            return new NotFoundException("Manager not Found");
        });
    }

    private Uni<Employee> validateNotDisabled(Employee employee) {
        if(employee.getActive().equals(DISABLED)){
            LOG.warnf("Disable employee skipped employeeId=%d status=%s", employee.id, employee.getActive());
            return Uni.createFrom().failure(new NotFoundException("Employee is already disabled"));
        }
        return Uni.createFrom().item(employee);
    }

    private Uni<Employee> validateNotActive(Employee employee) {
        if(employee.getActive().equals(ACTIVE)){
            LOG.warnf("Activate employee skipped employeeId=%d status=%s", employee.id, employee.getActive());
            return Uni.createFrom().failure(new IllegalStateException("Employee is already active"));
        }
        return Uni.createFrom().item(employee);
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.id,
                employee.getName(),
                employee.getCompany().id,
                employee.getActive(),
                employee.getCreatedAt()
        );
    }




}
