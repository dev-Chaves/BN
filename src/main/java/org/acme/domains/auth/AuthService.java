package org.acme.domains.auth;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.account.Account;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.auth.dto.LoginContextData;
import org.acme.domains.auth.dto.LoginRequest;
import org.acme.domains.auth.dto.LoginResponse;
import org.acme.domains.auth.logincontext.LoginContextResolver;
import org.acme.domains.shared.enums.Role;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class AuthService {

    private final AccountRepository accountRepository;
    private final Map<Role, LoginContextResolver> resolvers;

    public AuthService(AccountRepository accountRepository, List<LoginContextResolver> resolverList) {
        this.accountRepository = accountRepository;
        this.resolvers = resolverList.stream()
                .collect(Collectors.toMap(LoginContextResolver::supports, Function.identity()));
    }

    public Uni<LoginResponse> login(LoginRequest req) {
        return validateAccount(req.email())
                .flatMap(acc -> validatePassword(req.password(), acc).replaceWith(acc)
                        .flatMap(account -> resolveByRole(acc)))
                .map(ctx -> new LoginResponse(ctx.token()));
    }

    private Uni<LoginContextData> resolveByRole(Account account) {
        LoginContextResolver resolver = resolvers.get(account.getRole());
        if (resolver == null) {
            return Uni.createFrom().failure(new IllegalStateException("No login resolver for role: " + account.getRole()));
        }
        return resolver.resolve(account);
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
