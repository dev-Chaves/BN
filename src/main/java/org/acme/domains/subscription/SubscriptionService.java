package org.acme.domains.subscription;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.benefit.Benefit;
import org.acme.domains.benefit.BenefitRepository;
import org.acme.domains.employee.Employee;
import org.acme.domains.employee.EmployeeRepository;
import org.acme.domains.subscription.dto.CreateSubscriptionRequest;
import org.acme.domains.subscription.dto.SubscriptionResponse;

@ApplicationScoped
public class SubscriptionService {

    private final BenefitRepository benefitRepository;
    private final EmployeeRepository employeeRepository;

    public SubscriptionService(BenefitRepository benefitRepository, EmployeeRepository employeeRepository) {
        this.benefitRepository = benefitRepository;
        this.employeeRepository = employeeRepository;
    }

    public Uni<SubscriptionResponse> subscribeToBenefit(CreateSubscriptionRequest request, Long id){

        return benefitRepository.findById(request.benefitId()).onItem().ifNull().failWith(new NotFoundException("Employee not found"))
                .flatMap(benefit ->
                        employeeRepository.findById(id)
                .onItem().ifNull().failWith(new NotFoundException("Benefit not found"))
                .flatMap(employee -> createSubscription(benefit, employee)))
                .map(subscription -> new SubscriptionResponse(
                        subscription.id,
                        subscription.getBenefit().getName(),
                        subscription.getEmployee().getName(),
                        subscription.getCreatedAt()
                ));

    }

    private Uni<Subscription> createSubscription(Benefit benefit, Employee employee){
        return Uni.createFrom().item(Subscription.builder(benefit, employee).build());
    }
}
