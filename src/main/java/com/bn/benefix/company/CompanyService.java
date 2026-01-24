package com.bn.benefix.company;

import com.bn.benefix.company.dto.CompanyCreationRequestDTO;
import com.bn.benefix.company.dto.CompanyCreationResponseDTO;
import com.bn.benefix.company.dto.CompanyUpdateRequestDTO;
import com.bn.benefix.manager.Manager;
import com.bn.benefix.manager.ManagerRepository;
import com.bn.benefix.shared.domain.CNPJ;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final ManagerRepository managerRepository;

    public CompanyService(CompanyRepository companyRepository, ManagerRepository managerRepository) {
        this.companyRepository = companyRepository;
        this.managerRepository = managerRepository;
    }

    public CompanyCreationResponseDTO createCompany(CompanyCreationRequestDTO dto){

        if (companyRepository.findByCNPJ(dto.cnpj()).isPresent()) {
            throw new IllegalArgumentException("Company with this CNPJ already exists");
        }

        Company newCompany = Company.builder(
                dto.name(),
                CNPJ.of(dto.cnpj()))
                .build();

        Company savedCompany = companyRepository.save(newCompany);

        return new CompanyCreationResponseDTO(
                savedCompany.getId(),
                savedCompany.getName(),
                savedCompany.getCnpj().getValue(),
                savedCompany.getActive(),
                savedCompany.getCreatedAt()
        );
    }

    public List<CompanyCreationResponseDTO> findAll() {
        return companyRepository.findAll().stream()
                .map(c -> new CompanyCreationResponseDTO(
                        c.getId(),
                        c.getName(),
                        c.getCnpj().getValue(),
                        c.getActive(),
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
                company.getActive(),
                company.getCreatedAt()
        );
    }

    public Company findByCnpj(String cnpj) {
        return companyRepository.findByCNPJ(cnpj)
                .orElseThrow(() -> new RuntimeException("Company not found with CNPJ: " + cnpj));
    }

    @Transactional
    public CompanyCreationResponseDTO update(Long id, CompanyUpdateRequestDTO dto, UUID accountId) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Company not found"));

        validateManagerAuthorization(accountId, company.getId());

        company.update(dto.name());

        return new CompanyCreationResponseDTO(
                company.getId(),
                company.getName(),
                company.getCnpj().getValue(),
                company.getActive(),
                company.getCreatedAt()
        );
    }

    @Transactional
    public void delete(Long id, UUID accountId) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Company not found"));

        validateManagerAuthorization(accountId, company.getId());
        
        company.deactivateCompany();
    }

    private void validateManagerAuthorization(UUID accountId, Long companyId) {
        Manager manager = managerRepository.findByAccountId(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Manager not found"));

        if (!manager.getCompany().getId().equals(companyId)) {
            throw new SecurityException("User is not authorized to access this company");
        }
    }

}
