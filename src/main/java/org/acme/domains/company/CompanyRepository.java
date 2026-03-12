package org.acme.domains.company;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CompanyRepository implements PanacheRepository<Company> {

    public Uni<Company> findByCNPJ(String cnpj) {
        return find("cnpj", cnpj).firstResult();
    }

    public Uni<Company> findByName(String name) {
        return find("name", name).firstResult();
    }

}
