package org.acme.domains.auth;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.account.Account;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.auth.dto.LoginRequest;
import org.acme.domains.employee.Employee;
import org.acme.domains.employee.EmployeeRepository;
import org.acme.domains.employee.EmployeeStatus;
import org.acme.domains.manager.Manager;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.shared.domain.CPF;
import org.acme.domains.shared.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ManagerRepository managerRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(employeeRepository, accountRepository, managerRepository);
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
        Employee employee = buildEmployee(account, EmployeeStatus.DISABLED);
        LoginRequest request = new LoginRequest(account.getEmail(), "123456");

        when(accountRepository.findByEmail(request.email())).thenReturn(Uni.createFrom().item(account));
        when(employeeRepository.findByAccountId(account.id)).thenReturn(Uni.createFrom().item(employee));

        assertThrows(NotFoundException.class, () -> authService.login(request).await().indefinitely());
    }

    @Test
    void shouldFailWhenManagerNotFound() {
        Account account = buildAccount("manager@acme.com", "123456", Role.MANAGER);
        LoginRequest request = new LoginRequest(account.getEmail(), "123456");

        when(accountRepository.findByEmail(request.email())).thenReturn(Uni.createFrom().item(account));
        when(managerRepository.findByAccountId(account.id)).thenReturn(Uni.createFrom().nullItem());

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

    private Employee buildEmployee(Account account, EmployeeStatus status) {
        Employee employee = Employee.builder("Employee", null, account).build();
        setField(employee, "active", status);
        return employee;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set test field: " + fieldName, e);
        }
    }
}
