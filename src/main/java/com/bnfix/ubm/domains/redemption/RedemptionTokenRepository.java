package com.bnfix.ubm.domains.redemption;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface RedemptionTokenRepository extends JpaRepository<RedemptionToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RedemptionToken t join fetch t.subscription s join fetch s.employee join fetch s.benefit b join fetch b.provider where t.tokenHash = :hash")
    Optional<RedemptionToken> findByHashWithRelations(@Param("hash") String hash);

    @Modifying
    @Query("update RedemptionToken t set t.status = :newStatus where t.subscription.id = :subscription and t.status = :oldStatus")
    int revokeActiveBySubscription(@Param("subscription") Long id, @Param("newStatus") RedemptionTokenStatus newStatus, @Param("oldStatus") RedemptionTokenStatus oldStatus);
    default int revokeActiveBySubscription(Long id) { return revokeActiveBySubscription(id, RedemptionTokenStatus.REVOKED, RedemptionTokenStatus.ACTIVE); }

    @Modifying
    @Query("update RedemptionToken t set t.status = :newStatus where t.status = :oldStatus and t.subscription.employee.id = :employee")
    int revokeActiveByEmployee(@Param("employee") Long id, @Param("newStatus") RedemptionTokenStatus newStatus, @Param("oldStatus") RedemptionTokenStatus oldStatus);
    default int revokeActiveByEmployee(Long id) { return revokeActiveByEmployee(id, RedemptionTokenStatus.REVOKED, RedemptionTokenStatus.ACTIVE); }

    @Modifying
    @Query("update RedemptionToken t set t.status = :newStatus where t.status = :oldStatus and (t.subscription.employee.company.id = :company or t.subscription.benefit.provider.id = :company)")
    int revokeActiveByCompanyId(@Param("company") Long id, @Param("newStatus") RedemptionTokenStatus newStatus, @Param("oldStatus") RedemptionTokenStatus oldStatus);
    default int revokeActiveByCompanyId(Long id) { return revokeActiveByCompanyId(id, RedemptionTokenStatus.REVOKED, RedemptionTokenStatus.ACTIVE); }

    @Modifying
    @Query("update RedemptionToken t set t.status = :consumed, t.consumedAt = :now where t.id = :id and t.status = :active and t.expiresAt > :now")
    int consumeIfActive(@Param("id") UUID id, @Param("now") LocalDateTime now, @Param("consumed") RedemptionTokenStatus consumed, @Param("active") RedemptionTokenStatus active);
    default int consumeIfActive(UUID id, LocalDateTime now) { return consumeIfActive(id, now, RedemptionTokenStatus.CONSUMED, RedemptionTokenStatus.ACTIVE); }
}
