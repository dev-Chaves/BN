package com.bnfix.ubm.shared.security;

import com.bnfix.ubm.domains.company.Company;
import com.bnfix.ubm.domains.company.CompanyRepository;
import com.bnfix.ubm.domains.employee.Employee;
import com.bnfix.ubm.domains.manager.Manager;
import com.bnfix.ubm.domains.partnership.Partnership;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TenantGuard {
    private final CompanyRepository companyRepository;

    public TenantGuard(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Company verifyManagerCompanyAccess(Manager manager, Long companyId) {
        AccessStatusGuard.requireActive(manager);
        requireCompanyId(manager, companyId);
        Company company =
                companyRepository.findById(companyId).orElseThrow(() -> new SecurityException("Company not found"));
        if (!Boolean.TRUE.equals(company.getActive())) throw new SecurityException("Company is inactive");
        return company;
    }

    public Employee verifyManagerEmployeeAccess(Manager manager, Employee employee) {
        AccessStatusGuard.requireActive(manager);
        requireCompanyId(manager, employee == null || employee.getCompany() == null ? null : employee.getCompany().id);
        return employee;
    }

    public Partnership verifyManagerPartnershipProviderAccess(Manager manager, Partnership partnership) {
        AccessStatusGuard.requireActive(manager);
        Long providerId = partnership == null
                        || partnership.getBenefit() == null
                        || partnership.getBenefit().getProvider() == null
                ? null
                : partnership.getBenefit().getProvider().id;
        requireCompanyId(manager, providerId);
        return partnership;
    }

    private void requireCompanyId(Manager manager, Long expected) {
        if (expected == null || manager.getCompany() == null || manager.getCompany().id == null)
            throw new SecurityException("Company not found");
        if (!manager.getCompany().id.equals(expected)) {
            log.warn(
                    "Tenant mismatch denied for manager {} (managerCompany={}, targetCompany={})",
                    manager.getAccount().getEmail(),
                    manager.getCompany().id,
                    expected);
            throw new SecurityException("Tenant mismatch");
        }
    }
}
