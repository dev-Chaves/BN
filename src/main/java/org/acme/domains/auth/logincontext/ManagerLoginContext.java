package org.acme.domains.auth.logincontext;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.account.Account;
import org.acme.domains.auth.TokenService;
import org.acme.domains.auth.dto.LoginContextData;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.shared.enums.Role;

@ApplicationScoped
public class ManagerLoginContext implements LoginContextResolver{

    private final ManagerRepository managerRepository;

    private final TokenService tokenService;

    public ManagerLoginContext(ManagerRepository managerRepository, TokenService tokenService) {
        this.managerRepository = managerRepository;
        this.tokenService = tokenService;
    }

    @Override
    public Role supports() {
        return Role.MANAGER;
    }

    @Override
    public Uni<LoginContextData> resolve(Account account) {
        return managerRepository.findByAccountId(account.id).onItem().ifNull().failWith(() -> new NotFoundException("Manager not found"))
                .map(manager ->
                        new LoginContextData(
                                tokenService.generateToken(account.getEmail(), manager.getCompany().id, Role.MANAGER.name()),
                                Role.MANAGER,
                                manager.id
                        ));
    }
}
