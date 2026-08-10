package com.bnfix.ubm.domains.benefitrequest;

import com.bnfix.ubm.domains.account.*;
import com.bnfix.ubm.domains.benefit.*;
import com.bnfix.ubm.domains.benefitrequest.dto.*;
import com.bnfix.ubm.domains.employee.*;
import com.bnfix.ubm.domains.manager.*;
import com.bnfix.ubm.domains.partnership.PartnershipRepository;
import com.bnfix.ubm.domains.subscription.*;
import com.bnfix.ubm.shared.security.*;
import java.time.LocalDateTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class BenefitAccessRequestService {
    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final ManagerRepository managerRepository;
    private final BenefitRepository benefitRepository;
    private final BenefitAccessRequestRepository benefitAccessRequestRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PartnershipRepository partnershipRepository;

    public BenefitAccessRequestService(
            AccountRepository accountRepository,
            EmployeeRepository employeeRepository,
            ManagerRepository managerRepository,
            BenefitRepository benefitRepository,
            BenefitAccessRequestRepository benefitAccessRequestRepository,
            SubscriptionRepository subscriptionRepository,
            PartnershipRepository partnershipRepository) {
        this.accountRepository = accountRepository;
        this.employeeRepository = employeeRepository;
        this.managerRepository = managerRepository;
        this.benefitRepository = benefitRepository;
        this.benefitAccessRequestRepository = benefitAccessRequestRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.partnershipRepository = partnershipRepository;
    }

    @Transactional
    public BenefitAccessRequestResponse request(String email, Long benefitId) {
        Employee employee = employee(email);
        Benefit benefit = benefitRepository
                .findByIdWithProviderAndCategories(benefitId)
                .orElseThrow(() -> new NoSuchElementException("Benefit not found"));
        if (benefit.getProvider().id.equals(employee.getCompany().id))
            throw new IllegalArgumentException("Own-company benefit does not require sharing");
        if (!benefit.isAvailableAt(LocalDateTime.now())) throw new IllegalStateException("Benefit is not available");
        if (subscriptionRepository.existsByEmployeeAndBenefit(employee.id, benefit.id))
            throw new IllegalStateException("Benefit is already available to this employee");
        if (benefitAccessRequestRepository.findPending(employee.id, benefit.id).isPresent())
            throw new IllegalStateException("A pending request already exists");
        BenefitAccessRequest request = benefitAccessRequestRepository.save(new BenefitAccessRequest(employee, benefit));
        log.info("Benefit access request {} created by employee {} for benefit {}", request.id, employee.id, benefitId);
        return response(request);
    }

    @Transactional(readOnly = true)
    public List<BenefitAccessRequestResponse> mine(String email) {
        return benefitAccessRequestRepository.findByEmployee(employee(email).id).stream()
                .map(this::response)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BenefitAccessRequestResponse> pending(String email, Long company) {
        Manager manager = manager(email, company);
        return benefitAccessRequestRepository.findPendingByProvider(manager.getCompany().id).stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    public BenefitAccessRequestResponse approve(String email, Long company, Long id) {
        Manager manager = manager(email, company);
        BenefitAccessRequest benefitAccessRequest = owned(id, manager);
        AccessStatusGuard.requireActive(benefitAccessRequest.getEmployee());
        if (!benefitAccessRequest.getBenefit().isOperationalAt(LocalDateTime.now()))
            throw new IllegalStateException("Benefit is no longer available");
        benefitAccessRequest.approve(manager);
        if (!subscriptionRepository.existsByEmployeeAndBenefit(
                benefitAccessRequest.getEmployee().id, benefitAccessRequest.getBenefit().id))
            subscriptionRepository.save(
                    Subscription.builder(benefitAccessRequest.getBenefit(), benefitAccessRequest.getEmployee())
                            .build());
        log.info("Benefit access request {} approved by manager {}", id, manager.id);
        return response(benefitAccessRequest);
    }

    @Transactional
    public BenefitAccessRequestResponse reject(String email, Long company, Long id, String reason) {
        Manager manager = manager(email, company);
        BenefitAccessRequest benefitAccessRequest = owned(id, manager);
        benefitAccessRequest.reject(manager, reason);
        log.info("Benefit access request {} rejected by manager {}", id, manager.id);
        return response(benefitAccessRequest);
    }

    private BenefitAccessRequest owned(Long id, Manager manager) {
        BenefitAccessRequest benefitAccessRequest = benefitAccessRequestRepository
                .findByIdWithRelations(id)
                .orElseThrow(() -> new NoSuchElementException("Request not found"));
        if (!benefitAccessRequest.getBenefit().getProvider().id.equals(manager.getCompany().id))
            throw new SecurityException("Only the provider company can review this request");
        return benefitAccessRequest;
    }

    private Employee employee(String email) {
        return accountRepository
                .findByEmail(email)
                .flatMap(account -> employeeRepository.findByAccountId(account.id))
                .map(AccessStatusGuard::requireActive)
                .orElseThrow(() -> new NoSuchElementException("Employee not found"));
    }

    private Manager manager(String email, Long id) {
        return managerRepository
                .findByEmailAndCompanyId(email, id)
                .map(AccessStatusGuard::requireActive)
                .orElseThrow(() -> new NoSuchElementException("Manager not found"));
    }

    private BenefitAccessRequestResponse response(BenefitAccessRequest benefitAccessRequest) {
        return new BenefitAccessRequestResponse(
                benefitAccessRequest.id,
                benefitAccessRequest.getBenefit().id,
                benefitAccessRequest.getBenefit().getName(),
                benefitAccessRequest.getBenefit().getProvider().getName(),
                benefitAccessRequest.getEmployee().id,
                benefitAccessRequest.getEmployee().getName(),
                benefitAccessRequest.getEmployee().getCompany().getName(),
                benefitAccessRequest.getStatus(),
                benefitAccessRequest.getRequestedAt(),
                benefitAccessRequest.getReviewedAt(),
                benefitAccessRequest.getRejectionReason());
    }
}
