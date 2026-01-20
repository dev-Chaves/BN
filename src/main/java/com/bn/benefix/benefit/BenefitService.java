package com.bn.benefix.benefit;

import com.bn.benefix.benefit.dto.BenefitCreationRequestDTO;
import com.bn.benefix.benefit.dto.BenefitCreationResponseDTO;
import com.bn.benefix.company.Company;
import com.bn.benefix.company.CompanyRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class BenefitService {

    private final BenefitRepository benefitRepository;
    private final CompanyRepository companyRepository;

    public BenefitService(BenefitRepository benefitRepository, CompanyRepository companyRepository) {
        this.benefitRepository = benefitRepository;
        this.companyRepository = companyRepository;
    }

    public BenefitCreationResponseDTO createBenefit(BenefitCreationRequestDTO dto) {
        Company provider = companyRepository.findByCNPJ(dto.providerCNPJ())
                .orElseThrow(() -> new IllegalArgumentException("Provider company not found"));

        Benefit newBenefit = new Benefit.Builder(
                dto.name(),
                provider)
                .description(dto.description())
                .build();

        Benefit savedBenefit = benefitRepository.save(newBenefit);

        return new BenefitCreationResponseDTO(
                savedBenefit.getId(),
                savedBenefit.getName(),
                savedBenefit.getProvider().getName(),
                savedBenefit.getActive(),
                savedBenefit.getCreatedAt()
        );
    }

    public java.util.List<BenefitCreationResponseDTO> findAll() {
        return benefitRepository.findAll().stream()
                .map(b -> new BenefitCreationResponseDTO(
                        b.getId(),
                        b.getName(),
                        b.getProvider().getName(),
                        b.getActive(),
                        b.getCreatedAt()
                ))
                .toList();
    }

    public BenefitCreationResponseDTO findById(Long id) {
        Benefit benefit = benefitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Benefit not found"));
        return new BenefitCreationResponseDTO(
                benefit.getId(),
                benefit.getName(),
                benefit.getProvider().getName(),
                benefit.getActive(),
                benefit.getCreatedAt()
        );
    }

    @Transactional
    public BenefitCreationResponseDTO update(Long id, com.bn.benefix.benefit.dto.BenefitUpdateRequestDTO dto) {
        Benefit benefit = benefitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Benefit not found"));
        
        benefit.update(dto.name(), dto.description());
        
        return new BenefitCreationResponseDTO(
                benefit.getId(),
                benefit.getName(),
                benefit.getProvider().getName(),
                benefit.getActive(),
                benefit.getCreatedAt()
        );
    }

    public void delete(Long id) {
        if (!benefitRepository.existsById(id)) {
            throw new EntityNotFoundException("Benefit not found");
        }
        benefitRepository.deleteById(id);
    }
}
