package org.acme.domains.employee;

import org.acme.domains.account.Account;
import org.acme.domains.company.Company;
import org.acme.domains.subscription.Subscription;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "employees")
public class Employee extends PanacheEntity {

    @Column(nullable = false)
    private String name;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    private EmployeeStatus active;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "employee")
    private List<Subscription> subscriptions;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    protected Employee() {}

    private Employee(Builder builder) {
        this.name = builder.name;
        this.company = builder.company;
        this.account = builder.account;
    }

    public String getName() { return name; }
    public Account getAccount() { return account; }
    public Company getCompany() { return company; }
    public EmployeeStatus getActive() { return active; }
    public List<Subscription> getSubscriptions() { return subscriptions; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    private void setName(String name) { this.name = name; }
    private void setAccount(Account account) { this.account = account; }
    private void setCompany(Company company) { this.company = company; }
    private void setActive(EmployeeStatus active) { this.active = active; }
    private void setSubscriptions(List<Subscription> subscriptions) { this.subscriptions = subscriptions; }
    private void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.active = EmployeeStatus.DISABLE;
    }

    public void update(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    public static class Builder {

        private final String name;
        private final Company company;
        private final Account account;

        public Builder(String name, Company company, Account account) {
            this.name = name;
            this.company = company;
            this.account = account;
        }

        public Employee build() {
            return new Employee(this);
        }
    }

    public void activeEmployee(EmployeeStatus val) {
        if (val == EmployeeStatus.DISABLE) throw new IllegalArgumentException("This employee is disabled");
        this.active = val;
    }

    public void disableEmployee(EmployeeStatus val) {
        if (val == EmployeeStatus.ACTIVE) throw new IllegalArgumentException("This employee is activated");
        this.active = val;
    }

    public void defineCompany(Company company) {
        this.company = company;
    }
}
