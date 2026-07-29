package org.acme.domains.subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.domains.benefit.Benefit;
import org.acme.domains.benefit.BenefitRepository;
import org.acme.domains.employee.Employee;
import org.acme.domains.employee.EmployeeRepository;

import java.util.List;

@ApplicationScoped
public class CompanyBenefitAssignmentService {

    private final BenefitRepository benefitRepository;
    private final EmployeeRepository employeeRepository;
    private final SubscriptionRepository subscriptionRepository;

    public CompanyBenefitAssignmentService(
            BenefitRepository benefitRepository,
            EmployeeRepository employeeRepository,
            SubscriptionRepository subscriptionRepository
    ) {
        this.benefitRepository = benefitRepository;
        this.employeeRepository = employeeRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    public Uni<Void> assignBenefitToActiveEmployees(Benefit benefit) {
        return employeeRepository.findActiveByCompanyId(benefit.getProvider().id)
                .flatMap(employees -> assignBenefit(benefit, employees));
    }

    public Uni<Void> assignActiveCompanyBenefits(Employee employee) {
        return benefitRepository.findActiveByProvider(employee.getCompany().id)
                .flatMap(benefits -> assignBenefits(employee, benefits));
    }

    private Uni<Void> assignBenefit(Benefit benefit, List<Employee> employees) {
        return Multi.createFrom().iterable(employees)
                .onItem().transformToUniAndConcatenate(employee -> ensureSubscription(employee, benefit))
                .collect().asList()
                .replaceWithVoid();
    }

    private Uni<Void> assignBenefits(Employee employee, List<Benefit> benefits) {
        return Multi.createFrom().iterable(benefits)
                .onItem().transformToUniAndConcatenate(benefit -> ensureSubscription(employee, benefit))
                .collect().asList()
                .replaceWithVoid();
    }

    private Uni<Void> ensureSubscription(Employee employee, Benefit benefit) {
        return subscriptionRepository.existsByEmployeeAndBenefit(employee.id, benefit.id)
                .flatMap(exists -> exists
                        ? Uni.createFrom().voidItem()
                        : subscriptionRepository.persist(Subscription.builder(benefit, employee).build())
                                .replaceWithVoid());
    }
}
