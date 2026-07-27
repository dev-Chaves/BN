package org.acme.domains.redemption;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BenefitRedemptionRepository implements PanacheRepository<BenefitRedemption> {
    public Uni<Long> countBySubscription(Long subscriptionId) {
        return count("subscription.id", subscriptionId);
    }
}
