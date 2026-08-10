package com.bnfix.ubm.domains.subscription;

import com.bnfix.ubm.domains.benefit.*;
import com.bnfix.ubm.domains.employee.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CompanyBenefitAssignmentService {
    private final EmployeeRepository employeeRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BenefitRepository benefitRepository;

    public CompanyBenefitAssignmentService(
            EmployeeRepository employeeRepository,
            SubscriptionRepository subscriptionRepository,
            BenefitRepository benefitRepository) {
        this.employeeRepository = employeeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.benefitRepository = benefitRepository;
    }

    @Transactional
    public void assignBenefitToActiveEmployees(Benefit benefit) {
        employeeRepository
                .findByCompanyIdAndActive(benefit.getProvider().id, EmployeeStatus.ACTIVE)
                .forEach(employee -> ensure(employee, benefit));
    }

    @Transactional
    public void assignActiveCompanyBenefits(Employee employee) {
        benefitRepository
                .findByProviderIdAndActiveTrue(employee.getCompany().id)
                .forEach(benefit -> ensure(employee, benefit));
    }

    private void ensure(Employee employee, Benefit benefit) {
        if (!subscriptionRepository.existsByEmployeeAndBenefit(employee.id, benefit.id)) {
            subscriptionRepository.save(Subscription.builder(benefit, employee).build());
            log.info("Auto-assigned benefit {} to employee {}", benefit.id, employee.id);
        }
    }
}
