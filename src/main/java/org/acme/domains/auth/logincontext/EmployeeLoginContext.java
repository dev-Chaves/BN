package org.acme.domains.auth.logincontext;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.domains.account.Account;
import org.acme.domains.auth.TokenService;
import org.acme.domains.auth.dto.LoginContextData;
import org.acme.domains.employee.EmployeeRepository;
import org.acme.domains.auth.AuthenticationException;
import org.acme.domains.shared.security.AccessStatusGuard;
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
        return employeeRepository.findByAccountId(account.id)
                .onItem().ifNull().failWith(AuthenticationException::new)
                .map(AccessStatusGuard::requireActive)
                .map(employee -> new LoginContextData(
                        tokenService.generateToken(account.getEmail(), employee.getCompany().id, Role.USER.name()),
                        Role.USER,
                        employee.id
                ));
    }

}
