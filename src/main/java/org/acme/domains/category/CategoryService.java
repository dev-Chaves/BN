package org.acme.domains.category;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.domains.category.dto.CategoryResponse;

import java.util.List;

@ApplicationScoped
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @WithSession
    public Uni<List<CategoryResponse>> listAll() {
        return categoryRepository.listAll()
                .map(categories -> categories.stream()
                        .map(this::toResponse)
                        .toList());
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.id, category.getName());
    }
}
