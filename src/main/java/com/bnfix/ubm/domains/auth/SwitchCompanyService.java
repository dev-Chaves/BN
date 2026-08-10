package com.bnfix.ubm.domains.auth;

import com.bnfix.ubm.domains.auth.dto.LoginResponse;
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

    public SwitchCompanyService(ManagerRepository managerRepository, TokenService tokenService) {
        this.managerRepository = managerRepository;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public LoginResponse switchCompany(String email, Long companyId) {
        Manager manager = managerRepository
                .findByEmailAndCompanyId(email, companyId)
                .orElseThrow(() -> new AuthenticationException());
        AccessStatusGuard.requireActive(manager);
        UUID accountId = manager.getAccount().id;
        log.info("Manager {} switched to company {}", manager.getAccount().getEmail(), companyId);
        return new LoginResponse(tokenService.generateToken(
                manager.getAccount().getEmail(), accountId, manager.getCompany().id, Role.MANAGER.name()));
    }
}
