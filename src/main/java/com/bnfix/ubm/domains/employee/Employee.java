package com.bnfix.ubm.domains.employee;

import com.bnfix.ubm.domains.account.Account;
import com.bnfix.ubm.domains.company.Company;
import com.bnfix.ubm.domains.subscription.Subscription;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employeesSeq")
    @SequenceGenerator(name = "employeesSeq", sequenceName = "employees_SEQ", allocationSize = 50)
    public Long id;

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
        name = builder.name;
        company = builder.company;
        account = builder.account;
        active = builder.active;
    }

    public String getName() {
        return name;
    }

    public Account getAccount() {
        return account;
    }

    public Company getCompany() {
        return company;
    }

    public EmployeeStatus getActive() {
        return active;
    }

    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (active == null) active = EmployeeStatus.DISABLED;
    }

    public void update(String name) {
        if (name != null && !name.isBlank()) this.name = name;
    }

    public void active() {
        if (active == EmployeeStatus.ACTIVE) throw new IllegalArgumentException("This employee is already activated");
        active = EmployeeStatus.ACTIVE;
    }

    public void disable() {
        if (active == EmployeeStatus.DISABLED) throw new IllegalArgumentException("This employee is already disabled");
        active = EmployeeStatus.DISABLED;
    }

    public void defineCompany(Company company) {
        this.company = company;
    }

    public static Builder builder(String name, Company company, Account account) {
        return new Builder(name, company, account);
    }

    public static class Builder {
        private final String name;
        private final Company company;
        private final Account account;
        private EmployeeStatus active = EmployeeStatus.DISABLED;

        public Builder(String name, Company company, Account account) {
            this.name = name;
            this.company = company;
            this.account = account;
        }

        public Employee build() {
            return new Employee(this);
        }
    }
}
