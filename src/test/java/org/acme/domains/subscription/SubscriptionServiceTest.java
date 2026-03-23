package org.acme.domains.subscription;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.account.Account;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.benefit.Benefit;
import org.acme.domains.benefit.BenefitRepository;
import org.acme.domains.company.Company;
import org.acme.domains.employee.Employee;
import org.acme.domains.employee.EmployeeRepository;
import org.acme.domains.shared.domain.CNPJ;
import org.acme.domains.shared.domain.CPF;
import org.acme.domains.shared.enums.Role;
import org.acme.domains.subscription.dto.CreateSubscriptionRequest;
import org.acme.domains.subscription.dto.SubscriptionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SubscriptionServiceTest {

    @Mock
    private BenefitRepository benefitRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private AccountRepository accountRepository;

    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        subscriptionService = new SubscriptionService(benefitRepository, employeeRepository, subscriptionRepository, accountRepository);
    }

    @Test
    void shouldFailWhenBenefitNotFound() {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest(100L);
        when(benefitRepository.findById(request.benefitId())).thenReturn(Uni.createFrom().nullItem());

        assertThrows(NotFoundException.class, () -> subscriptionService.subscribeToBenefit(request, "user@acme.com").await().indefinitely());
    }

    @Test
    void shouldSubscribeByJwtEmail() {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest(1L);
        Company company = Company.builder("ACME", CNPJ.of("11222333000181")).build();
        company.id = 7L;

        Account employeeAccount = Account.builder("User", CPF.of("52998224725"), "pwd", "user@acme.com", Role.USER).build();
        employeeAccount.id = UUID.randomUUID();
        Employee employee = Employee.builder("User", company, employeeAccount).build();
        employee.id = 4L;

        Company provider = Company.builder("Provider", CNPJ.of("12345678000195")).build();
        Benefit benefit = Benefit.builder("Gym", provider).description("desc").build();
        benefit.id = 1L;

        when(benefitRepository.findById(request.benefitId())).thenReturn(Uni.createFrom().item(benefit));
        when(accountRepository.findByEmail("user@acme.com")).thenReturn(Uni.createFrom().item(employeeAccount));
        when(employeeRepository.findByAccountId(employeeAccount.id)).thenReturn(Uni.createFrom().item(employee));
        when(subscriptionRepository.persist(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription subscription = invocation.getArgument(0);
            subscription.id = 50L;
            return Uni.createFrom().item(subscription);
        });

        SubscriptionResponse response = subscriptionService.subscribeToBenefit(request, "user@acme.com").await().indefinitely();
        assertEquals(50L, response.id());
        assertEquals("User", response.employeeName());
        assertEquals("Gym", response.benefitName());
    }
}
