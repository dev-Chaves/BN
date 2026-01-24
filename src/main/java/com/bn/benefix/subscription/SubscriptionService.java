package com.bn.benefix.subscription;

import com.bn.benefix.account.Account;
import com.bn.benefix.benefit.Benefit;
import com.bn.benefix.benefit.BenefitRepository;
import com.bn.benefix.employee.Employee;
import com.bn.benefix.employee.EmployeeRepository;
import com.bn.benefix.manager.Manager;
import com.bn.benefix.manager.ManagerRepository;
import com.bn.benefix.partnership.Partnership;
import com.bn.benefix.partnership.PartnershipRepository;
import com.bn.benefix.partnership.PartnershipStatus;
import com.bn.benefix.shared.enums.Role;
import com.bn.benefix.subscription.dto.SubscriptionCreationRequestDTO;
import com.bn.benefix.subscription.dto.SubscriptionResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final EmployeeRepository employeeRepository;
    private final BenefitRepository benefitRepository;
    private final PartnershipRepository partnershipRepository;
    private final ManagerRepository managerRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, EmployeeRepository employeeRepository, BenefitRepository benefitRepository, PartnershipRepository partnershipRepository, ManagerRepository managerRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.employeeRepository = employeeRepository;
        this.benefitRepository = benefitRepository;
        this.partnershipRepository = partnershipRepository;
        this.managerRepository = managerRepository;
    }

    public SubscriptionResponseDTO subscribe(SubscriptionCreationRequestDTO dto, UUID accountId) {
        Employee employee = employeeRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Employee profile not found for this user"));

        Benefit benefit = benefitRepository.findById(dto.benefitId())
                .orElseThrow(() -> new EntityNotFoundException("Benefit not found"));

        Partnership partnership = partnershipRepository.findByClientCompanyAndBenefit(employee.getCompany(), benefit)
                .orElseThrow(() -> new IllegalArgumentException("Your company does not have a partnership for this benefit"));

        if (partnership.getStatus() != PartnershipStatus.ACTIVE) {
            throw new IllegalArgumentException("The partnership for this benefit is not active");
        }

        if (subscriptionRepository.findByEmployeeAndBenefit(employee, benefit).isPresent()) {
            throw new IllegalArgumentException("You are already subscribed to this benefit");
        }

        Subscription subscription = new Subscription.Builder(benefit, employee).build();
        Subscription savedSubscription = subscriptionRepository.save(subscription);

        return convertToDTO(savedSubscription);
    }

    public List<SubscriptionResponseDTO> findAll(Account requester) {
        if (requester.getRole() == Role.ADMIN) {
            return subscriptionRepository.findAll().stream()
                    .map(this::convertToDTO)
                    .toList();
        }

        if (requester.getRole() == Role.MANAGER) {
            Manager manager = managerRepository.findByAccountId(requester.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Manager profile not found"));
            
            return subscriptionRepository.findAll().stream()
                    .filter(s -> s.getEmployee().getCompany().getId().equals(manager.getCompany().getId()))
                    .map(this::convertToDTO)
                    .toList();
        }

        // USER role
        Employee employee = employeeRepository.findByAccount_Id(requester.getId())
                .orElseThrow(() -> new EntityNotFoundException("Employee profile not found"));

        return subscriptionRepository.findAll().stream()
                .filter(s -> s.getEmployee().getId().equals(employee.getId()))
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public void cancelSubscription(Long id, Account requester) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found"));

        if (requester.getRole() == Role.ADMIN) {
            subscriptionRepository.delete(subscription);
            return;
        }

        if (requester.getRole() == Role.MANAGER) {
            Manager manager = managerRepository.findByAccountId(requester.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Manager not found"));
            
            if (!subscription.getEmployee().getCompany().getId().equals(manager.getCompany().getId())) {
                 throw new SecurityException("You can only cancel subscriptions for your company's employees");
            }
            subscriptionRepository.delete(subscription);
            return;
        }

        Employee employee = employeeRepository.findByAccount_Id(requester.getId())
                .orElseThrow(() -> new EntityNotFoundException("Employee profile not found"));

        if (!subscription.getEmployee().getId().equals(employee.getId())) {
            throw new SecurityException("You can only cancel your own subscriptions");
        }

        subscriptionRepository.delete(subscription);
    }

    private SubscriptionResponseDTO convertToDTO(Subscription s) {
        return new SubscriptionResponseDTO(
                s.getId(),
                s.getEmployee().getName(),
                s.getBenefit().getName(),
                s.getCreatedAt()
        );
    }
}