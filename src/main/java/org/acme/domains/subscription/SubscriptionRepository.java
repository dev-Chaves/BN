package org.acme.domains.subscription;

import java.util.List;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SubscriptionRepository implements PanacheRepository<Subscription> {

    public Uni<List<Subscription>> findByEmployeeId(Long employeeId) {
        return list("employee.id", employeeId);
    }

    public Uni<List<Subscription>> findByBenefitId(Long benefitId) {
        return list("benefit.id", benefitId);
    }

    public Uni<Subscription> findByEmployeeAndBenefit(Long employeeId, Long benefitId) {
        return find("employee.id = ?1 and benefit.id = ?2", employeeId, benefitId).firstResult();
    }

    public Uni<Boolean> existsByEmployeeAndBenefit(Long employeeId, Long benefitId) {
        return findByEmployeeAndBenefit(employeeId, benefitId)
                .onItem().transform(subscription -> subscription != null);
    }

}
