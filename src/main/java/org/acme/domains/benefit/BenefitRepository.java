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

    public Uni<List<Benefit>> findActiveByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Uni.createFrom().item(List.of());
        }
        return find("id in ?1 and active = true", ids).list();
    }

}
