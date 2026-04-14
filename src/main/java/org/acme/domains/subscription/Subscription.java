package org.acme.domains.subscription;

import org.acme.domains.benefit.Benefit;
import org.acme.domains.employee.Employee;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
public class Subscription extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subscriptionsSeq")
    @SequenceGenerator(name = "subscriptionsSeq", sequenceName = "subscriptions_SEQ", allocationSize = 50)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benefit_id", nullable = false)
    private Benefit benefit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Subscription() {}

    private Subscription(Builder builder) {
        this.benefit = builder.benefit;
        this.employee = builder.employee;
    }

    public Benefit getBenefit() { return benefit; }
    public Employee getEmployee() { return employee; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    private void setBenefit(Benefit benefit) { this.benefit = benefit; }
    private void setEmployee(Employee employee) { this.employee = employee; }
    private void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static Builder builder(Benefit benefit, Employee employee) {
        return new Builder(benefit, employee);
    }

    public static class Builder {
        private final Benefit benefit;
        private final Employee employee;

        public Builder(Benefit benefit, Employee employee) {
            this.benefit = benefit;
            this.employee = employee;
        }

        public Subscription build() {
            return new Subscription(this);
        }
    }
}
