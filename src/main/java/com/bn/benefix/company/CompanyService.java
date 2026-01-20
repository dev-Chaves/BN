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

    public java.util.List<CompanyCreationResponseDTO> findAll() {
        return companyRepository.findAll().stream()
                .map(c -> new CompanyCreationResponseDTO(
                        c.getId(),
                        c.getName(),
                        c.getCnpj().getValue(),
                        c.getCreatedAt()
                ))
                .toList();
    }

    public CompanyCreationResponseDTO findById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Company not found"));
        return new CompanyCreationResponseDTO(
                company.getId(),
                company.getName(),
                company.getCnpj().getValue(),
                company.getCreatedAt()
        );
    }

    public Company findByCnpj(String cnpj) {
        return companyRepository.findByCNPJ(cnpj)
                .orElseThrow(() -> new RuntimeException("Company not found with CNPJ: " + cnpj));
    }

    @org.springframework.transaction.annotation.Transactional
    public CompanyCreationResponseDTO update(Long id, com.bn.benefix.company.dto.CompanyUpdateRequestDTO dto) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Company not found"));

        company.update(dto.name());

        return new CompanyCreationResponseDTO(
                company.getId(),
                company.getName(),
                company.getCnpj().getValue(),
                company.getCreatedAt()
        );
    }

    public void delete(Long id) {
        if (!companyRepository.existsById(id)) {
            throw new jakarta.persistence.EntityNotFoundException("Company not found");
        }
        companyRepository.deleteById(id);
    }

}
