package com.bn.benefix.company;

import com.bn.benefix.company.dto.CompanyCreationRequestDTO;
import com.bn.benefix.company.dto.CompanyCreationResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController()
@RequestMapping("company")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping()
    public ResponseEntity<CompanyCreationResponseDTO> createCompany(@Valid @RequestBody CompanyCreationRequestDTO dto, UriComponentsBuilder uriBuilder){

        var response = companyService.createCompany(dto);

        var uri = uriBuilder.path("/company/{id}").buildAndExpand(response.id()).toUri();

        return ResponseEntity.created(uri).body(response);

    }

    @org.springframework.web.bind.annotation.GetMapping
    public ResponseEntity<java.util.List<CompanyCreationResponseDTO>> getAllCompanies() {
        return ResponseEntity.ok(companyService.findAll());
    }

    @org.springframework.web.bind.annotation.GetMapping("/{id}")
    public ResponseEntity<CompanyCreationResponseDTO> getCompanyById(@org.springframework.web.bind.annotation.PathVariable Long id) {
        return ResponseEntity.ok(companyService.findById(id));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    public ResponseEntity<CompanyCreationResponseDTO> updateCompany(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody com.bn.benefix.company.dto.CompanyUpdateRequestDTO dto) {
        return ResponseEntity.ok(companyService.update(id, dto));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(@org.springframework.web.bind.annotation.PathVariable Long id) {
        companyService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
