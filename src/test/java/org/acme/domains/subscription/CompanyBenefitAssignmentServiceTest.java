package org.acme.domains.subscription;

import io.smallrye.mutiny.Uni;
import org.acme.domains.account.Account;
import org.acme.domains.benefit.Benefit;
import org.acme.domains.benefit.BenefitRepository;
import org.acme.domains.company.Company;
import org.acme.domains.employee.Employee;
import org.acme.domains.employee.EmployeeRepository;
import org.acme.domains.shared.domain.CNPJ;
import org.acme.domains.shared.domain.CPF;
import org.acme.domains.shared.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyBenefitAssignmentServiceTest {

    @Mock
    private BenefitRepository benefitRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private CompanyBenefitAssignmentService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new CompanyBenefitAssignmentService(
                benefitRepository,
                employeeRepository,
                subscriptionRepository
        );
    }

    @Test
    void shouldAssignActivatedBenefitToActiveEmployees() {
        Company company = company();
        Benefit benefit = benefit(company);
        Employee employee = employee(company);

        when(employeeRepository.findActiveByCompanyId(company.id))
                .thenReturn(Uni.createFrom().item(List.of(employee)));
        when(subscriptionRepository.existsByEmployeeAndBenefit(employee.id, benefit.id))
                .thenReturn(Uni.createFrom().item(false));
        when(subscriptionRepository.persist(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription subscription = invocation.getArgument(0);
            return Uni.createFrom().item(subscription);
        });

        service.assignBenefitToActiveEmployees(benefit).await().indefinitely();

        verify(subscriptionRepository).persist(any(Subscription.class));
    }

    @Test
    void shouldNotDuplicateExistingCompanyBenefitAssignment() {
        Company company = company();
        Benefit benefit = benefit(company);
        Employee employee = employee(company);

        when(benefitRepository.findActiveByProvider(company.id))
                .thenReturn(Uni.createFrom().item(List.of(benefit)));
        when(subscriptionRepository.existsByEmployeeAndBenefit(employee.id, benefit.id))
                .thenReturn(Uni.createFrom().item(true));

        service.assignActiveCompanyBenefits(employee).await().indefinitely();

        verify(subscriptionRepository, never()).persist(any(Subscription.class));
    }

    private Company company() {
        Company company = Company.builder("ACME", CNPJ.of("11222333000181")).build();
        company.id = 10L;
        return company;
    }

    private Benefit benefit(Company company) {
        Benefit benefit = Benefit.builder("Gym", company).description("Corporate gym").build();
        benefit.id = 20L;
        return benefit;
    }

    private Employee employee(Company company) {
        Employee employee = Employee.builder(
                "Employee",
                company,
                Account.builder(
                        "Employee",
                        CPF.of("52998224725"),
                        "pwd",
                        "employee@acme.com",
                        Role.USER
                ).build()
        ).build();
        employee.id = 30L;
        return employee;
    }
}
