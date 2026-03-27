package org.acme.domains.partnership;

import org.acme.domains.benefit.Benefit;
import org.acme.domains.company.Company;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "partnerships")
public class Partnership extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_company_id")
    private Company clientCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benefit_id")
    private Benefit benefit;

    @Enumerated(EnumType.STRING)
    private PartnershipStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected Partnership() {}

    private Partnership(Builder builder) {
        this.clientCompany = builder.clientCompany;
        this.benefit = builder.benefit;
    }

    public Company getClientCompany() { return clientCompany; }
    public Benefit getBenefit() { return benefit; }
    public PartnershipStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    private void setClientCompany(Company clientCompany) { this.clientCompany = clientCompany; }
    private void setBenefit(Benefit benefit) { this.benefit = benefit; }
    private void setStatus(PartnershipStatus status) { this.status = status; }
    private void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = PartnershipStatus.PENDING;
    }

    public void updateStatus(PartnershipStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    public static Builder builder(Company clientCompany, Benefit benefit) {
        return new Builder(clientCompany, benefit);
    }

    public static class Builder {
        private final Company clientCompany;
        private final Benefit benefit;

        public Builder(Company clientCompany, Benefit benefit) {
            this.clientCompany = clientCompany;
            this.benefit = benefit;
        }

        public Partnership build() {
            return new Partnership(this);
        }
    }
}
