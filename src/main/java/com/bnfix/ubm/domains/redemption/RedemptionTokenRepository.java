package com.bnfix.ubm.domains.redemption;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RedemptionTokenRepository extends JpaRepository<RedemptionToken, UUID> {
    @Query(
            "select t from RedemptionToken t join fetch t.employee e join fetch e.company join fetch t.benefit b join fetch b.provider where t.tokenHash = :hash")
    Optional<RedemptionToken> findByHashWithRelations(@Param("hash") String hash);

    @Modifying
    @Query(
            "update RedemptionToken t set t.status = :newStatus where t.employee.id = :employee and t.benefit.id = :benefit and t.status = :oldStatus")
    int revokeActiveByEmployeeAndBenefit(
            @Param("employee") Long employeeId,
            @Param("benefit") Long benefitId,
            @Param("newStatus") RedemptionTokenStatus newStatus,
            @Param("oldStatus") RedemptionTokenStatus oldStatus);

    default int revokeActiveByEmployeeAndBenefit(Long employeeId, Long benefitId) {
        return revokeActiveByEmployeeAndBenefit(
                employeeId, benefitId, RedemptionTokenStatus.REVOKED, RedemptionTokenStatus.ACTIVE);
    }

    @Modifying
    @Query(
            "update RedemptionToken t set t.status = :newStatus where t.status = :oldStatus and t.employee.id = :employee")
    int revokeActiveByEmployee(
            @Param("employee") Long id,
            @Param("newStatus") RedemptionTokenStatus newStatus,
            @Param("oldStatus") RedemptionTokenStatus oldStatus);

    default int revokeActiveByEmployee(Long id) {
        return revokeActiveByEmployee(id, RedemptionTokenStatus.REVOKED, RedemptionTokenStatus.ACTIVE);
    }

    @Modifying
    @Query(
            "update RedemptionToken t set t.status = :newStatus where t.status = :oldStatus and (t.employee.company.id = :company or t.benefit.provider.id = :company)")
    int revokeActiveByCompanyId(
            @Param("company") Long id,
            @Param("newStatus") RedemptionTokenStatus newStatus,
            @Param("oldStatus") RedemptionTokenStatus oldStatus);

    default int revokeActiveByCompanyId(Long id) {
        return revokeActiveByCompanyId(id, RedemptionTokenStatus.REVOKED, RedemptionTokenStatus.ACTIVE);
    }

    @Modifying
    @Query("update RedemptionToken t set t.status = :consumed, t.consumedAt = :now where t.id = :id")
    int consumeIfActive(
            @Param("id") UUID id, @Param("now") LocalDateTime now, @Param("consumed") RedemptionTokenStatus consumed);

    default int consumeIfActive(UUID id, LocalDateTime now) {
        return consumeIfActive(id, now, RedemptionTokenStatus.CONSUMED);
    }
}
