package com.bnfix.ubm.domains.redemption;

import com.bnfix.ubm.domains.benefit.Benefit;
import com.bnfix.ubm.domains.employee.Employee;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "redemption_tokens")
public class RedemptionToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "benefit_id", nullable = false)
    private Benefit benefit;

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

    public RedemptionToken(Employee employee, Benefit benefit, String tokenHash, LocalDateTime expiresAt) {
        this.employee = employee;
        this.benefit = benefit;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        issuedAt = LocalDateTime.now();
        status = RedemptionTokenStatus.ACTIVE;
    }

    public Employee getEmployee() {
        return employee;
    }

    public Benefit getBenefit() {
        return benefit;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public RedemptionTokenStatus getStatus() {
        return status;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getConsumedAt() {
        return consumedAt;
    }
}
