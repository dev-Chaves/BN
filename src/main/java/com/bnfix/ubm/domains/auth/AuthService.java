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
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!test")
public class AuthService {
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
    private final AccountRepository accounts;
    private final ManagerRepository managers;
    private final EmployeeRepository employees;
    private final TokenService tokens;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(
            AccountRepository accounts,
            ManagerRepository managers,
            EmployeeRepository employees,
            TokenService tokens,
            BCryptPasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.managers = managers;
        this.employees = employees;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Account account = accounts.findByEmail(email).orElse(null);
        String hash = account == null ? DUMMY_PASSWORD_HASH : account.getPassword();
        boolean valid = passwordEncoder.matches(request.password(), hash);
        if (account == null || !valid) throw new AuthenticationException();

        if (account.getRole() == null) throw new AuthenticationException();
        return switch (account.getRole()) {
            case ADMIN -> new LoginResponse(tokens.generateToken(email, account.id, null, Role.ADMIN.name()));
            case MANAGER -> managerToken(account);
            case USER -> employeeToken(account);
            default -> throw new AuthenticationException();
        };
    }

    private LoginResponse managerToken(Account account) {
        Manager manager = managers.findActiveByAccountId(account.id).orElseThrow(AuthenticationException::new);
        AccessStatusGuard.requireActive(manager);
        return new LoginResponse(
                tokens.generateToken(account.getEmail(), account.id, manager.getCompany().id, Role.MANAGER.name()));
    }

    private LoginResponse employeeToken(Account account) {
        Employee employee = employees.findByAccountId(account.id).orElseThrow(AuthenticationException::new);
        AccessStatusGuard.requireActive(employee);
        return new LoginResponse(
                tokens.generateToken(account.getEmail(), account.id, employee.getCompany().id, Role.USER.name()));
    }
}
