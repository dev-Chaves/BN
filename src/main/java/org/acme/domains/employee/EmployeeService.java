package org.acme.domains.employee;

import io.quarkus.elytron.security.common.BcryptUtil;
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

import static org.acme.domains.employee.EmployeeStatus.DISABLED;

@ApplicationScoped
public class EmployeeService {

    private final CompanyRepository companyRepository;

    private final AccountRepository accountRepository;

    private final ManagerRepository managerRepository;

    private final EmployeeRepository employeeRepository;

    public EmployeeService(CompanyRepository companyRepository, AccountRepository accountRepository, ManagerRepository managerRepository, EmployeeRepository employeeRepository) {
        this.companyRepository = companyRepository;
        this.accountRepository = accountRepository;
        this.managerRepository = managerRepository;
        this.employeeRepository = employeeRepository;
    }

    public Uni<EmployeeResponse> createEmployee(CreateEmployeeRequest request, String managerEmail, Long companyId) {

        String hashPassword = BcryptUtil.bcryptHash(request.password());

        Account account = Account.builder(
                request.name(),
                CPF.of(request.cpf()),
                hashPassword,
                request.email(),
                Role.USER).build();

        return companyRepository.findById(companyId)
                .onItem()
                .transform(companySalva -> {
                    return Employee.builder(request.name(), companySalva,  account).build();
                })

                .call(() -> accountRepository.persist(account))

                .call(employeeRepository::persist)

                .onItem().transform(employee -> new EmployeeResponse(
                        employee.id,
                        employee.getName(),
                        employee.getCompany().id,
                        employee.getActive(),
                        employee.getCreatedAt()));


    }

    @WithTransaction
    public Uni<EmployeeResponse> disabledEmployee(Long id, String managerEmail) {

        return managerRepository.findByEmail(managerEmail).onItem().ifNull().failWith(()-> new NotFoundException("Manager not Found"))
                .flatMap(manager ->
                        employeeRepository.findById(id).onItem().ifNull().failWith(()-> new NotFoundException("Employee not Found"))
                                .flatMap(employee -> validateSameCompany(manager, employee))
                                .flatMap(this::validateNotDisabled)
                                .flatMap(employee -> {
                                    employee.disableEmployee(DISABLED);
                                    return employeeRepository.persist(employee);
                                })
                                .map(employee -> new EmployeeResponse(
                                        employee.id,
                                        employee.getName(),
                                        employee.getCompany().id,
                                        employee.getActive(),
                                        employee.getCreatedAt()
                                ))
                );

    }

    private Uni<Employee> validateSameCompany(Manager manager, Employee employee) {
        if (!manager.getCompany().id.equals(employee.getCompany().id)) {
            return Uni.createFrom().failure(new NotFoundException("Manager and Employee are not the same company"));
        }
        return Uni.createFrom().item(employee);
    }

    private Uni<Employee> validateNotDisabled(Employee employee) {
        if(employee.getActive().equals(DISABLED)){
            return Uni.createFrom().failure(new NotFoundException("Employee is already disabled"));
        }
        return Uni.createFrom().item(employee);
    }




}
