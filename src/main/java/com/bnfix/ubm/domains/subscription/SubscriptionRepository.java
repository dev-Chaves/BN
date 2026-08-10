package com.bnfix.ubm.domains.subscription;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByEmployeeId(Long id);

    List<Subscription> findByBenefitId(Long id);

    Optional<Subscription> findByEmployeeIdAndBenefitId(Long employeeId, Long benefitId);

    default boolean existsByEmployeeAndBenefit(Long employeeId, Long benefitId) {
        return findByEmployeeIdAndBenefitId(employeeId, benefitId).isPresent();
    }

    @Query(
            "select distinct s from Subscription s join fetch s.employee e join fetch s.benefit b join fetch b.provider left join fetch b.categories where e.id=:id order by s.createdAt desc")
    List<Subscription> findByEmployeeWithBenefit(@Param("id") Long id);

    @Query(
            "select distinct s from Subscription s join fetch s.employee e join fetch s.benefit b join fetch b.provider left join fetch b.categories where s.id=:sid and e.id=:eid")
    Optional<Subscription> findOwnedWithBenefit(@Param("sid") Long subscriptionId, @Param("eid") Long employeeId);
}
