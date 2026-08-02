package org.acme.domains.benefitrequest;

import io.smallrye.mutiny.Uni;
import org.acme.domains.account.Account;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.benefit.Benefit;
import org.acme.domains.benefit.BenefitRepository;
import org.acme.domains.company.Company;
import org.acme.domains.employee.Employee;
import org.acme.domains.employee.EmployeeRepository;
import org.acme.domains.manager.Manager;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.shared.domain.CNPJ;
import org.acme.domains.shared.domain.CPF;
import org.acme.domains.shared.enums.Role;
import org.acme.domains.subscription.Subscription;
import org.acme.domains.subscription.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BenefitAccessRequestServiceTest {

    @Mock AccountRepository accountRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock ManagerRepository managerRepository;
    @Mock BenefitRepository benefitRepository;
    @Mock BenefitAccessRequestRepository requestRepository;
    @Mock SubscriptionRepository subscriptionRepository;

    private BenefitAccessRequestService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new BenefitAccessRequestService(
                accountRepository,
                employeeRepository,
                managerRepository,
                benefitRepository,
                requestRepository,
                subscriptionRepository
        );
    }

    @Test
    void shouldNotApproveARequestForADisabledEmployee() {
        Company provider = activeCompany(10L, "Provider", "11222333000181");
        Company client = activeCompany(20L, "Client", "12345678000195");
        Manager manager = Manager.builder("Manager", provider, managerAccount()).build();
        manager.id = 30L;
        manager.onCreate();

        Employee disabledEmployee = Employee.builder("Employee", client, employeeAccount()).build();
        disabledEmployee.id = 40L;

        Benefit benefit = Benefit.builder("Gym", provider).description("Gym access").build();
        benefit.id = 50L;
        benefit.activeBenefit();

        BenefitAccessRequest request = new BenefitAccessRequest(disabledEmployee, benefit);
        request.id = 60L;
        request.onCreate();

        when(managerRepository.findByEmailAndCompanyId("manager@provider.com", 10L))
                .thenReturn(Uni.createFrom().item(manager));
        when(requestRepository.findByIdWithRelations(60L))
                .thenReturn(Uni.createFrom().item(request));

        assertThrows(SecurityException.class, () -> service.approve(
                "manager@provider.com",
                10L,
                60L
        ).await().indefinitely());

        verify(subscriptionRepository, never()).existsByEmployeeAndBenefit(any(), any());
        verify(subscriptionRepository, never()).persist(any(Subscription.class));
    }

    private Company activeCompany(Long id, String name, String cnpj) {
        Company company = Company.builder(name, CNPJ.of(cnpj)).build();
        company.id = id;
        company.onCreate();
        return company;
    }

    private Account managerAccount() {
        return Account.builder(
                "Manager",
                CPF.of("52998224725"),
                "password",
                "manager@provider.com",
                Role.MANAGER
        ).build();
    }

    private Account employeeAccount() {
        return Account.builder(
                "Employee",
                CPF.of("11144477735"),
                "password",
                "employee@client.com",
                Role.USER
        ).build();
    }
}
