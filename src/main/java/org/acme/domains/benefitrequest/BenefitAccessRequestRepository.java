package org.acme.domains.benefitrequest;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class BenefitAccessRequestRepository implements PanacheRepository<BenefitAccessRequest> {

    public Uni<BenefitAccessRequest> findPending(Long employeeId, Long benefitId) {
        return find("employee.id = ?1 and benefit.id = ?2 and status = ?3",
                employeeId, benefitId, BenefitAccessRequestStatus.PENDING).firstResult();
    }

    public Uni<List<BenefitAccessRequest>> findByEmployee(Long employeeId) {
        return find("""
                select r from BenefitAccessRequest r
                join fetch r.employee e
                join fetch r.benefit b
                join fetch b.provider
                where e.id = ?1 order by r.requestedAt desc
                """, employeeId).list();
    }

    public Uni<List<BenefitAccessRequest>> findPendingByProvider(Long providerId) {
        return find("""
                select r from BenefitAccessRequest r
                join fetch r.employee e
                join fetch e.company
                join fetch r.benefit b
                join fetch b.provider p
                where p.id = ?1 and r.status = ?2 order by r.requestedAt
                """, providerId, BenefitAccessRequestStatus.PENDING).list();
    }

    public Uni<BenefitAccessRequest> findByIdWithRelations(Long id) {
        return find("""
                select r from BenefitAccessRequest r
                join fetch r.employee e
                join fetch e.company
                join fetch r.benefit b
                join fetch b.provider
                where r.id = ?1
                """, id)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .firstResult();
    }

    public Uni<Integer> cancelPendingByCompanyId(Long companyId, LocalDateTime cancelledAt) {
        return update("""
                update BenefitAccessRequest r
                set r.status = ?1, r.reviewedAt = ?2, r.rejectionReason = ?3
                where r.status = ?4
                  and (
                    r.employee.id in (
                        select e.id from Employee e where e.company.id = ?5
                    )
                    or r.benefit.id in (
                        select b.id from Benefit b where b.provider.id = ?5
                    )
                  )
                """,
                BenefitAccessRequestStatus.CANCELLED,
                cancelledAt,
                "Company deactivated",
                BenefitAccessRequestStatus.PENDING,
                companyId);
    }
}
