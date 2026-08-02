package org.acme.domains.manager;

import java.util.List;
import java.util.UUID;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ManagerRepository implements PanacheRepository<Manager> {

    public Uni<List<Manager>> findByCompanyId(Long companyId) {
        return list("company.id", companyId);
    }

    public Uni<Manager> findActiveByAccountId(UUID accountId) {
        return find("""
                select m from Manager m
                join fetch m.account
                join fetch m.company c
                where m.account.id = ?1
                  and m.active = true
                  and c.active = true
                order by m.createdAt desc, m.id desc
                """, accountId).firstResult();
    }

    public Uni<List<Manager>> findByCNPJ(String cnpj) {
        return list("company.cnpj", cnpj);
    }

    public Uni<Manager> findByEmailAndCompanyId(String email, Long companyId) {
        return find("""
                select m from Manager m
                join fetch m.account a
                join fetch m.company c
                where lower(a.email) = lower(?1)
                  and c.id = ?2
                """, email.trim(), companyId).firstResult();
    }

    public Uni<List<Manager>> findActiveByEmail(String email) {
        return find("""
                select m from Manager m
                join fetch m.account a
                join fetch m.company c
                where lower(a.email) = lower(?1)
                  and m.active = true
                  and c.active = true
                order by m.createdAt desc, m.id desc
                """, email.trim()).list();
    }

    public Uni<Integer> deactivateByCompanyId(Long companyId) {
        return update("active = false where company.id = ?1 and active = true", companyId);
    }

}
