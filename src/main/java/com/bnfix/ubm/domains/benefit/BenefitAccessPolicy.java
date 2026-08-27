package com.bnfix.ubm.domains.benefit;

import com.bnfix.ubm.domains.employee.Employee;
import com.bnfix.ubm.domains.partnership.PartnershipRepository;
import com.bnfix.ubm.shared.security.AccessStatusGuard;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class BenefitAccessPolicy {
    private final PartnershipRepository partnershipRepository;

    public BenefitAccessPolicy(PartnershipRepository partnershipRepository) {
        this.partnershipRepository = partnershipRepository;
    }

    public boolean isEligible(Employee employee, Benefit benefit, LocalDateTime now) {
        try {
            AccessStatusGuard.requireActive(employee);
        } catch (SecurityException exception) {
            return false;
        }
        if (benefit == null || !benefit.isOperationalAt(now)) return false;
        Long employeeCompanyId = employee.getCompany().id;
        if (benefit.getProvider().id.equals(employeeCompanyId))
            return Boolean.TRUE.equals(benefit.getAvailableToProviderEmployees());
        return partnershipRepository.existsActivePartnership(employeeCompanyId, benefit.id);
    }

    public void requireEligible(Employee employee, Benefit benefit, LocalDateTime now) {
        if (!isEligible(employee, benefit, now)) throw new SecurityException("Benefit not available for employee");
    }
}
