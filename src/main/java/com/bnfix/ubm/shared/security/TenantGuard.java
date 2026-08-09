package com.bnfix.ubm.shared.security;

import com.bnfix.ubm.domains.benefit.Benefit;
import com.bnfix.ubm.domains.company.Company;
import com.bnfix.ubm.domains.company.CompanyRepository;
import com.bnfix.ubm.domains.employee.Employee;
import com.bnfix.ubm.domains.manager.Manager;
import com.bnfix.ubm.domains.partnership.Partnership;
import com.bnfix.ubm.domains.partnership.PartnershipRepository;
import org.springframework.stereotype.Component;

@Component
public class TenantGuard {
    private final CompanyRepository companies;
    private final PartnershipRepository partnerships;

    public TenantGuard(CompanyRepository companies, PartnershipRepository partnerships) {
        this.companies = companies;
        this.partnerships = partnerships;
    }

    public Company verifyManagerCompanyAccess(Manager manager, Long companyId) {
        AccessStatusGuard.requireActive(manager);
        requireCompanyId(manager, companyId);
        Company company = companies.findById(companyId).orElseThrow(() -> new SecurityException("Company not found"));
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

    public Benefit verifyEmployeeBenefitAccess(Employee employee, Benefit benefit) {
        AccessStatusGuard.requireActive(employee);
        Long companyId = employee.getCompany() == null ? null : employee.getCompany().id;
        if (companyId == null
                || benefit == null
                || benefit.id == null
                || !partnerships.existsActivePartnership(companyId, benefit.id))
            throw new SecurityException("Benefit not available for tenant");
        return benefit;
    }

    private void requireCompanyId(Manager manager, Long expected) {
        if (expected == null || manager.getCompany() == null || manager.getCompany().id == null)
            throw new SecurityException("Company not found");
        if (!manager.getCompany().id.equals(expected)) throw new SecurityException("Tenant mismatch");
    }
}
