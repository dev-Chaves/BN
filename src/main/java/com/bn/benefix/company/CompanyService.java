package com.bn.benefix.company;

import com.bn.benefix.company.dto.CompanyCreationRequestDTO;
import com.bn.benefix.company.dto.CompanyCreationResponseDTO;
import com.bn.benefix.shared.domain.CNPJ;
import org.springframework.stereotype.Service;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public CompanyCreationResponseDTO createCompany(CompanyCreationRequestDTO dto){

        Company newCompany = Company.builder(
                dto.name(),
                CNPJ.of(dto.cnpj()))
                .build();

        Company savedCompany = companyRepository.save(newCompany);

        return new CompanyCreationResponseDTO(
                savedCompany.getId(),
                savedCompany.getName(),
                savedCompany.getCnpj().getValue(),
                savedCompany.getCreatedAt()
        );
    }

    public Company findByCnpj(String cnpj) {
        return companyRepository.findByCNPJ(cnpj)
                .orElseThrow(() -> new RuntimeException("Company not found with CNPJ: " + cnpj));
    }

}
