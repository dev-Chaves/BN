package com.bnfix.ubm.domains.partnership;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PartnershipRepository extends JpaRepository<Partnership, Long> {
    List<Partnership> findByClientCompanyId(Long id);

    List<Partnership> findByBenefitId(Long id);

    List<Partnership> findByStatus(PartnershipStatus status);

    List<Partnership> findByClientCompanyIdAndStatus(Long id, PartnershipStatus status);

    boolean existsByClientCompanyIdAndBenefitId(Long clientCompanyId, Long benefitId);

    Optional<Partnership> findByClientCompanyIdAndBenefitIdAndStatus(
            Long clientCompanyId, Long benefitId, PartnershipStatus status);

    @Query(
            "select p from Partnership p join fetch p.clientCompany join fetch p.benefit b join fetch b.provider where p.id=:id")
    Optional<Partnership> findByIdWithProvider(@Param("id") Long id);

    @Query(
            "select p from Partnership p join fetch p.clientCompany join fetch p.benefit b join fetch b.provider where b.provider.id=:id and p.status=:status order by p.createdAt")
    List<Partnership> findPendingByProvider(@Param("id") Long id, @Param("status") PartnershipStatus status);

    default List<Partnership> findPendingByProvider(Long id) {
        return findPendingByProvider(id, PartnershipStatus.PENDING);
    }

    default boolean existsActivePartnership(Long clientCompanyId, Long benefitId) {
        return existsByClientCompanyIdAndBenefitIdAndStatus(clientCompanyId, benefitId, PartnershipStatus.ACTIVE);
    }

    boolean existsByClientCompanyIdAndBenefitIdAndStatus(
            Long clientCompanyId, Long benefitId, PartnershipStatus status);

    @Modifying
    @Query(
            "update Partnership p set p.status=:disabled where p.status in (:pending,:active) and (p.clientCompany.id=:id or p.benefit.provider.id=:id)")
    int disableByCompanyId(
            @Param("id") Long id,
            @Param("disabled") PartnershipStatus disabled,
            @Param("pending") PartnershipStatus pending,
            @Param("active") PartnershipStatus active);

    default int disableByCompanyId(Long id) {
        return disableByCompanyId(id, PartnershipStatus.DISABLED, PartnershipStatus.PENDING, PartnershipStatus.ACTIVE);
    }
}
