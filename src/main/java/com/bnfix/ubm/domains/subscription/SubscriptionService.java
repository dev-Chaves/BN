package com.bnfix.ubm.domains.subscription;

import com.bnfix.ubm.domains.account.*;
import com.bnfix.ubm.domains.benefit.*;
import com.bnfix.ubm.domains.employee.*;
import com.bnfix.ubm.domains.partnership.*;
import com.bnfix.ubm.domains.subscription.dto.*;
import com.bnfix.ubm.shared.security.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {
    private final AccountRepository accounts;
    private final EmployeeRepository employees;
    private final BenefitRepository benefits;
    private final SubscriptionRepository subscriptions;
    private final PartnershipRepository partnerships;
    private final TenantGuard tenant;

    public SubscriptionService(
            AccountRepository a,
            EmployeeRepository e,
            BenefitRepository b,
            SubscriptionRepository s,
            PartnershipRepository p,
            TenantGuard t) {
        accounts = a;
        employees = e;
        benefits = b;
        subscriptions = s;
        partnerships = p;
        tenant = t;
    }

    @Transactional
    public SubscriptionResponse subscribe(CreateSubscriptionRequest r, String email) {
        Employee e = employee(email);
        Benefit b = benefits.findByIdWithProviderAndCategories(r.benefitId())
                .orElseThrow(() -> new NoSuchElementException("Benefit not found"));
        tenant.verifyEmployeeBenefitAccess(e, b);
        if (!partnerships.existsByClientCompanyIdAndBenefitIdAndStatus(
                e.getCompany().id, b.id, PartnershipStatus.ACTIVE))
            throw new IllegalStateException("No active partnership found for this benefit and company");
        if (subscriptions.existsByEmployeeAndBenefit(e.id, b.id))
            throw new IllegalStateException("Employee already has an active subscription for this benefit");
        Subscription s = subscriptions.save(Subscription.builder(b, e).build());
        return new SubscriptionResponse(s.id, e.getName(), b.getName(), s.getCreatedAt());
    }

    Employee employee(String email) {
        return accounts.findByEmail(email)
                .flatMap(a -> employees.findByAccountId(a.id))
                .map(AccessStatusGuard::requireActive)
                .orElseThrow(() -> new NoSuchElementException("Employee not found"));
    }
}
