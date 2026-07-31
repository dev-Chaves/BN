package org.acme.domains.auth;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.acme.domains.account.Account;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.auth.dto.LoginContextData;
import org.acme.domains.auth.dto.LoginRequest;
import org.acme.domains.auth.dto.LoginResponse;
import org.acme.domains.auth.logincontext.LoginContextResolver;
import org.acme.domains.shared.enums.Role;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class AuthService {
    private static final Logger LOG = Logger.getLogger(AuthService.class);
    private static final String DUMMY_PASSWORD_HASH = BcryptUtil.bcryptHash("benefix-invalid-login-password");

    private final AccountRepository accountRepository;
    private final Map<Role, LoginContextResolver> resolvers;

    @Inject
    public AuthService(AccountRepository accountRepository, Instance<LoginContextResolver> resolverList) {
        this(accountRepository, resolverList.stream().toList());
    }

    AuthService(AccountRepository accountRepository, List<LoginContextResolver> resolverList) {
        this.accountRepository = accountRepository;
        this.resolvers = resolverList.stream()
                .collect(Collectors.toMap(LoginContextResolver::supports, Function.identity()));
    }

    @WithSession
    public Uni<LoginResponse> login(LoginRequest req) {
        String normalizedEmail = normalizeEmail(req.email());
        return accountRepository.findByEmail(normalizedEmail)
                .flatMap(account -> validateCredentials(req.password(), account))
                .flatMap(this::resolveByRole)
                .onFailure(SecurityException.class).transform(ignored -> new AuthenticationException())
                .map(ctx -> new LoginResponse(ctx.token()));
    }

    private Uni<LoginContextData> resolveByRole(Account account) {
        LoginContextResolver resolver = resolvers.get(account.getRole());
        if (resolver == null) {
            LOG.errorf("Login resolver not found role=%s", account.getRole());
            return Uni.createFrom().failure(new AuthenticationException());
        }
        return resolver.resolve(account);
    }

    private Uni<Account> validateCredentials(String password, Account account) {
        String hash = account == null ? DUMMY_PASSWORD_HASH : account.getPassword();
        boolean matches = BcryptUtil.matches(password, hash);
        if (account == null || !matches) {
            LOG.warn("Invalid login credentials");
            return Uni.createFrom().failure(new AuthenticationException());
        }
        return Uni.createFrom().item(account);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

}
