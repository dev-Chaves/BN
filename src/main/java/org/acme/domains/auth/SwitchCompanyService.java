package org.acme.domains.auth;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.domains.auth.dto.LoginResponse;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.shared.enums.Role;
import org.acme.domains.shared.security.AccessStatusGuard;

@ApplicationScoped
public class SwitchCompanyService {

    private final ManagerRepository managerRepository;
    private final TokenService tokenService;

    public SwitchCompanyService(ManagerRepository managerRepository, TokenService tokenService) {
        this.managerRepository = managerRepository;
        this.tokenService = tokenService;
    }

    @WithSession
    public Uni<LoginResponse> switchCompany(String accountEmail, Long companyId) {
        return managerRepository.findByEmailAndCompanyId(accountEmail, companyId)
                .onItem().ifNull().failWith(() -> new SecurityException("Company context is unavailable"))
                .map(AccessStatusGuard::requireActive)
                .map(manager -> new LoginResponse(tokenService.generateToken(
                        manager.getAccount().getEmail(),
                        manager.getCompany().id,
                        Role.MANAGER.name()
                )));
    }
}
