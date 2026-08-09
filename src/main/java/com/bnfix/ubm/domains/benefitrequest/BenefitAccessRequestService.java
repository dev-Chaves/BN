package com.bnfix.ubm.domains.benefitrequest;

import com.bnfix.ubm.domains.account.*;
import com.bnfix.ubm.domains.benefit.*;
import com.bnfix.ubm.domains.benefitrequest.dto.*;
import com.bnfix.ubm.domains.employee.*;
import com.bnfix.ubm.domains.manager.*;
import com.bnfix.ubm.domains.subscription.*;
import com.bnfix.ubm.shared.security.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BenefitAccessRequestService {
    private final AccountRepository accounts;
    private final EmployeeRepository employees;
    private final ManagerRepository managers;
    private final BenefitRepository benefits;
    private final BenefitAccessRequestRepository requests;
    private final SubscriptionRepository subscriptions;

    public BenefitAccessRequestService(
            AccountRepository a,
            EmployeeRepository e,
            ManagerRepository m,
            BenefitRepository b,
            BenefitAccessRequestRepository r,
            SubscriptionRepository s) {
        accounts = a;
        employees = e;
        managers = m;
        benefits = b;
        requests = r;
        subscriptions = s;
    }

    @Transactional
    public BenefitAccessRequestResponse request(String email, Long benefitId) {
        Employee e = employee(email);
        Benefit b = benefits.findByIdWithProviderAndCategories(benefitId)
                .orElseThrow(() -> new NoSuchElementException("Benefit not found"));
        if (b.getProvider().id.equals(e.getCompany().id))
            throw new IllegalArgumentException("Own-company benefit does not require sharing");
        if (!b.isAvailableAt(LocalDateTime.now())) throw new IllegalStateException("Benefit is not available");
        if (subscriptions.existsByEmployeeAndBenefit(e.id, b.id))
            throw new IllegalStateException("Benefit is already available to this employee");
        if (requests.findPending(e.id, b.id).isPresent())
            throw new IllegalStateException("A pending request already exists");
        return response(requests.save(new BenefitAccessRequest(e, b)));
    }

    @Transactional(readOnly = true)
    public List<BenefitAccessRequestResponse> mine(String email) {
        return requests.findByEmployee(employee(email).id).stream()
                .map(this::response)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BenefitAccessRequestResponse> pending(String email, Long company) {
        Manager m = manager(email, company);
        return requests.findPendingByProvider(m.getCompany().id).stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    public BenefitAccessRequestResponse approve(String email, Long company, Long id) {
        Manager m = manager(email, company);
        BenefitAccessRequest r = owned(id, m);
        AccessStatusGuard.requireActive(r.getEmployee());
        if (!r.getBenefit().isOperationalAt(LocalDateTime.now()))
            throw new IllegalStateException("Benefit is no longer available");
        r.approve(m);
        if (!subscriptions.existsByEmployeeAndBenefit(r.getEmployee().id, r.getBenefit().id))
            subscriptions.save(
                    Subscription.builder(r.getBenefit(), r.getEmployee()).build());
        return response(r);
    }

    @Transactional
    public BenefitAccessRequestResponse reject(String email, Long company, Long id, String reason) {
        Manager m = manager(email, company);
        BenefitAccessRequest r = owned(id, m);
        r.reject(m, reason);
        return response(r);
    }

    private BenefitAccessRequest owned(Long id, Manager m) {
        BenefitAccessRequest r =
                requests.findByIdWithRelations(id).orElseThrow(() -> new NoSuchElementException("Request not found"));
        if (!r.getBenefit().getProvider().id.equals(m.getCompany().id))
            throw new SecurityException("Only the provider company can review this request");
        return r;
    }

    private Employee employee(String e) {
        return accounts.findByEmail(e)
                .flatMap(a -> employees.findByAccountId(a.id))
                .map(AccessStatusGuard::requireActive)
                .orElseThrow(() -> new NoSuchElementException("Employee not found"));
    }

    private Manager manager(String e, Long id) {
        return managers.findByEmailAndCompanyId(e, id)
                .map(AccessStatusGuard::requireActive)
                .orElseThrow(() -> new NoSuchElementException("Manager not found"));
    }

    private BenefitAccessRequestResponse response(BenefitAccessRequest r) {
        return new BenefitAccessRequestResponse(
                r.id,
                r.getBenefit().id,
                r.getBenefit().getName(),
                r.getBenefit().getProvider().getName(),
                r.getEmployee().id,
                r.getEmployee().getName(),
                r.getEmployee().getCompany().getName(),
                r.getStatus(),
                r.getRequestedAt(),
                r.getReviewedAt(),
                r.getRejectionReason());
    }
}
