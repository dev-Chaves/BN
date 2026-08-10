package com.bnfix.ubm.domains.category;

import com.bnfix.ubm.domains.category.dto.CategoryResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public List<CategoryResponse> list() {
        return categoryService.listAll();
    }
}
