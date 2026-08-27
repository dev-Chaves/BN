package com.bnfix.ubm.domains.redemption;

import com.bnfix.ubm.domains.benefit.Benefit;
import com.bnfix.ubm.domains.company.Company;
import com.bnfix.ubm.domains.employee.Employee;
import com.bnfix.ubm.domains.manager.Manager;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "benefit_redemptions")
public class BenefitRedemption {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "benefitRedemptionsSeq")
    @SequenceGenerator(name = "benefitRedemptionsSeq", sequenceName = "benefit_redemptions_SEQ", allocationSize = 50)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "benefit_id", nullable = false)
    private Benefit benefit;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "token_id", nullable = false, unique = true)
    private RedemptionToken token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_company_id", nullable = false)
    private Company providerCompany;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiary_company_id", nullable = false)
    private Company beneficiaryCompany;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "redeemed_by_manager_id", nullable = false)
    private Manager redeemedBy;

    @Column(name = "redeemed_at", nullable = false)
    private LocalDateTime redeemedAt;

    protected BenefitRedemption() {}

    public BenefitRedemption(
            Employee employee,
            Benefit benefit,
            RedemptionToken token,
            Company providerCompany,
            Company beneficiaryCompany,
            Manager redeemedBy) {
        this.employee = employee;
        this.benefit = benefit;
        this.token = token;
        this.providerCompany = providerCompany;
        this.beneficiaryCompany = beneficiaryCompany;
        this.redeemedBy = redeemedBy;
    }

    @PrePersist
    void onCreate() {
        redeemedAt = LocalDateTime.now();
    }

    public Employee getEmployee() {
        return employee;
    }

    public Benefit getBenefit() {
        return benefit;
    }

    public RedemptionToken getToken() {
        return token;
    }

    public Company getProviderCompany() {
        return providerCompany;
    }

    public Company getBeneficiaryCompany() {
        return beneficiaryCompany;
    }

    public Manager getRedeemedBy() {
        return redeemedBy;
    }

    public LocalDateTime getRedeemedAt() {
        return redeemedAt;
    }
}
