package com.bnfix.ubm.domains.benefit;

import com.bnfix.ubm.domains.benefit.dto.*;
import com.bnfix.ubm.domains.category.*;
import com.bnfix.ubm.domains.category.dto.CategoryResponse;
import com.bnfix.ubm.domains.company.*;
import com.bnfix.ubm.domains.manager.*;
import com.bnfix.ubm.domains.subscription.CompanyBenefitAssignmentService;
import com.bnfix.ubm.shared.security.TenantGuard;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class BenefitService {
    private final BenefitRepository benefitRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;
    private final ManagerRepository managerRepository;
    private final TenantGuard tenantGuard;
    private final CompanyBenefitAssignmentService companyBenefitAssignmentService;

    public BenefitService(
            BenefitRepository benefitRepository,
            CompanyRepository companyRepository,
            CategoryRepository categoryRepository,
            ManagerRepository managerRepository,
            TenantGuard tenantGuard,
            CompanyBenefitAssignmentService companyBenefitAssignmentService) {
        this.benefitRepository = benefitRepository;
        this.companyRepository = companyRepository;
        this.categoryRepository = categoryRepository;
        this.managerRepository = managerRepository;
        this.tenantGuard = tenantGuard;
        this.companyBenefitAssignmentService = companyBenefitAssignmentService;
    }

    @Transactional
    public BenefitResponse create(CreateBenefitRequest request, String email, Long tenantId) {
        Manager manager = manager(email, tenantId);
        Company company = tenantGuard.verifyManagerCompanyAccess(manager, request.companyId());
        Set<Category> categories = fetch(request.categoryIds());
        Benefit saved = benefitRepository.save(Benefit.builder(request.name(), company)
                .description(request.description())
                .categories(categories)
                .availability(
                        request.publiclyVisible(),
                        request.validFrom(),
                        request.validUntil(),
                        request.maxUsesPerUser(),
                        request.terms())
                .build());
        log.info("Benefit {} created by manager {} for company {}", saved.id, manager.id, company.id);
        return response(saved);
    }

    @Transactional(readOnly = true)
    public Page<BenefitResponse> tenant(String email, Long id, Long categoryId, int page, int size) {
        manager(email, id);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100));
        return (categoryId == null
                        ? benefitRepository.findByCompanyId(id, pageable)
                        : benefitRepository.findByCompanyIdAndCategoryId(id, categoryId, pageable))
                .map(this::response);
    }

    @Transactional(readOnly = true)
    public Page<BenefitResponse> marketplace(String email, Long id, Long categoryId, int page, int size) {
        manager(email, id);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        return (categoryId == null
                        ? benefitRepository.findPublicAvailableByProviderNot(id, pageable)
                        : benefitRepository.findActiveByProviderNotAndCategoryId(id, categoryId, pageable))
                .map(this::response);
    }

    @Transactional
    public BenefitResponse update(Long benefitId, UpdateBenefitRequest request, String email, Long id) {
        Manager manager = manager(email, id);
        Benefit benefit = benefit(benefitId);
        tenantGuard.verifyManagerCompanyAccess(manager, benefit.getProvider().id);
        benefit.update(request.name(), request.description());
        benefit.updateAvailability(
                request.publiclyVisible(),
                request.validFrom(),
                request.validUntil(),
                request.maxUsesPerUser(),
                request.terms());
        if (request.categoryIds() != null) benefit.updateCategories(fetch(request.categoryIds()));
        log.info("Benefit {} updated by manager {}", benefitId, manager.id);
        return response(benefit);
    }

    @Transactional
    public BenefitResponse status(Long benefitId, String email, Long id, boolean active) {
        Manager manager = manager(email, id);
        Benefit benefit = benefit(benefitId);
        tenantGuard.verifyManagerCompanyAccess(manager, benefit.getProvider().id);
        if (active) {
            benefit.activeBenefit();
            companyBenefitAssignmentService.assignBenefitToActiveEmployees(benefit);
        } else benefit.deactivateBenefit();
        log.info("Benefit {} {} by manager {}", benefitId, active ? "activated" : "deactivated", manager.id);
        return response(benefit);
    }

    @Transactional
    public void delete(Long benefitId, String email, Long id) {
        Manager manager = manager(email, id);
        Benefit benefit = benefit(benefitId);
        tenantGuard.verifyManagerCompanyAccess(manager, benefit.getProvider().id);
        benefitRepository.delete(benefit);
        log.info("Benefit {} deleted by manager {}", benefitId, manager.id);
    }

    private Manager manager(String email, Long id) {
        return managerRepository
                .findByEmailAndCompanyId(email, id)
                .map(com.bnfix.ubm.shared.security.AccessStatusGuard::requireActive)
                .orElseThrow(() -> new NoSuchElementException("Manager not found"));
    }

    private Benefit benefit(Long id) {
        return benefitRepository
                .findByIdWithProviderAndCategories(id)
                .orElseThrow(() -> new NoSuchElementException("Benefit not found"));
    }

    private Set<Category> fetch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Set.of();
        List<Category> found = categoryRepository.findAllById(ids);
        if (found.size() != new HashSet<>(ids).size())
            throw new NoSuchElementException("One or more categories not found");
        return Set.copyOf(found);
    }

    private BenefitResponse response(Benefit benefit) {
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
                benefit.getCategories().stream()
                        .map(category -> new CategoryResponse(category.id, category.getName()))
                        .toList());
    }
}
