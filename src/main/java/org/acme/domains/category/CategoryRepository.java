package org.acme.domains.category;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class CategoryRepository implements PanacheRepository<Category> {

    public Uni<Category> findByName(String name) {
        return find("name", name).firstResult();
    }

    public Uni<List<Category>> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Uni.createFrom().item(List.of());
        }
        return find("id in ?1", ids).list();
    }
}
