package org.acme.domains.benefit;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.benefit.dto.BenefitResponse;
import org.acme.domains.benefit.dto.CreateBenefitRequest;
import org.acme.domains.benefit.dto.UpdateBenefitRequest;
import org.acme.domains.company.Company;
import org.acme.domains.company.CompanyRepository;
import org.acme.domains.manager.Manager;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.shared.security.TenantGuard;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class BenefitService {
    private static final Logger LOG = Logger.getLogger(BenefitService.class);

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

    @WithTransaction
    public Uni<BenefitResponse> createBenefit(CreateBenefitRequest request, String managerEmail){

        return validateManager(managerEmail)
                .flatMap(manager -> tenantGuard.verifyManagerCompanyAccess(manager, request.companyId()))
                .flatMap(company -> create(request, company))
                .call(benefitRepository::persist)
                .onItem().transform(this::toResponse);

    }

    @WithSession
    public Uni<List<BenefitResponse>> listBenefitsByTenant(Long companyId, String email){

        return validateManager(email)
                .flatMap(manager -> tenantGuard.verifyManagerCompanyAccess(manager, companyId))
                .flatMap(company -> listBenefitByCompanyId(company.id))
                .map(benefits -> benefits.stream().map(this::toResponse).toList());
    }

    @WithSession
    public Uni<List<BenefitResponse>> managerMarketplace(String managerEmail) {
        return validateManager(managerEmail)
                .flatMap(manager -> tenantGuard.verifyManagerCompanyAccess(manager, manager.getCompany().id))
                .flatMap(company -> benefitRepository.findActiveByProviderNot(company.id))
                .map(benefits -> benefits.stream().map(this::toResponse).toList());
    }

    @WithTransaction
    public Uni<BenefitResponse> updateBenefit(Long benefitId, UpdateBenefitRequest request, String managerEmail) {
        return validateManager(managerEmail)
                .flatMap(manager -> getBenefitById(benefitId)
                        .flatMap(benefit -> tenantGuard.verifyManagerCompanyAccess(manager, benefit.getProvider().id)
                                .replaceWith(benefit)))
                .map(benefit -> {
                    benefit.update(request.name(), request.description());
                    return benefit;
                })
                .map(this::toResponse);
    }

    @WithTransaction
    public Uni<BenefitResponse> activateBenefit(Long benefitId, String managerEmail) {
        return changeBenefitStatus(benefitId, managerEmail, true);
    }

    @WithTransaction
    public Uni<BenefitResponse> deactivateBenefit(Long benefitId, String managerEmail) {
        return changeBenefitStatus(benefitId, managerEmail, false);
    }

    @WithTransaction
    public Uni<Void> deleteBenefit(Long benefitId, String managerEmail) {
        return validateManager(managerEmail)
                .flatMap(manager -> getBenefitById(benefitId)
                        .flatMap(benefit -> tenantGuard.verifyManagerCompanyAccess(manager, benefit.getProvider().id)
                                .replaceWith(benefit)))
                .flatMap(benefit -> benefitRepository.deleteById(benefit.id))
                .replaceWithVoid();
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
                .ifNull().failWith(() -> {
                    LOG.warnf("Manager not found managerEmail=%s", email);
                    return new RuntimeException("Manager not found");
                });
    }

    private Uni<Benefit> create(CreateBenefitRequest request, Company company) {
        return Uni.createFrom().item(Benefit.builder(request.name(), company).description(request.description()).build());
    }

    private Uni<Benefit> getBenefitById(Long benefitId) {
        return benefitRepository.findById(benefitId)
                .onItem().ifNull().failWith(() -> new NotFoundException("Benefit not found"));
    }

    private Uni<BenefitResponse> changeBenefitStatus(Long benefitId, String managerEmail, boolean activate) {
        return validateManager(managerEmail)
                .flatMap(manager -> getBenefitById(benefitId)
                        .flatMap(benefit -> tenantGuard.verifyManagerCompanyAccess(manager, benefit.getProvider().id)
                                .replaceWith(benefit)))
                .map(benefit -> {
                    if (activate) {
                        benefit.activeBenefit();
                    } else {
                        benefit.deactivateBenefit();
                    }
                    return benefit;
                })
                .map(this::toResponse);
    }

}
