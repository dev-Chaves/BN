package com.bnfix.ubm.domains.redemption;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BenefitRedemptionRepository extends JpaRepository<BenefitRedemption, Long> {
    long countBySubscriptionId(Long subscriptionId);
}
