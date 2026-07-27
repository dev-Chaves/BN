package org.acme.domains.benefitrequest;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

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
                """, id).firstResult();
    }
}
