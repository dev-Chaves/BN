package com.bn.benefix.partnership;

import com.bn.benefix.benefit.Benefit;
import com.bn.benefix.benefit.BenefitRepository;
import com.bn.benefix.company.Company;
import com.bn.benefix.company.CompanyRepository;
import com.bn.benefix.partnership.dto.PartnershipCreationRequestDTO;
import com.bn.benefix.partnership.dto.PartnershipCreationResponseDTO;
import com.bn.benefix.partnership.dto.PartnershipUpdateRequestDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PartnershipService {

    private final PartnershipRepository partnershipRepository;
    private final CompanyRepository companyRepository;
    private final BenefitRepository benefitRepository;

    public PartnershipService(PartnershipRepository partnershipRepository, CompanyRepository companyRepository, BenefitRepository benefitRepository) {
        this.partnershipRepository = partnershipRepository;
        this.companyRepository = companyRepository;
        this.benefitRepository = benefitRepository;
    }

    public PartnershipCreationResponseDTO createPartnership(PartnershipCreationRequestDTO dto) {
        Company clientCompany = companyRepository.findById(dto.clientCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Client company not found"));

        Benefit benefit = benefitRepository.findById(dto.benefitId())
                .orElseThrow(() -> new IllegalArgumentException("Benefit not found"));

        Partnership newPartnership = new Partnership.Builder(
                clientCompany,
                benefit)
                .build();

        Partnership savedPartnership = partnershipRepository.save(newPartnership);

        return new PartnershipCreationResponseDTO(
                savedPartnership.getId(),
                savedPartnership.getClientCompany().getId(),
                savedPartnership.getBenefit().getId(),
                savedPartnership.getStatus(),
                savedPartnership.getCreatedAt()
        );
    }

    public List<PartnershipCreationResponseDTO> findAll() {
        return partnershipRepository.findAll().stream()
                .map(p -> new PartnershipCreationResponseDTO(
                        p.getId(),
                        p.getClientCompany().getId(),
                        p.getBenefit().getId(),
                        p.getStatus(),
                        p.getCreatedAt()
                ))
                .toList();
    }

    public PartnershipCreationResponseDTO findById(Long id) {
        Partnership partnership = partnershipRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Partnership not found"));
        return new PartnershipCreationResponseDTO(
                partnership.getId(),
                partnership.getClientCompany().getId(),
                partnership.getBenefit().getId(),
                partnership.getStatus(),
                partnership.getCreatedAt()
        );
    }

    @Transactional
    public PartnershipCreationResponseDTO update(Long id, PartnershipUpdateRequestDTO dto) {
        Partnership partnership = partnershipRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Partnership not found"));

        partnership.updateStatus(dto.status());

        return new PartnershipCreationResponseDTO(
                partnership.getId(),
                partnership.getClientCompany().getId(),
                partnership.getBenefit().getId(),
                partnership.getStatus(),
                partnership.getCreatedAt()
        );
    }

    @Transactional
    public void delete(Long id) {
        Partnership partnership = partnershipRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Partnership not found"));
        
        partnership.updateStatus(PartnershipStatus.DISABLE);
    }
}
