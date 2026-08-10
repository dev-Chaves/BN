package com.bnfix.ubm.domains.subscription;

import com.bnfix.ubm.domains.account.*;
import com.bnfix.ubm.domains.benefit.*;
import com.bnfix.ubm.domains.employee.*;
import com.bnfix.ubm.domains.partnership.*;
import com.bnfix.ubm.domains.subscription.dto.*;
import com.bnfix.ubm.shared.security.*;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class SubscriptionService {
    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final BenefitRepository benefitRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PartnershipRepository partnershipRepository;
    private final TenantGuard tenantGuard;

    public SubscriptionService(
            AccountRepository accountRepository,
            EmployeeRepository employeeRepository,
            BenefitRepository benefitRepository,
            SubscriptionRepository subscriptionRepository,
            PartnershipRepository partnershipRepository,
            TenantGuard tenantGuard) {
        this.accountRepository = accountRepository;
        this.employeeRepository = employeeRepository;
        this.benefitRepository = benefitRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.partnershipRepository = partnershipRepository;
        this.tenantGuard = tenantGuard;
    }

    @Transactional
    public SubscriptionResponse subscribe(CreateSubscriptionRequest request, String email) {
        Employee employee = employee(email);
        Benefit benefit = benefitRepository
                .findByIdWithProviderAndCategories(request.benefitId())
                .orElseThrow(() -> new NoSuchElementException("Benefit not found"));
        tenantGuard.verifyEmployeeBenefitAccess(employee, benefit);
        if (!partnershipRepository.existsByClientCompanyIdAndBenefitIdAndStatus(
                employee.getCompany().id, benefit.id, PartnershipStatus.ACTIVE))
            throw new IllegalStateException("No active partnership found for this benefit and company");
        if (subscriptionRepository.existsByEmployeeAndBenefit(employee.id, benefit.id))
            throw new IllegalStateException("Employee already has an active subscription for this benefit");
        Subscription subscription = subscriptionRepository.save(
                Subscription.builder(benefit, employee).build());
        log.info("Subscription {} created for employee {} and benefit {}", subscription.id, employee.id, benefit.id);
        return new SubscriptionResponse(
                subscription.id, employee.getName(), benefit.getName(), subscription.getCreatedAt());
    }

    Employee employee(String email) {
        return accountRepository
                .findByEmail(email)
                .flatMap(account -> employeeRepository.findByAccountId(account.id))
                .map(AccessStatusGuard::requireActive)
                .orElseThrow(() -> new NoSuchElementException("Employee not found"));
    }
}
