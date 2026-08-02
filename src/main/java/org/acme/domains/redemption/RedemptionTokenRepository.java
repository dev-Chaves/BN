package org.acme.domains.redemption;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class RedemptionTokenRepository implements PanacheRepository<RedemptionToken> {

    public Uni<RedemptionToken> findByHashWithRelations(String hash) {
        return find("""
                select t from RedemptionToken t
                join fetch t.subscription s
                join fetch s.employee e
                join fetch s.benefit b
                join fetch b.provider
                where t.tokenHash = ?1
                """, hash).firstResult();
    }

    public Uni<Integer> revokeActiveBySubscription(Long subscriptionId) {
        return update("status = ?1 where subscription.id = ?2 and status = ?3",
                RedemptionTokenStatus.REVOKED, subscriptionId, RedemptionTokenStatus.ACTIVE);
    }

    public Uni<Integer> revokeActiveByEmployee(Long employeeId) {
        return update("""
                status = ?1
                where status = ?2
                  and subscription.id in (
                    select s.id from Subscription s where s.employee.id = ?3
                  )
                """, RedemptionTokenStatus.REVOKED, RedemptionTokenStatus.ACTIVE, employeeId);
    }

    public Uni<Integer> revokeActiveByCompanyId(Long companyId) {
        return update("""
                status = ?1
                where status = ?2
                  and subscription.id in (
                    select subscription.id from Subscription subscription
                    where subscription.employee.company.id = ?3
                       or subscription.benefit.provider.id = ?3
                  )
                """, RedemptionTokenStatus.REVOKED, RedemptionTokenStatus.ACTIVE, companyId);
    }

    public Uni<Integer> consumeIfActive(UUID id, LocalDateTime now) {
        return update("""
                status = ?1, consumedAt = ?2
                where id = ?3 and status = ?4 and expiresAt > ?2
                """, RedemptionTokenStatus.CONSUMED, now, id, RedemptionTokenStatus.ACTIVE);
    }
}
