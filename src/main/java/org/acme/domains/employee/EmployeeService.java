package org.acme.domains.employee;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.domains.account.Account;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.company.CompanyRepository;
import org.acme.domains.employee.dto.CreateEmployeeRequest;
import org.acme.domains.employee.dto.EmployeeResponse;
import org.acme.domains.employee.dto.UpdateEmployeeRequest;
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

    public Uni<EmployeeResponse> createEmployee(CreateEmployeeRequest request, String managerEmail) {

        Account account = Account.builder(
                request.name(),
                CPF.of(request.cpf()),
                request.password(),
                request.email(),
                Role.USER).build();

        return companyRepository.findByManagerEmail(managerEmail)
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

    public Uni<EmployeeResponse> disabledEmployee(Long id, String managerEmail) {

        return managerRepository.findByEmail(managerEmail).onItem()
                .transform(manager -> manager.getCompany().id)
                .call(()-> employeeRepository.findById(id).onItem()
                        .transform(employee -> employee.disableEmployee(DISABLED)));

    }
//
//    @WithTransaction
//    public Uni<EmployeeResponse> disableEmployee(Long employeeId, String managerEmail) {
//        return managerRepository.findByEmail(managerEmail)
//                .onItem().ifNull().failWith(() -> new NotFoundException("Manager nao encontrado"))
//                .flatMap(manager ->
//                        employeeRepository.findById(employeeId)
//                                .onItem().ifNull().failWith(() -> new NotFoundException("Employee nao encontrado"))
//                                .flatMap(employee -> validateSameCompany(manager, employee))
//                                .flatMap(this::validateNotDisabled)
//                                .invoke(employee -> employee.disableEmployee(EmployeeStatus.DISABLED)) // side-effect
//                                .onItem().transform(this::toResponse)
//                );
//    }
//
//    private Uni<Employee> validateSameCompany(Manager manager, Employee employee) {
//        if (!manager.getCompany().id.equals(employee.getCompany().id)) {
//            return Uni.createFrom().failure(new ForbiddenException("Manager e employee nao sao da mesma empresa"));
//        }
//        return Uni.createFrom().item(employee);
//    }
//
//    private Uni<Employee> validateNotDisabled(Employee employee) {
//        if (employee.getActive() == EmployeeStatus.DISABLED) {
//            return Uni.createFrom().failure(new ConflictException("Employee ja esta desativado"));
//        }
//        return Uni.createFrom().item(employee);
//    }





}
