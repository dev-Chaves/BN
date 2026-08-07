package com.bnfix.ubm.domains.category;
import com.bnfix.ubm.domains.category.dto.CategoryResponse; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.util.List;
@Service public class CategoryService { private final CategoryRepository repository; public CategoryService(CategoryRepository repository){this.repository=repository;} @Transactional(readOnly=true) public List<CategoryResponse> listAll(){return repository.findAllByOrderByNameAsc().stream().map(c->new CategoryResponse(c.id,c.getName())).toList();} }
