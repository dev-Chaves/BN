package com.bnfix.ubm.domains.auth;

import com.bnfix.ubm.domains.auth.dto.AuthMeResponse;
import com.bnfix.ubm.domains.auth.dto.AuthResult;
import com.bnfix.ubm.domains.manager.Manager;
import com.bnfix.ubm.domains.manager.ManagerRepository;
import com.bnfix.ubm.domains.shared.enums.Role;
import com.bnfix.ubm.shared.security.AccessStatusGuard;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Profile("!test")
public class SwitchCompanyService {
    private final ManagerRepository managerRepository;
    private final TokenService tokenService;
    private final AuthMeService authMeService;

    public SwitchCompanyService(
            ManagerRepository managerRepository, TokenService tokenService, AuthMeService authMeService) {
        this.managerRepository = managerRepository;
        this.tokenService = tokenService;
        this.authMeService = authMeService;
    }

    @Transactional(readOnly = true)
    public AuthResult switchCompany(String email, Long companyId) {
        Manager manager = managerRepository
                .findByEmailAndCompanyId(email, companyId)
                .orElseThrow(() -> new AuthenticationException());
        AccessStatusGuard.requireActive(manager);
        UUID accountId = manager.getAccount().id;
        String token = tokenService.generateToken(
                manager.getAccount().getEmail(), accountId, manager.getCompany().id, Role.MANAGER.name());
        AuthMeResponse user = authMeService.build(manager.getAccount(), Role.MANAGER.name(), manager.getCompany());
        log.info("Manager {} switched to company {}", manager.getAccount().getEmail(), companyId);
        return new AuthResult(token, user);
    }
}
