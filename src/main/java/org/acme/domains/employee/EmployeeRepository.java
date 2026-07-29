package org.acme.domains.employee;

import java.util.List;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EmployeeRepository implements PanacheRepository<Employee> {

    public Uni<List<Employee>> findByCompanyId(Long companyId) {
        return list("company.id", companyId);
    }

    public Uni<List<Employee>> findActiveByCompanyId(Long companyId) {
        return list("company.id = ?1 and active = ?2", companyId, EmployeeStatus.ACTIVE);
    }

    public Uni<Employee> findByAccountId(java.util.UUID accountId) {
        return find("""
                select e from Employee e
                join fetch e.account
                join fetch e.company
                where e.account.id = ?1
                """, accountId).firstResult();
    }

}
