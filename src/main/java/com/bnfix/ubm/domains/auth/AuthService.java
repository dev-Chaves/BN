package com.bnfix.ubm.domains.auth;

import com.bnfix.ubm.domains.account.Account;
import com.bnfix.ubm.domains.account.AccountRepository;
import com.bnfix.ubm.domains.auth.dto.LoginRequest;
import com.bnfix.ubm.domains.auth.dto.LoginResponse;
import com.bnfix.ubm.domains.employee.Employee;
import com.bnfix.ubm.domains.employee.EmployeeRepository;
import com.bnfix.ubm.domains.manager.Manager;
import com.bnfix.ubm.domains.manager.ManagerRepository;
import com.bnfix.ubm.domains.shared.enums.Role;
import com.bnfix.ubm.shared.security.AccessStatusGuard;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Profile("!test")
public class AuthService {
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
    private final AccountRepository accountRepository;
    private final ManagerRepository managerRepository;
    private final EmployeeRepository employeeRepository;
    private final TokenService tokenService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(
            AccountRepository accountRepository,
            ManagerRepository managerRepository,
            EmployeeRepository employeeRepository,
            TokenService tokenService,
            BCryptPasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.managerRepository = managerRepository;
        this.employeeRepository = employeeRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Account account = accountRepository.findByEmail(email).orElse(null);
        String hash = account == null ? DUMMY_PASSWORD_HASH : account.getPassword();
        boolean valid = passwordEncoder.matches(request.password(), hash);
        if (account == null || !valid) {
            log.warn("Failed login attempt for email {}", email);
            throw new AuthenticationException();
        }

        if (account.getRole() == null) {
            log.warn("Login denied for email {} with no role", email);
            throw new AuthenticationException();
        }
        LoginResponse response =
                switch (account.getRole()) {
                    case ADMIN ->
                        new LoginResponse(tokenService.generateToken(email, account.id, null, Role.ADMIN.name()));
                    case MANAGER -> managerToken(account);
                    case USER -> employeeToken(account);
                    default -> throw new AuthenticationException();
                };
        log.info("User {} logged in as {}", email, account.getRole());
        return response;
    }

    private LoginResponse managerToken(Account account) {
        Manager manager = managerRepository.findActiveByAccountId(account.id).orElseThrow(AuthenticationException::new);
        AccessStatusGuard.requireActive(manager);
        return new LoginResponse(tokenService.generateToken(
                account.getEmail(), account.id, manager.getCompany().id, Role.MANAGER.name()));
    }

    private LoginResponse employeeToken(Account account) {
        Employee employee = employeeRepository.findByAccountId(account.id).orElseThrow(AuthenticationException::new);
        AccessStatusGuard.requireActive(employee);
        return new LoginResponse(
                tokenService.generateToken(account.getEmail(), account.id, employee.getCompany().id, Role.USER.name()));
    }
}
