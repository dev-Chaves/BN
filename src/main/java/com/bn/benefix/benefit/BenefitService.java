package com.bn.benefix.benefit;

import com.bn.benefix.benefit.dto.BenefitCreationRequestDTO;
import com.bn.benefix.benefit.dto.BenefitCreationResponseDTO;
import com.bn.benefix.benefit.dto.BenefitUpdateRequestDTO;
import com.bn.benefix.company.Company;
import com.bn.benefix.company.CompanyRepository;
import com.bn.benefix.manager.Manager;
import com.bn.benefix.manager.ManagerRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BenefitService {

    private final BenefitRepository benefitRepository;
    private final CompanyRepository companyRepository;
    private final ManagerRepository managerRepository;

    public BenefitService(BenefitRepository benefitRepository, CompanyRepository companyRepository, ManagerRepository managerRepository) {
        this.benefitRepository = benefitRepository;
        this.companyRepository = companyRepository;
        this.managerRepository = managerRepository;
    }

    public BenefitCreationResponseDTO createBenefit(BenefitCreationRequestDTO dto, UUID accountId) {
        Company provider = companyRepository.findByCNPJ(dto.providerCNPJ())
                .orElseThrow(() -> new IllegalArgumentException("Provider company not found"));

        validateManagerAuthorization(accountId, provider.getId());

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

    public List<BenefitCreationResponseDTO> findAll() {
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
    public BenefitCreationResponseDTO update(Long id, BenefitUpdateRequestDTO dto, UUID accountId) {
        Benefit benefit = benefitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Benefit not found"));

        validateManagerAuthorization(accountId, benefit.getProvider().getId());
        
        benefit.update(dto.name(), dto.description());
        
        return new BenefitCreationResponseDTO(
                benefit.getId(),
                benefit.getName(),
                benefit.getProvider().getName(),
                benefit.getActive(),
                benefit.getCreatedAt()
        );
    }

    @Transactional
    public void delete(Long id, UUID accountId) {
        Benefit benefit = benefitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Benefit not found"));

        validateManagerAuthorization(accountId, benefit.getProvider().getId());
        
        benefit.deactivateBenefit();
    }

    private void validateManagerAuthorization(UUID accountId, Long companyId) {
        Manager manager = managerRepository.findByAccountId(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Manager not found"));

        if (!manager.getCompany().getId().equals(companyId)) {
            throw new SecurityException("User is not authorized to manage benefits for this company");
        }
    }
}
