package org.acme.domains.employee;

import java.util.List;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EmployeeRepository implements PanacheRepository<Employee> {

    public Uni<List<Employee>> findByCompanyId(Long companyId) {
        return list("company.profileId", companyId);
    }

    public Uni<Employee> findByAccountId(java.util.UUID accountId) {
        return find("account.profileId", accountId).firstResult();
    }

}
