package org.acme.domains.partnership;

import java.util.List;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PartnershipRepository implements PanacheRepository<Partnership> {

    public Uni<List<Partnership>> findByClientCompanyId(Long companyId) {
        return list("clientCompany.id", companyId);
    }

    public Uni<List<Partnership>> findByBenefitId(Long benefitId) {
        return list("benefit.id", benefitId);
    }

    public Uni<List<Partnership>> findByStatus(PartnershipStatus status) {
        return list("status", status);
    }

    public Uni<List<Partnership>> findByClientCompanyIdAndStatus(Long companyId, PartnershipStatus status) {
        return list("clientCompany.id = ?1 and status = ?2", companyId, status);
    }

    public Uni<Boolean> findExistingPartnership(Long clientCompanyId, Long benefitId) {
        return find("select count(p) from Partnership p where p.clientCompany.id = ?1 and p.benefit.id = ?2", clientCompanyId, benefitId).count().map(count -> count > 0);
    }

    public Uni<Partnership> findByClientCompanyBenefitAndStatus(Long clientCompanyId, Long benefitId, PartnershipStatus status) {
        return find("clientCompany.id = ?1 and benefit.id = ?2 and status = ?3", clientCompanyId, benefitId, status).firstResult();
    }

    public Uni<Partnership> findByIdWithProvider(Long id) {
        return find("select p from Partnership p join fetch p.clientCompany join fetch p.benefit b join fetch b.provider where p.id = ?1", id)
                .firstResult();
    }

    public Uni<List<Partnership>> findPendingByProvider(Long providerId) {
        return find("""
                select p from Partnership p
                join fetch p.clientCompany
                join fetch p.benefit b
                join fetch b.provider
                where b.provider.id = ?1 and p.status = ?2 order by p.createdAt
                """, providerId, PartnershipStatus.PENDING).list();
    }

    public Uni<Boolean> existsActivePartnership(Long clientCompanyId, Long benefitId) {
        return findByClientCompanyBenefitAndStatus(clientCompanyId, benefitId, PartnershipStatus.ACTIVE)
                .onItem().transform(partnership -> partnership != null);
    }

    public Uni<Integer> disableByCompanyId(Long companyId) {
        return update("""
                status = ?1
                where status in (?2, ?3)
                  and (
                    clientCompany.id = ?4
                    or benefit.id in (
                      select benefit.id from Benefit benefit where benefit.provider.id = ?4
                    )
                  )
                """,
                PartnershipStatus.DISABLED,
                PartnershipStatus.PENDING,
                PartnershipStatus.ACTIVE,
                companyId);
    }

}
