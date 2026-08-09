package com.bnfix.ubm.domains.auth;

import com.bnfix.ubm.domains.auth.dto.LoginResponse;
import com.bnfix.ubm.domains.manager.Manager;
import com.bnfix.ubm.domains.manager.ManagerRepository;
import com.bnfix.ubm.domains.shared.enums.Role;
import com.bnfix.ubm.shared.security.AccessStatusGuard;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!test")
public class SwitchCompanyService {
    private final ManagerRepository managers;
    private final TokenService tokens;

    public SwitchCompanyService(ManagerRepository managers, TokenService tokens) {
        this.managers = managers;
        this.tokens = tokens;
    }

    @Transactional(readOnly = true)
    public LoginResponse switchCompany(String email, Long companyId) {
        Manager manager =
                managers.findByEmailAndCompanyId(email, companyId).orElseThrow(() -> new AuthenticationException());
        AccessStatusGuard.requireActive(manager);
        UUID accountId = manager.getAccount().id;
        return new LoginResponse(tokens.generateToken(
                manager.getAccount().getEmail(), accountId, manager.getCompany().id, Role.MANAGER.name()));
    }
}
