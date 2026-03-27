package org.acme.domains.auth.logincontext;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.account.Account;
import org.acme.domains.auth.TokenService;
import org.acme.domains.auth.dto.LoginContextData;
import org.acme.domains.employee.Employee;
import org.acme.domains.employee.EmployeeRepository;
import org.acme.domains.employee.EmployeeStatus;
import org.acme.domains.shared.enums.Role;

@ApplicationScoped
public class EmployeeLoginContext implements LoginContextResolver{

    private final EmployeeRepository employeeRepository;

    private final TokenService tokenService;

    public EmployeeLoginContext(EmployeeRepository employeeRepository, TokenService tokenService) {
        this.employeeRepository = employeeRepository;
        this.tokenService = tokenService;
    }

    @Override
    public Role supports() {
        return Role.USER;
    }

    @Override
    public Uni<LoginContextData> resolve(Account account) {
        return employeeRepository.findByAccountId(account.id).onItem().ifNull().failWith(()-> new NotFoundException("Employee not found"))
                .flatMap(this::verifyDisabledEmployee)
                .map(employee -> new LoginContextData(
                        tokenService.generateToken(account.getEmail(), employee.getCompany().id, Role.USER.name()),
                        Role.USER,
                        employee.id
                ));
    }

    private Uni<Employee> verifyDisabledEmployee(Employee employee) {
        if(employee.getActive().equals(EmployeeStatus.DISABLED)){
            return Uni.createFrom().failure(new IllegalStateException("Employee is disabled"));
        }
        return Uni.createFrom().item(employee);
    }
}
