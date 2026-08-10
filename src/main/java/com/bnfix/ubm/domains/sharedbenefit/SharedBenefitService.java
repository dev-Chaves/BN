package com.bnfix.ubm.domains.sharedbenefit;

import com.bnfix.ubm.domains.account.*;
import com.bnfix.ubm.domains.benefit.*;
import com.bnfix.ubm.domains.benefitrequest.*;
import com.bnfix.ubm.domains.category.dto.*;
import com.bnfix.ubm.domains.employee.*;
import com.bnfix.ubm.domains.sharedbenefit.dto.*;
import com.bnfix.ubm.domains.subscription.*;
import com.bnfix.ubm.shared.security.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SharedBenefitService {
    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final BenefitRepository benefitRepository;
    private final BenefitAccessRequestRepository benefitAccessRequestRepository;
    private final SubscriptionRepository subscriptionRepository;

    public SharedBenefitService(
            AccountRepository accountRepository,
            EmployeeRepository employeeRepository,
            BenefitRepository benefitRepository,
            BenefitAccessRequestRepository benefitAccessRequestRepository,
            SubscriptionRepository subscriptionRepository) {
        this.accountRepository = accountRepository;
        this.employeeRepository = employeeRepository;
        this.benefitRepository = benefitRepository;
        this.benefitAccessRequestRepository = benefitAccessRequestRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional(readOnly = true)
    public List<SharedBenefitResponse> available(String email) {
        Employee employee = employee(email);
        Set<Long> subscribed = subscriptionRepository.findByEmployeeWithBenefit(employee.id).stream()
                .map(subscription -> subscription.getBenefit().id)
                .collect(Collectors.toSet());
        Map<Long, String> statuses = benefitAccessRequestRepository.findByEmployee(employee.id).stream()
                .collect(Collectors.toMap(
                        request -> request.getBenefit().id,
                        request -> request.getStatus().name(),
                        (existing, incoming) -> existing));
        return benefitRepository
                .findPublicAvailableByProviderNot(employee.getCompany().id, PageRequest.of(0, 100))
                .getContent()
                .stream()
                .filter(benefit -> !subscribed.contains(benefit.id))
                .map(benefit -> response(benefit, null, statuses.getOrDefault(benefit.id, "AVAILABLE_TO_REQUEST")))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SharedBenefitResponse> mine(String email) {
        return subscriptionRepository.findByEmployeeWithBenefit(employee(email).id).stream()
                .filter(subscription -> subscription.getBenefit().isOperationalAt(LocalDateTime.now()))
                .map(subscription -> response(subscription.getBenefit(), subscription.id, "APPROVED"))
                .toList();
    }

    private Employee employee(String email) {
        return accountRepository
                .findByEmail(email)
                .flatMap(account -> employeeRepository.findByAccountId(account.id))
                .map(AccessStatusGuard::requireActive)
                .orElseThrow(() -> new NoSuchElementException("Employee not found"));
    }

    private SharedBenefitResponse response(Benefit benefit, Long subscriptionId, String status) {
        return new SharedBenefitResponse(
                benefit.id,
                subscriptionId,
                benefit.getName(),
                benefit.getDescription(),
                benefit.getProvider().getName(),
                benefit.getCategories().stream()
                        .map(category -> new CategoryResponse(category.id, category.getName()))
                        .toList(),
                benefit.getValidUntil(),
                benefit.getMaxUsesPerUser(),
                benefit.getTerms(),
                status);
    }
}
