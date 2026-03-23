package org.acme.domains.subscription;

import java.util.List;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SubscriptionRepository implements PanacheRepository<Subscription> {

    public Uni<List<Subscription>> findByEmployeeId(Long employeeId) {
        return list("employee.profileId", employeeId);
    }

    public Uni<List<Subscription>> findByBenefitId(Long benefitId) {
        return list("benefit.profileId", benefitId);
    }

}
