package org.acme.domains.benefitrequest;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.acme.domains.benefit.Benefit;
import org.acme.domains.employee.Employee;
import org.acme.domains.manager.Manager;

import java.time.LocalDateTime;

@Entity
@Table(name = "benefit_access_requests")
public class BenefitAccessRequest extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "benefitAccessRequestsSeq")
    @SequenceGenerator(name = "benefitAccessRequestsSeq", sequenceName = "benefit_access_requests_SEQ", allocationSize = 50)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "benefit_id", nullable = false)
    private Benefit benefit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BenefitAccessRequestStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_manager_id")
    private Manager reviewedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    protected BenefitAccessRequest() {}

    public BenefitAccessRequest(Employee employee, Benefit benefit) {
        this.employee = employee;
        this.benefit = benefit;
    }

    @PrePersist
    void onCreate() {
        requestedAt = LocalDateTime.now();
        status = BenefitAccessRequestStatus.PENDING;
    }

    public Employee getEmployee() { return employee; }
    public Benefit getBenefit() { return benefit; }
    public BenefitAccessRequestStatus getStatus() { return status; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public Manager getReviewedBy() { return reviewedBy; }
    public String getRejectionReason() { return rejectionReason; }

    public void approve(Manager manager) {
        ensurePending();
        status = BenefitAccessRequestStatus.APPROVED;
        reviewedBy = manager;
        reviewedAt = LocalDateTime.now();
        rejectionReason = null;
    }

    public void reject(Manager manager, String reason) {
        ensurePending();
        status = BenefitAccessRequestStatus.REJECTED;
        reviewedBy = manager;
        reviewedAt = LocalDateTime.now();
        rejectionReason = reason;
    }

    private void ensurePending() {
        if (status != BenefitAccessRequestStatus.PENDING) {
            throw new IllegalStateException("Request has already been reviewed");
        }
    }
}
