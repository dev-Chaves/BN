package org.acme.domains.auth;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.account.Account;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.auth.dto.LoginRequest;
import org.acme.domains.auth.logincontext.EmployeeLoginContext;
import org.acme.domains.auth.logincontext.ManagerLoginContext;
import org.acme.domains.shared.domain.CPF;
import org.acme.domains.shared.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private AuthService authService;

    @Mock
    private EmployeeLoginContext employeeLoginContext;

    @Mock
    private ManagerLoginContext managerLoginContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        lenient().when(employeeLoginContext.supports()).thenReturn(Role.USER);
        lenient().when(managerLoginContext.supports()).thenReturn(Role.MANAGER);
        authService = new AuthService(accountRepository, List.of(employeeLoginContext, managerLoginContext));
    }

    @Test
    void shouldFailWhenAccountNotFound() {
        LoginRequest request = new LoginRequest("missing@acme.com", "123456");
        when(accountRepository.findByEmail(request.email())).thenReturn(Uni.createFrom().nullItem());

        assertThrows(NotFoundException.class, () -> authService.login(request).await().indefinitely());
    }

    @Test
    void shouldFailWhenPasswordIsInvalid() {
        Account account = buildAccount("user@acme.com", "123456", Role.USER);
        LoginRequest request = new LoginRequest(account.getEmail(), "wrong-password");
        when(accountRepository.findByEmail(request.email())).thenReturn(Uni.createFrom().item(account));

        assertThrows(IllegalStateException.class, () -> authService.login(request).await().indefinitely());
    }

    @Test
    void shouldFailWhenEmployeeIsDisabled() {
        Account account = buildAccount("user@acme.com", "123456", Role.USER);
        LoginRequest request = new LoginRequest(account.getEmail(), "123456");

        when(accountRepository.findByEmail(request.email())).thenReturn(Uni.createFrom().item(account));
        when(employeeLoginContext.resolve(account))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("Employee is disabled")));

        assertThrows(IllegalStateException.class, () -> authService.login(request).await().indefinitely());
    }

    @Test
    void shouldFailWhenManagerNotFound() {
        Account account = buildAccount("manager@acme.com", "123456", Role.MANAGER);
        LoginRequest request = new LoginRequest(account.getEmail(), "123456");

        when(accountRepository.findByEmail(request.email())).thenReturn(Uni.createFrom().item(account));
        when(managerLoginContext.resolve(account))
                .thenReturn(Uni.createFrom().failure(new NotFoundException("Manager not found")));

        assertThrows(NotFoundException.class, () -> authService.login(request).await().indefinitely());
    }

    private Account buildAccount(String email, String rawPassword, Role role) {
        Account account = Account.builder(
                "Test User",
                CPF.of("52998224725"),
                BcryptUtil.bcryptHash(rawPassword),
                email,
                role
        ).build();
        account.id = UUID.randomUUID();
        return account;
    }
}
