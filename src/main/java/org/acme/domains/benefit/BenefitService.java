package org.acme.domains.benefit;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.benefit.dto.BenefitResponse;
import org.acme.domains.benefit.dto.CreateBenefitRequest;
import org.acme.domains.company.Company;
import org.acme.domains.company.CompanyRepository;
import org.acme.domains.manager.Manager;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.shared.security.TenantGuard;

import java.util.List;

@ApplicationScoped
public class BenefitService {

    private final ManagerRepository managerRepository;

    private final BenefitRepository benefitRepository;

    private final CompanyRepository companyRepository;

    private final TenantGuard tenantGuard;

    public BenefitService(ManagerRepository managerRepository, BenefitRepository benefitRepository, CompanyRepository companyRepository, TenantGuard tenantGuard) {
        this.managerRepository = managerRepository;
        this.benefitRepository = benefitRepository;
        this.companyRepository = companyRepository;
        this.tenantGuard = tenantGuard;
    }

    public Uni<BenefitResponse> createBenefit(CreateBenefitRequest request, String managerEmail){

        return validateManager(managerEmail)
                .flatMap(manager -> tenantGuard.verifyManagerCompanyAccess(manager, request.companyId()))
                .flatMap(company -> create(request, company))
                .call(benefitRepository::persist)
                .onItem().transform(this::toResponse);

    }

    public Uni<List<BenefitResponse>> listBenefitsByTenant(Long companyId, String email){

        return validateManager(email)
                .flatMap(manager -> tenantGuard.verifyManagerCompanyAccess(manager, companyId))
                .flatMap(company -> listBenefitByCompanyId(company.id))
                .map(benefits -> benefits.stream().map(this::toResponse).toList());
    }

    private BenefitResponse toResponse (Benefit benefit){
        return new BenefitResponse(
                benefit.id,
                benefit.getName(),
                benefit.getProvider().getName(),
                benefit.getActive(),
                benefit.getCreatedAt());
    }

    private Uni<List<Benefit>> listBenefitByCompanyId(Long companyId){
        return benefitRepository.findByCompanyId(companyId).onItem().ifNull().failWith(() -> new NotFoundException("Unauthorized access: Company not found"));
    }

    private Uni<Manager> validateManager(String email){
        return managerRepository.findByEmail(email).onItem()
                .ifNull().failWith(new RuntimeException("Manager not found"));
    }

    private Uni<Benefit> create(CreateBenefitRequest request, Company company) {
        return Uni.createFrom().item(Benefit.builder(request.name(), company).build());
    }

}
