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

        var company = companyService.createCompany(dto);

        var response = new CompanyCreationResponseDTO(
                company.getId(),
                company.getName(),
                company.getCnpj().getValue(),
                company.getCreatedAt()
        );

        var uri = uriBuilder.path("/company/{id}").buildAndExpand(company.getId()).toUri();

        return ResponseEntity.created(uri).body(response);

    }

}
