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

}
