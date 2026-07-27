package org.acme.domains.redemption;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.acme.domains.subscription.Subscription;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "redemption_tokens")
public class RedemptionToken extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RedemptionTokenStatus status;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    protected RedemptionToken() {}

    public RedemptionToken(Subscription subscription, String tokenHash, LocalDateTime expiresAt) {
        this.subscription = subscription;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        issuedAt = LocalDateTime.now();
        status = RedemptionTokenStatus.ACTIVE;
    }

    public Subscription getSubscription() { return subscription; }
    public String getTokenHash() { return tokenHash; }
    public RedemptionTokenStatus getStatus() { return status; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getConsumedAt() { return consumedAt; }
}
