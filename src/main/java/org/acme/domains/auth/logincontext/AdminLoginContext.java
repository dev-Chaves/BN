package org.acme.domains.auth.logincontext;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.domains.account.Account;
import org.acme.domains.auth.TokenService;
import org.acme.domains.auth.dto.LoginContextData;
import org.acme.domains.shared.enums.Role;

@ApplicationScoped
public class AdminLoginContext implements LoginContextResolver {
    private final TokenService tokenService;

    public AdminLoginContext(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public Role supports() {
        return Role.ADMIN;
    }

    @Override
    public Uni<LoginContextData> resolve(Account account) {
        return Uni.createFrom().item(new LoginContextData(
                tokenService.generateToken(account.getEmail(), null, Role.ADMIN.name()),
                Role.ADMIN,
                null
        ));
    }
}
