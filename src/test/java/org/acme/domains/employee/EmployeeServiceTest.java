package org.acme.domains.employee;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.account.Account;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.company.Company;
import org.acme.domains.company.CompanyRepository;
import org.acme.domains.employee.dto.CreateEmployeeRequest;
import org.acme.domains.employee.dto.EmployeeResponse;
import org.acme.domains.manager.Manager;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.shared.domain.CNPJ;
import org.acme.domains.shared.domain.CPF;
import org.acme.domains.shared.enums.Role;
import org.acme.domains.shared.security.TenantGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class EmployeeServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TenantGuard tenantGuard;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        employeeService = new EmployeeService(
                companyRepository,
                accountRepository,
                managerRepository,
                employeeRepository,
                tenantGuard
        );
    }

    @Test
    void shouldFailWhenManagerNotFound() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Employee",
                "52998224725",
                "employee@acme.com",
                "123456",
                10L
        );

        when(managerRepository.findByEmail("manager@acme.com")).thenReturn(Uni.createFrom().nullItem());

        assertThrows(NotFoundException.class, () ->
                employeeService.createEmployee(request, "manager@acme.com", 10L).await().indefinitely());
    }

    @Test
    void shouldFailWhenTenantValidationFails() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Employee",
                "52998224725",
                "employee@acme.com",
                "123456",
                99L
        );
        Company managerCompany = Company.builder("ACME", CNPJ.of("11222333000181")).build();
        managerCompany.id = 10L;
        Manager manager = Manager.builder(
                "Manager",
                managerCompany,
                Account.builder("Manager", CPF.of("52998224725"), "pwd", "manager@acme.com", Role.MANAGER).build()
        ).build();

        when(managerRepository.findByEmail("manager@acme.com")).thenReturn(Uni.createFrom().item(manager));
        when(tenantGuard.verifyTenant(10L, 99L)).thenReturn(Uni.createFrom().failure(new SecurityException("Unauthorized access: Tenant mismatch")));

        assertThrows(SecurityException.class, () ->
                employeeService.createEmployee(request, "manager@acme.com", 99L).await().indefinitely());
    }

    @Test
    void shouldCreateEmployee() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Employee",
                "52998224725",
                "employee@acme.com",
                "123456",
                10L
        );
        Company company = Company.builder("ACME", CNPJ.of("11222333000181")).build();
        company.id = 10L;
        Company managerCompany = Company.builder("ACME", CNPJ.of("11222333000181")).build();
        managerCompany.id = 10L;
        Manager manager = Manager.builder(
                "Manager",
                managerCompany,
                Account.builder("Manager", CPF.of("52998224725"), "pwd", "manager@acme.com", Role.MANAGER).build()
        ).build();

        when(managerRepository.findByEmail("manager@acme.com")).thenReturn(Uni.createFrom().item(manager));
        when(tenantGuard.verifyTenant(10L, 10L)).thenReturn(Uni.createFrom().item(company));
        when(accountRepository.persist(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.id = UUID.randomUUID();
            return Uni.createFrom().item(account);
        });
        when(employeeRepository.persist(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.id = 50L;
            return Uni.createFrom().item(employee);
        });

        EmployeeResponse response = employeeService.createEmployee(request, "manager@acme.com", 10L).await().indefinitely();

        assertEquals(50L, response.id());
        assertEquals("Employee", response.name());
        assertEquals(10L, response.companyId());
    }
}
