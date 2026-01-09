package com.bn.benefix.company;

import com.bn.benefix.company.dto.CompanyCreationRequestDTO;
import com.bn.benefix.shared.domain.CNPJ;
import org.springframework.stereotype.Service;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Company createCompany(CompanyCreationRequestDTO dto){

        Company newCompany = Company.builder(
                dto.name(),
                CNPJ.of(dto.cnpj()))
                .build();

        return companyRepository.save(newCompany);
    }

}
