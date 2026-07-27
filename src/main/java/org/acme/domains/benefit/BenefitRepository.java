package org.acme.domains.benefit;

import java.util.List;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BenefitRepository implements PanacheRepository<Benefit> {

    public Uni<List<Benefit>> findByCNPJ(String cnpj) {
        return list("provider.cnpj", cnpj);
    }

    public Uni<List<Benefit>> findByCompanyId(Long companyId) {
        return list("provider.id", companyId);
    }

    public Uni<List<Benefit>> findActiveByProviderNot(Long companyId) {
        return find("provider.id <> ?1 and active = true", companyId).list();
    }

    public Uni<List<Benefit>> findPublicAvailableByProviderNot(Long companyId) {
        return find("""
                select distinct b from Benefit b
                join fetch b.provider p
                left join fetch b.categories
                where p.id <> ?1
                  and p.active = true
                  and b.active = true
                  and b.publiclyVisible = true
                  and (b.validFrom is null or b.validFrom <= CURRENT_TIMESTAMP)
                  and (b.validUntil is null or b.validUntil > CURRENT_TIMESTAMP)
                order by b.createdAt desc
                """, companyId).list();
    }

    public Uni<Benefit> findByIdWithProviderAndCategories(Long benefitId) {
        return find("""
                select distinct b from Benefit b
                join fetch b.provider
                left join fetch b.categories
                where b.id = ?1
                """, benefitId).firstResult();
    }

    public Uni<List<Benefit>> findActiveByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Uni.createFrom().item(List.of());
        }
        return find("id in ?1 and active = true", ids).list();
    }

    public Uni<List<Benefit>> findByCompanyIdAndCategoryId(Long companyId, Long categoryId) {
        return find("provider.id = ?1 and ?2 member of categories", companyId, categoryId).list();
    }

    public Uni<List<Benefit>> findActiveByProviderNotAndCategoryId(Long companyId, Long categoryId) {
        return find("provider.id <> ?1 and active = true and ?2 member of categories", companyId, categoryId).list();
    }

}
