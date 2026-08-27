package com.bnfix.ubm.domains.benefit;

import com.bnfix.ubm.domains.account.AccountRepository;
import com.bnfix.ubm.domains.benefit.dto.EmployeeBenefitResponse;
import com.bnfix.ubm.domains.category.dto.CategoryResponse;
import com.bnfix.ubm.domains.employee.Employee;
import com.bnfix.ubm.domains.employee.EmployeeRepository;
import com.bnfix.ubm.domains.redemption.BenefitRedemptionRepository;
import com.bnfix.ubm.shared.security.AccessStatusGuard;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeBenefitService {
    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final BenefitRepository benefitRepository;
    private final BenefitRedemptionRepository benefitRedemptionRepository;
    private final BenefitAccessPolicy benefitAccessPolicy;

    public EmployeeBenefitService(
            AccountRepository accountRepository,
            EmployeeRepository employeeRepository,
            BenefitRepository benefitRepository,
            BenefitRedemptionRepository benefitRedemptionRepository,
            BenefitAccessPolicy benefitAccessPolicy) {
        this.accountRepository = accountRepository;
        this.employeeRepository = employeeRepository;
        this.benefitRepository = benefitRepository;
        this.benefitRedemptionRepository = benefitRedemptionRepository;
        this.benefitAccessPolicy = benefitAccessPolicy;
    }

    @Transactional(readOnly = true)
    public List<EmployeeBenefitResponse> findAvailable(String email) {
        Employee employee = employee(email);
        LocalDateTime now = LocalDateTime.now();
        return benefitRepository.findAccessibleByCompany(employee.getCompany().id).stream()
                .filter(benefit -> benefitAccessPolicy.isEligible(employee, benefit, now))
                .map(benefit -> response(employee, benefit))
                .toList();
    }

    private Employee employee(String email) {
        return accountRepository
                .findByEmail(email)
                .flatMap(account -> employeeRepository.findByAccountId(account.id))
                .map(AccessStatusGuard::requireActive)
                .orElseThrow(() -> new NoSuchElementException("Employee not found"));
    }

    private EmployeeBenefitResponse response(Employee employee, Benefit benefit) {
        long used = benefitRedemptionRepository.countByEmployeeIdAndBenefitId(employee.id, benefit.id);
        return new EmployeeBenefitResponse(
                benefit.id,
                benefit.getName(),
                benefit.getDescription(),
                benefit.getProvider().getName(),
                benefit.getCategories().stream()
                        .map(category -> new CategoryResponse(category.id, category.getName()))
                        .toList(),
                benefit.getValidUntil(),
                benefit.getMaxUsesPerUser(),
                used,
                Math.max(0, (long) benefit.getMaxUsesPerUser() - used),
                benefit.getTerms());
    }
}
