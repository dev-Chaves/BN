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

        return managerRepository.

    }



}
