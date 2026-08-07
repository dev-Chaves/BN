package com.bnfix.ubm.domains.category;
import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import java.util.List; import com.bnfix.ubm.domains.category.dto.CategoryResponse;
@RestController @RequestMapping("/categories") public class CategoryController { private final CategoryService service; public CategoryController(CategoryService service){this.service=service;} @GetMapping @PreAuthorize("hasRole('MANAGER')") public List<CategoryResponse> list(){return service.listAll();} }
