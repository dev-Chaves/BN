package org.acme.domains.manager;

import java.util.List;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ManagerRepository implements PanacheRepository<Manager> {

    public Uni<List<Manager>> findByCompanyId(Long companyId) {
        return list("company.id", companyId);
    }

    public Uni<Manager> findByAccountId(java.util.UUID accountId) {
        return find("account.id", accountId).firstResult();
    }

    public Uni<List<Manager>> findByCNPJ(String cnpj) {
        return list("company.cnpj", cnpj);
    }

}
