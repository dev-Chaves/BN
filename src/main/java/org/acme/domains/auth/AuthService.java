package org.acme.domains.auth;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.account.Account;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.auth.dto.LoginRequest;
import org.acme.domains.auth.dto.LoginResponse;
import org.acme.domains.auth.logincontext.LoginContextResolver;
import org.acme.domains.company.CompanyRepository;
import org.acme.domains.employee.Employee;
import org.acme.domains.employee.EmployeeRepository;
import org.acme.domains.employee.EmployeeStatus;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.shared.enums.Role;

import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class AuthService {

    private final AccountRepository accountRepository;
    private final Map<Role, LoginContextResolver> resolvers;

    public AuthService(AccountRepository accountRepository, Map<Role, LoginContextResolver> resolvers) {
        this.accountRepository = accountRepository;
        this.resolvers = resolvers;
    }

    public Uni<LoginResponse> login(LoginRequest req) {
        return validateAccount(req.email())
                .flatMap(acc -> validatePassword(req.password(), acc).replaceWith(acc)
                .flatMap(account -> resolvers.get(acc.getRole()).resolve(acc)))
                .map(ctx -> new LoginResponse(ctx.token()));
    }

    private Uni<Account> validateAccount(String email) {
        return accountRepository.findByEmail(email).onItem().ifNull().failWith(() -> new NotFoundException("Account not found"));
    }

    private Uni<Void> validatePassword(String password, Account account) {

        if(!BcryptUtil.matches(password, account.getPassword())){
            return Uni.createFrom().failure(new IllegalStateException("Invalid credentials"));
        }
        return Uni.createFrom().voidItem();
    }

}
