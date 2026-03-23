package org.acme.domains.manager;

import java.util.List;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ManagerRepository implements PanacheRepository<Manager> {

    public Uni<List<Manager>> findByCompanyId(Long companyId) {
        return list("company.profileId", companyId);
    }

    public Uni<Manager> findByAccountId(java.util.UUID accountId) {
        return find("account.profileId", accountId).firstResult();
    }

    public Uni<List<Manager>> findByCNPJ(String cnpj) {
        return list("company.cnpj", cnpj);
    }

    public Uni<Manager> findByEmail(String email) {
        return find("select m from Manager m join m.account a where a.email = ?1", email).firstResult();
    }

}
