package com.bnfix.ubm.domains.category;

import com.bnfix.ubm.domains.category.dto.CategoryResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public List<CategoryResponse> list() {
        return service.listAll();
    }
}
