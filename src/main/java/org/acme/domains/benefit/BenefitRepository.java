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
        return list("select distinct b from Benefit b left join fetch b.categories where b.provider.id = ?1", companyId);
    }

    public Uni<List<Benefit>> findActiveByProviderNot(Long companyId) {
        return find("select distinct b from Benefit b left join fetch b.categories where b.provider.id <> ?1 and b.active = true", companyId).list();
    }

    public Uni<List<Benefit>> findActiveByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Uni.createFrom().item(List.of());
        }
        return find("select distinct b from Benefit b left join fetch b.categories where b.id in ?1 and b.active = true", ids).list();
    }

    public Uni<List<Benefit>> findByCompanyIdAndCategoryId(Long companyId, Long categoryId) {
        return find("select distinct b from Benefit b join fetch b.categories c where b.provider.id = ?1 and c.id = ?2", companyId, categoryId).list();
    }

    public Uni<List<Benefit>> findActiveByProviderNotAndCategoryId(Long companyId, Long categoryId) {
        return find("select distinct b from Benefit b join fetch b.categories c where b.provider.id <> ?1 and b.active = true and c.id = ?2", companyId, categoryId).list();
    }

}
