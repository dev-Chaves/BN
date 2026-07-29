package org.acme.domains.benefit;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.benefit.dto.BenefitResponse;
import org.acme.domains.benefit.dto.CreateBenefitRequest;
import org.acme.domains.benefit.dto.UpdateBenefitRequest;
import org.acme.domains.category.Category;
import org.acme.domains.category.CategoryRepository;
import org.acme.domains.category.dto.CategoryResponse;
import org.acme.domains.company.Company;
import org.acme.domains.company.CompanyRepository;
import org.acme.domains.manager.Manager;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.shared.security.TenantGuard;
import org.acme.domains.subscription.CompanyBenefitAssignmentService;
import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class BenefitService {
    private static final Logger LOG = Logger.getLogger(BenefitService.class);

    private final ManagerRepository managerRepository;

    private final BenefitRepository benefitRepository;

    private final CompanyRepository companyRepository;

    private final CategoryRepository categoryRepository;

    private final TenantGuard tenantGuard;

    private final CompanyBenefitAssignmentService companyBenefitAssignmentService;

    public BenefitService(
            ManagerRepository managerRepository,
            BenefitRepository benefitRepository,
            CompanyRepository companyRepository,
            CategoryRepository categoryRepository,
            TenantGuard tenantGuard,
            CompanyBenefitAssignmentService companyBenefitAssignmentService
    ) {
        this.managerRepository = managerRepository;
        this.benefitRepository = benefitRepository;
        this.companyRepository = companyRepository;
        this.categoryRepository = categoryRepository;
        this.tenantGuard = tenantGuard;
        this.companyBenefitAssignmentService = companyBenefitAssignmentService;
    }

    @WithTransaction
    public Uni<BenefitResponse> createBenefit(CreateBenefitRequest request, String managerEmail){

        return validateManager(managerEmail)
                .flatMap(manager -> tenantGuard.verifyManagerCompanyAccess(manager, request.companyId()))
                .flatMap(company -> fetchCategories(request.categoryIds()).map(categories -> create(request, company, categories)))
                .call(benefitRepository::persist)
                .onItem().transform(this::toResponse);

    }

    @WithSession
    public Uni<List<BenefitResponse>> listBenefitsByTenant(Long companyId, String email, Optional<Long> categoryId){

        return validateManager(email)
                .flatMap(manager -> tenantGuard.verifyManagerCompanyAccess(manager, companyId))
                .flatMap(company -> listBenefitByCompanyId(company.id, categoryId))
                .flatMap(this::fetchAllBenefitAssociations)
                .map(benefits -> benefits.stream().map(this::toResponse).toList());
    }

    @WithSession
    public Uni<List<BenefitResponse>> managerMarketplace(String managerEmail, Optional<Long> categoryId) {
        return validateManager(managerEmail)
                .flatMap(manager -> tenantGuard.verifyManagerCompanyAccess(manager, manager.getCompany().id))
                .flatMap(company -> {
                    if (categoryId.isPresent()) {
                        return benefitRepository.findActiveByProviderNotAndCategoryId(company.id, categoryId.get());
                    }
                    return benefitRepository.findActiveByProviderNot(company.id);
                })
                .flatMap(this::fetchAllBenefitAssociations)
                .map(benefits -> benefits.stream().map(this::toResponse).toList());
    }

    @WithTransaction
    public Uni<BenefitResponse> updateBenefit(Long benefitId, UpdateBenefitRequest request, String managerEmail) {
        return validateManager(managerEmail)
                .flatMap(manager -> getBenefitById(benefitId)
                        .flatMap(benefit -> tenantGuard.verifyManagerCompanyAccess(manager, benefit.getProvider().id)
                                .replaceWith(benefit)))
                .flatMap(benefit -> {
                    benefit.update(request.name(), request.description());
                    benefit.updateAvailability(
                            request.publiclyVisible(),
                            request.validFrom(),
                            request.validUntil(),
                            request.maxUsesPerUser(),
                            request.terms()
                    );
                    if (request.categoryIds() != null) {
                        return fetchCategories(request.categoryIds())
                                .map(categories -> {
                                    benefit.updateCategories(categories);
                                    return benefit;
                                });
                    }
                    return Uni.createFrom().item(benefit);
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
        List<CategoryResponse> categories = benefit.getCategories().stream()
                .map(c -> new CategoryResponse(c.id, c.getName()))
                .toList();
        return new BenefitResponse(
                benefit.id,
                benefit.getName(),
                benefit.getDescription(),
                benefit.getProvider().getName(),
                benefit.getActive(),
                benefit.getPubliclyVisible(),
                benefit.getValidUntil(),
                benefit.getMaxUsesPerUser(),
                benefit.getTerms(),
                benefit.getCreatedAt(),
                categories);
    }

    private Uni<List<Benefit>> listBenefitByCompanyId(Long companyId, Optional<Long> categoryId){
        if (categoryId.isPresent()) {
            return benefitRepository.findByCompanyIdAndCategoryId(companyId, categoryId.get())
                    .onItem().ifNull().failWith(() -> new NotFoundException("Unauthorized access: Company not found"));
        }
        return benefitRepository.findByCompanyId(companyId)
                .onItem().ifNull().failWith(() -> new NotFoundException("Unauthorized access: Company not found"));
    }

    private Uni<Manager> validateManager(String email){
        return managerRepository.findByEmail(email).onItem()
                .ifNull().failWith(() -> {
                    LOG.warnf("Manager not found managerEmail=%s", email);
                    return new RuntimeException("Manager not found");
                });
    }

    private Benefit create(CreateBenefitRequest request, Company company, Set<Category> categories) {
        return Benefit.builder(request.name(), company)
                .description(request.description())
                .categories(categories)
                .availability(
                        request.publiclyVisible(),
                        request.validFrom(),
                        request.validUntil(),
                        request.maxUsesPerUser(),
                        request.terms()
                )
                .build();
    }

    private Uni<Set<Category>> fetchCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Uni.createFrom().item(Set.<Category>of());
        }
        return categoryRepository.findByIds(categoryIds)
                .map(found -> {
                    if (found.size() != categoryIds.size()) {
                        throw new NotFoundException("One or more categories not found");
                    }
                    return Set.copyOf(found);
                });
    }

    private Uni<Benefit> getBenefitById(Long benefitId) {
        return benefitRepository.findById(benefitId)
                .onItem().ifNull().failWith(() -> new NotFoundException("Benefit not found"))
                .flatMap(this::fetchBenefitAssociations);
    }

    private Uni<Benefit> fetchBenefitAssociations(Benefit benefit) {
        return Mutiny.fetch(benefit.getCategories())
                .chain(() -> Mutiny.fetch(benefit.getProvider()))
                .replaceWith(benefit);
    }

    private Uni<List<Benefit>> fetchAllBenefitAssociations(List<Benefit> benefits) {
        if (benefits.isEmpty()) {
            return Uni.createFrom().item(benefits);
        }
        return Multi.createFrom().iterable(benefits)
                .onItem().transformToUniAndConcatenate(this::fetchBenefitAssociations)
                .collect().asList();
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
                .flatMap(benefit -> activate
                        ? companyBenefitAssignmentService.assignBenefitToActiveEmployees(benefit)
                                .replaceWith(benefit)
                        : Uni.createFrom().item(benefit))
                .map(this::toResponse);
    }

}
