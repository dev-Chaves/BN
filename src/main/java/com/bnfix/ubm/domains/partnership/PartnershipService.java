package com.bnfix.ubm.domains.partnership;

import com.bnfix.ubm.domains.benefit.*;
import com.bnfix.ubm.domains.company.*;
import com.bnfix.ubm.domains.manager.*;
import com.bnfix.ubm.domains.partnership.dto.*;
import com.bnfix.ubm.shared.security.TenantGuard;
import java.time.LocalDateTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class PartnershipService {
    private final PartnershipRepository partnershipRepository;
    private final BenefitRepository benefitRepository;
    private final ManagerRepository managerRepository;
    private final TenantGuard tenantGuard;

    public PartnershipService(
            PartnershipRepository partnershipRepository,
            BenefitRepository benefitRepository,
            ManagerRepository managerRepository,
            TenantGuard tenantGuard) {
        this.partnershipRepository = partnershipRepository;
        this.benefitRepository = benefitRepository;
        this.managerRepository = managerRepository;
        this.tenantGuard = tenantGuard;
    }

    @Transactional
    public PartnershipResponse request(String email, Long companyId, Long benefitId) {
        Manager manager = manager(email, companyId);
        Company client = tenantGuard.verifyManagerCompanyAccess(manager, companyId);
        Benefit benefit = benefitRepository
                .findByIdWithProviderAndCategories(benefitId)
                .orElseThrow(() -> new NoSuchElementException("Benefit not found"));
        if (client.id.equals(benefit.getProvider().id))
            throw new IllegalArgumentException("Company cannot request its own benefit");
        if (!benefit.isDiscoverableAt(LocalDateTime.now()))
            throw new IllegalStateException("Benefit is not available for partnership");
        if (partnershipRepository.existsByClientCompanyIdAndBenefitId(client.id, benefit.id))
            throw new IllegalStateException("Partnership already exists");
        Partnership partnership =
                partnershipRepository.save(Partnership.builder(client, benefit).build());
        log.info("Partnership {} requested by company {} for benefit {}", partnership.id, client.id, benefitId);
        return response(partnership);
    }

    @Transactional
    public PartnershipResponse transition(String email, Long companyId, Long id, PartnershipStatus target) {
        Manager manager = manager(email, companyId);
        Partnership partnership = partnershipRepository
                .findByIdWithProvider(id)
                .orElseThrow(() -> new NoSuchElementException("Partnership not found"));
        tenantGuard.verifyManagerPartnershipProviderAccess(manager, partnership);
        switch (target) {
            case ACTIVE -> partnership.activate();
            case REJECTED -> partnership.reject();
            case DISABLED -> partnership.disable();
            default -> throw new IllegalStateException("Invalid partnership status transition");
        }
        log.info("Partnership {} transitioned to {} by manager {}", id, target, manager.id);
        return response(partnership);
    }

    @Transactional(readOnly = true)
    public List<PartnershipResponse> pending(String email, Long id) {
        Manager manager = manager(email, id);
        return partnershipRepository.findPendingByProvider(manager.getCompany().id).stream()
                .map(this::response)
                .toList();
    }

    private Manager manager(String email, Long id) {
        return managerRepository
                .findByEmailAndCompanyId(email, id)
                .map(com.bnfix.ubm.shared.security.AccessStatusGuard::requireActive)
                .orElseThrow(() -> new NoSuchElementException("Manager not found"));
    }

    private PartnershipResponse response(Partnership partnership) {
        return new PartnershipResponse(
                partnership.id,
                partnership.getClientCompany().id,
                partnership.getClientCompany().getName(),
                partnership.getBenefit().id,
                partnership.getBenefit().getName(),
                partnership.getStatus(),
                partnership.getCreatedAt());
    }
}
