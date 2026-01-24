package com.bn.benefix.partnership;

import com.bn.benefix.benefit.Benefit;
import com.bn.benefix.benefit.BenefitRepository;
import com.bn.benefix.company.Company;
import com.bn.benefix.company.CompanyRepository;
import com.bn.benefix.manager.Manager;
import com.bn.benefix.manager.ManagerRepository;
import com.bn.benefix.partnership.dto.PartnershipCreationRequestDTO;
import com.bn.benefix.partnership.dto.PartnershipCreationResponseDTO;
import com.bn.benefix.partnership.dto.PartnershipUpdateRequestDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PartnershipService {

    private final PartnershipRepository partnershipRepository;
    private final CompanyRepository companyRepository;
    private final BenefitRepository benefitRepository;
    private final ManagerRepository managerRepository;

    public PartnershipService(PartnershipRepository partnershipRepository, CompanyRepository companyRepository, BenefitRepository benefitRepository, ManagerRepository managerRepository) {
        this.partnershipRepository = partnershipRepository;
        this.companyRepository = companyRepository;
        this.benefitRepository = benefitRepository;
        this.managerRepository = managerRepository;
    }

    public PartnershipCreationResponseDTO createPartnership(PartnershipCreationRequestDTO dto, UUID accountId) {
        Company clientCompany = companyRepository.findById(dto.clientCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Client company not found"));

        validateManagerAuthorization(accountId, clientCompany.getId());

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

    public void acceptPartnership(Long idPartnership, UUID idAccount) {

        Partnership partnership = partnershipRepository.findById(idPartnership)
                .orElseThrow(() -> new EntityNotFoundException("Partnership not found"));

        Manager manager = managerRepository.findByAccountId(idAccount).orElseThrow(()-> new EntityNotFoundException("Manager not found"));

        if(!partnership.getBenefit().getProvider().getId().equals(manager.getCompany().getId())) {
            throw new IllegalArgumentException("Manager's company is not the provider of this benefit");
        }

        partnership.updateStatus(PartnershipStatus.ACTIVE);
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
    public PartnershipCreationResponseDTO update(Long id, PartnershipUpdateRequestDTO dto, UUID accountId) {
        Partnership partnership = partnershipRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Partnership not found"));

        validateManagerAccessToPartnership(accountId, partnership);

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
    public void delete(Long id, UUID accountId) {
        Partnership partnership = partnershipRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Partnership not found"));
        
        validateManagerAccessToPartnership(accountId, partnership);

        partnership.updateStatus(PartnershipStatus.DISABLE);
    }

    private void validateManagerAuthorization(UUID accountId, Long companyId) {
        Manager manager = managerRepository.findByAccountId(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Manager not found"));

        if (!manager.getCompany().getId().equals(companyId)) {
            throw new SecurityException("User is not authorized to act on behalf of this company");
        }
    }

    private void validateManagerAccessToPartnership(UUID accountId, Partnership partnership) {
        Manager manager = managerRepository.findByAccountId(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Manager not found"));

        Long managerCompanyId = manager.getCompany().getId();
        boolean isClient = managerCompanyId.equals(partnership.getClientCompany().getId());
        boolean isProvider = managerCompanyId.equals(partnership.getBenefit().getProvider().getId());

        if (!isClient && !isProvider) {
             throw new SecurityException("User is not authorized to modify this partnership");
        }
    }
}
