package com.bn.benefix.company;

import com.bn.benefix.company.dto.CompanyCreationRequestDTO;
import com.bn.benefix.company.dto.CompanyCreationResponseDTO;
import com.bn.benefix.company.dto.CompanyUpdateRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

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

    @GetMapping
    public ResponseEntity<List<CompanyCreationResponseDTO>> getAllCompanies() {
        return ResponseEntity.ok(companyService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyCreationResponseDTO> getCompanyById(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyCreationResponseDTO> updateCompany(
            @PathVariable Long id,
            @RequestBody CompanyUpdateRequestDTO dto,
            @AuthenticationPrincipal com.bn.benefix.infra.security.AccountUserDetails userDetails) {
        return ResponseEntity.ok(companyService.update(id, dto, userDetails.getAccount().getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(
            @PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.bn.benefix.infra.security.AccountUserDetails userDetails) {
        companyService.delete(id, userDetails.getAccount().getId());
        return ResponseEntity.noContent().build();
    }

}
