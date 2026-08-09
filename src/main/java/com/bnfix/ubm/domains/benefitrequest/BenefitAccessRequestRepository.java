package com.bnfix.ubm.domains.benefitrequest;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface BenefitAccessRequestRepository extends JpaRepository<BenefitAccessRequest, Long> {
    @Query(
            "select r from BenefitAccessRequest r join fetch r.employee e join fetch e.company join fetch r.benefit b join fetch b.provider where e.id=:employee order by r.requestedAt desc")
    List<BenefitAccessRequest> findByEmployee(@Param("employee") Long employee);

    @Query(
            "select r from BenefitAccessRequest r join fetch r.employee e join fetch e.company join fetch r.benefit b join fetch b.provider where b.provider.id=:provider and r.status=com.bnfix.ubm.domains.benefitrequest.BenefitAccessRequestStatus.PENDING order by r.requestedAt")
    List<BenefitAccessRequest> findPendingByProvider(@Param("provider") Long provider);

    @Query(
            "select r from BenefitAccessRequest r join fetch r.employee e join fetch e.company join fetch r.benefit b join fetch b.provider where r.id=:id")
    Optional<BenefitAccessRequest> findByIdWithRelations(@Param("id") Long id);

    @Query(
            "select r from BenefitAccessRequest r where r.employee.id=:employee and r.benefit.id=:benefit and r.status=com.bnfix.ubm.domains.benefitrequest.BenefitAccessRequestStatus.PENDING")
    Optional<BenefitAccessRequest> findPending(@Param("employee") Long employee, @Param("benefit") Long benefit);
}
