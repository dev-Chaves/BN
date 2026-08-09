package com.bnfix.ubm.domains.manager;

import com.bnfix.ubm.domains.account.Account;
import com.bnfix.ubm.domains.company.Company;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "managers",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_managers_account_company",
                        columnNames = {"account_id", "company_id"}))
public class Manager {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "managersSeq")
    @SequenceGenerator(name = "managersSeq", sequenceName = "managers_SEQ", allocationSize = 50)
    public Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "company_owner", nullable = false)
    private Boolean companyOwner;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    protected Manager() {}

    private Manager(Builder b) {
        name = b.name;
        company = b.company;
        account = b.account;
        companyOwner = b.companyOwner;
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

    public Boolean getActive() {
        return active;
    }

    public Boolean getCompanyOwner() {
        return companyOwner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        active = true;
        if (companyOwner == null) companyOwner = false;
    }

    public void update(String n) {
        if (n != null && !n.isBlank()) name = n;
    }

    public void activeManager() {
        active = true;
    }

    public void deactivateManager() {
        active = false;
    }

    public void defineCompany(Company c) {
        company = c;
    }

    public static Builder builder(String n, Company c, Account a) {
        return new Builder(n, c, a);
    }

    public static class Builder {
        private final String name;
        private final Company company;
        private final Account account;
        private Boolean companyOwner = false;

        public Builder(String n, Company c, Account a) {
            name = n;
            company = c;
            account = a;
        }

        public Builder companyOwner() {
            companyOwner = true;
            return this;
        }

        public Manager build() {
            return new Manager(this);
        }
    }
}
