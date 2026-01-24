package com.bn.benefix.employee;

import com.bn.benefix.account.Account;
import com.bn.benefix.company.Company;
import com.bn.benefix.subscription.Subscription;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "employees")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter(AccessLevel.PRIVATE)
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    private Employee(Builder builder){
        this.name = builder.name;
        this.company = builder.company;
        this.account = builder.account;
    }

    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
        this.active = EmployeeStatus.DISABLE;
    }

    public void update(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    public static class Builder{

        private final String name;
        private final Company company;
        private final Account account;

        public Builder(String name, Company company, Account account) {
            this.name = name;
            this.company = company;
            this.account = account;
        }

        public Employee build(){
            return new Employee(this);
        }

    }

    public void activeEmployee(EmployeeStatus val){
        if(val == EmployeeStatus.DISABLE) throw new IllegalArgumentException("This employee is disabled");
        this.active = val;
    }

    public void disableEmployee(EmployeeStatus val){
        if(val == EmployeeStatus.ACTIVE) throw new IllegalArgumentException("This employee is activated");
        this.active = val;
    }

    public void defineCompany(Company company){
        this.company = company;
    }


}
