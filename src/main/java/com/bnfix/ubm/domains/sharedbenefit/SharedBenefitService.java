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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SharedBenefitService {
    private final AccountRepository accounts;
    private final EmployeeRepository employees;
    private final BenefitRepository benefits;
    private final BenefitAccessRequestRepository requests;
    private final SubscriptionRepository subscriptions;

    public SharedBenefitService(
            AccountRepository a,
            EmployeeRepository e,
            BenefitRepository b,
            BenefitAccessRequestRepository r,
            SubscriptionRepository s) {
        accounts = a;
        employees = e;
        benefits = b;
        requests = r;
        subscriptions = s;
    }

    @Transactional(readOnly = true)
    public List<SharedBenefitResponse> available(String email) {
        Employee e = employee(email);
        Set<Long> subscribed = subscriptions.findByEmployeeWithBenefit(e.id).stream()
                .map(x -> x.getBenefit().id)
                .collect(Collectors.toSet());
        Map<Long, String> statuses = requests.findByEmployee(e.id).stream()
                .collect(Collectors.toMap(
                        x -> x.getBenefit().id, x -> x.getStatus().name(), (a, b) -> a));
        return benefits
                .findPublicAvailableByProviderNot(
                        e.getCompany().id, org.springframework.data.domain.PageRequest.of(0, 100))
                .getContent()
                .stream()
                .filter(b -> !subscribed.contains(b.id))
                .map(b -> response(b, null, statuses.getOrDefault(b.id, "AVAILABLE_TO_REQUEST")))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SharedBenefitResponse> mine(String email) {
        return subscriptions.findByEmployeeWithBenefit(employee(email).id).stream()
                .filter(x -> x.getBenefit().isOperationalAt(LocalDateTime.now()))
                .map(x -> response(x.getBenefit(), x.id, "APPROVED"))
                .toList();
    }

    private Employee employee(String e) {
        return accounts.findByEmail(e)
                .flatMap(a -> employees.findByAccountId(a.id))
                .map(AccessStatusGuard::requireActive)
                .orElseThrow(() -> new NoSuchElementException("Employee not found"));
    }

    private SharedBenefitResponse response(Benefit b, Long sid, String status) {
        return new SharedBenefitResponse(
                b.id,
                sid,
                b.getName(),
                b.getDescription(),
                b.getProvider().getName(),
                b.getCategories().stream()
                        .map(c -> new CategoryResponse(c.id, c.getName()))
                        .toList(),
                b.getValidUntil(),
                b.getMaxUsesPerUser(),
                b.getTerms(),
                status);
    }
}
