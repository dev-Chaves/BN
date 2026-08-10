package com.bnfix.ubm.domains.partnership;

import com.bnfix.ubm.domains.benefit.Benefit;
import com.bnfix.ubm.domains.company.Company;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "partnerships")
public class Partnership {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "partnershipsSeq")
    @SequenceGenerator(name = "partnershipsSeq", sequenceName = "partnerships_SEQ", allocationSize = 50)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_company_id", nullable = false)
    private Company clientCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benefit_id", nullable = false)
    private Benefit benefit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartnershipStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Partnership() {}

    private Partnership(Builder builder) {
        clientCompany = builder.clientCompany;
        benefit = builder.benefit;
    }

    public Company getClientCompany() {
        return clientCompany;
    }

    public Benefit getBenefit() {
        return benefit;
    }

    public PartnershipStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        status = PartnershipStatus.PENDING;
    }

    public void updateStatus(PartnershipStatus status) {
        if (status != null) this.status = status;
    }

    public static Builder builder(Company company, Benefit benefit) {
        return new Builder(company, benefit);
    }

    public static class Builder {
        private final Company clientCompany;
        private final Benefit benefit;

        public Builder(Company company, Benefit benefit) {
            clientCompany = company;
            this.benefit = benefit;
        }

        public Partnership build() {
            return new Partnership(this);
        }
    }
}
