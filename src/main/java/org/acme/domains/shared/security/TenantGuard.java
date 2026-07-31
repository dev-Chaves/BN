package org.acme.domains.shared.security;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.benefit.Benefit;
import org.acme.domains.company.Company;
import org.acme.domains.company.CompanyRepository;
import org.acme.domains.employee.Employee;
import org.acme.domains.manager.Manager;
import org.acme.domains.partnership.Partnership;
import org.acme.domains.partnership.PartnershipRepository;

@ApplicationScoped
public class TenantGuard {

    private final CompanyRepository companyRepository;
    private final PartnershipRepository partnershipRepository;

    @Inject
    public TenantGuard(CompanyRepository companyRepository, PartnershipRepository partnershipRepository) {
        this.companyRepository = companyRepository;
        this.partnershipRepository = partnershipRepository;
    }

    public Uni<Company> verifyManagerCompanyAccess(Manager manager, Long companyId) {
        AccessStatusGuard.requireActive(manager);
        if (manager.getCompany() == null || manager.getCompany().id == null) {
            return Uni.createFrom().failure(new NotFoundException("Unauthorized access: Manager company not found"));
        }

        if (!manager.getCompany().id.equals(companyId)) {
            return Uni.createFrom().failure(new SecurityException("Unauthorized access: Tenant mismatch"));
        }

        return companyRepository.findById(companyId)
                .onItem().ifNull().failWith(() -> new NotFoundException("Unauthorized access: Company not found"))
                .map(company -> {
                    if (!Boolean.TRUE.equals(company.getActive())) throw new SecurityException("Company is inactive");
                    return company;
                });
    }

    public Uni<Employee> verifyManagerEmployeeAccess(Manager manager, Employee employee) {
        AccessStatusGuard.requireActive(manager);
        if (manager.getCompany() == null || manager.getCompany().id == null || employee.getCompany() == null || employee.getCompany().id == null) {
            return Uni.createFrom().failure(new NotFoundException("Unauthorized access: Company not found"));
        }

        if (!manager.getCompany().id.equals(employee.getCompany().id)) {
            return Uni.createFrom().failure(new SecurityException("Unauthorized access: Tenant mismatch"));
        }

        return Uni.createFrom().item(employee);
    }

    public Uni<Partnership> verifyManagerPartnershipProviderAccess(Manager manager, Partnership partnership) {
        AccessStatusGuard.requireActive(manager);
        if (manager.getCompany() == null || manager.getCompany().id == null) {
            return Uni.createFrom().failure(new NotFoundException("Unauthorized access: Manager company not found"));
        }

        if (partnership.getBenefit() == null || partnership.getBenefit().getProvider() == null || partnership.getBenefit().getProvider().id == null) {
            return Uni.createFrom().failure(new NotFoundException("Unauthorized access: Partnership provider not found"));
        }

        if (!manager.getCompany().id.equals(partnership.getBenefit().getProvider().id)) {
            return Uni.createFrom().failure(new SecurityException("Unauthorized access: Only provider company manager can manage partnership"));
        }

        return Uni.createFrom().item(partnership);
    }

    public Uni<Benefit> verifyEmployeeBenefitAccess(Employee employee, Benefit benefit) {
        AccessStatusGuard.requireActive(employee);
        if (employee.getCompany() == null || employee.getCompany().id == null) {
            return Uni.createFrom().failure(new NotFoundException("Unauthorized access: Company not found"));
        }

        return partnershipRepository.findExistingPartnership(employee.getCompany().id, benefit.id)
                .flatMap(partnershipExists -> {
                    if (!partnershipExists) {
                        return Uni.createFrom().failure(new SecurityException("Unauthorized access: Benefit not available for tenant"));
                    }
                    return Uni.createFrom().item(benefit);
                });
    }
}
