package com.bn.benefix.subscription;

import com.bn.benefix.benefit.Benefit;
import com.bn.benefix.employee.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter(AccessLevel.PRIVATE)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benefit_id", nullable = false)
    private Benefit benefit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private Subscription(Builder builder) {
        this.benefit = builder.benefit;
        this.employee = builder.employee;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
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
