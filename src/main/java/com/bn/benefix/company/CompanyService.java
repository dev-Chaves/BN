package com.bn.benefix.company;

import com.bn.benefix.company.dto.CompanyCreationRequestDTO;
import com.bn.benefix.company.dto.CompanyCreationResponseDTO;
import com.bn.benefix.company.dto.CompanyUpdateRequestDTO;
import com.bn.benefix.shared.domain.CNPJ;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public List<CompanyCreationResponseDTO> findAll() {
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
                .orElseThrow(() -> new EntityNotFoundException("Company not found"));
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

    @Transactional
    public CompanyCreationResponseDTO update(Long id, CompanyUpdateRequestDTO dto) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Company not found"));

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
            throw new EntityNotFoundException("Company not found");
        }
        companyRepository.deleteById(id);
    }

}
