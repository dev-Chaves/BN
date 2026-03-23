package org.acme.domains.partnership;

import java.util.List;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PartnershipRepository implements PanacheRepository<Partnership> {

    public Uni<List<Partnership>> findByClientCompanyId(Long companyId) {
        return list("clientCompany.profileId", companyId);
    }

    public Uni<List<Partnership>> findByBenefitId(Long benefitId) {
        return list("benefit.profileId", benefitId);
    }

    public Uni<List<Partnership>> findByStatus(PartnershipStatus status) {
        return list("status", status);
    }

    public Uni<Boolean> findExistingPartnership(Long clientCompanyId, Long benefitId) {
        return find("select count(p) from Partnership p where p.clientCompany.profileId = ?1 and p.benefit.profileId = ?2", clientCompanyId, benefitId).count().map(count -> count > 0);
    }

}
