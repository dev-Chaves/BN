package com.bnfix.ubm.domains.partnership;

import com.bnfix.ubm.domains.benefit.*;
import com.bnfix.ubm.domains.company.*;
import com.bnfix.ubm.domains.manager.*;
import com.bnfix.ubm.domains.partnership.dto.*;
import com.bnfix.ubm.shared.security.TenantGuard;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartnershipService {
    private final PartnershipRepository repo;
    private final BenefitRepository benefits;
    private final ManagerRepository managers;
    private final TenantGuard tenant;

    public PartnershipService(PartnershipRepository r, BenefitRepository b, ManagerRepository m, TenantGuard t) {
        repo = r;
        benefits = b;
        managers = m;
        tenant = t;
    }

    @Transactional
    public PartnershipResponse request(String email, Long companyId, Long benefitId) {
        Manager m = manager(email, companyId);
        Company client = tenant.verifyManagerCompanyAccess(m, companyId);
        Benefit b = benefits.findByIdWithProviderAndCategories(benefitId)
                .orElseThrow(() -> new NoSuchElementException("Benefit not found"));
        if (client.id.equals(b.getProvider().id))
            throw new IllegalArgumentException("Company cannot request its own benefit");
        if (!b.isDiscoverableAt(LocalDateTime.now()))
            throw new IllegalStateException("Benefit is not available for partnership");
        if (repo.existsByClientCompanyIdAndBenefitId(client.id, b.id))
            throw new IllegalStateException("Partnership already exists");
        return response(repo.save(Partnership.builder(client, b).build()));
    }

    @Transactional
    public PartnershipResponse transition(String email, Long companyId, Long id, PartnershipStatus target) {
        Manager m = manager(email, companyId);
        Partnership p =
                repo.findByIdWithProvider(id).orElseThrow(() -> new NoSuchElementException("Partnership not found"));
        tenant.verifyManagerPartnershipProviderAccess(m, p);
        if ((p.getStatus() == PartnershipStatus.PENDING
                        && (target == PartnershipStatus.ACTIVE || target == PartnershipStatus.REJECTED))
                || (p.getStatus() == PartnershipStatus.ACTIVE && target == PartnershipStatus.DISABLED)) {
            p.updateStatus(target);
            return response(p);
        }
        throw new IllegalStateException("Invalid partnership status transition");
    }

    @Transactional(readOnly = true)
    public List<PartnershipResponse> pending(String email, Long id) {
        Manager m = manager(email, id);
        return repo.findPendingByProvider(m.getCompany().id).stream()
                .map(this::response)
                .toList();
    }

    private Manager manager(String e, Long id) {
        return managers.findByEmailAndCompanyId(e, id)
                .map(com.bnfix.ubm.shared.security.AccessStatusGuard::requireActive)
                .orElseThrow(() -> new NoSuchElementException("Manager not found"));
    }

    private PartnershipResponse response(Partnership p) {
        return new PartnershipResponse(
                p.id,
                p.getClientCompany().id,
                p.getClientCompany().getName(),
                p.getBenefit().id,
                p.getBenefit().getName(),
                p.getStatus(),
                p.getCreatedAt());
    }
}
