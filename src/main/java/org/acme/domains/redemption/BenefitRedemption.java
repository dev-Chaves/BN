package org.acme.domains.redemption;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.acme.domains.company.Company;
import org.acme.domains.manager.Manager;
import org.acme.domains.subscription.Subscription;

import java.time.LocalDateTime;

@Entity
@Table(name = "benefit_redemptions")
public class BenefitRedemption extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "benefitRedemptionsSeq")
    @SequenceGenerator(name = "benefitRedemptionsSeq", sequenceName = "benefit_redemptions_SEQ", allocationSize = 50)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "token_id", nullable = false, unique = true)
    private RedemptionToken token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_company_id", nullable = false)
    private Company providerCompany;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "redeemed_by_manager_id", nullable = false)
    private Manager redeemedBy;

    @Column(name = "redeemed_at", nullable = false)
    private LocalDateTime redeemedAt;

    protected BenefitRedemption() {}

    public BenefitRedemption(Subscription subscription, RedemptionToken token, Company providerCompany, Manager redeemedBy) {
        this.subscription = subscription;
        this.token = token;
        this.providerCompany = providerCompany;
        this.redeemedBy = redeemedBy;
    }

    @PrePersist
    void onCreate() {
        redeemedAt = LocalDateTime.now();
    }

    public Subscription getSubscription() { return subscription; }
    public RedemptionToken getToken() { return token; }
    public LocalDateTime getRedeemedAt() { return redeemedAt; }
}
