package org.acme.domains.account;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AccountRepository implements PanacheRepository<Account> {

    public Uni<Account> findByCPF(String cpf){
        return find("cpf.value", cpf).firstResult();
    }

    public Uni<Account> findByEmail(String email){
        return find("lower(email) = lower(?1)", email.trim()).firstResult();
    }


}
