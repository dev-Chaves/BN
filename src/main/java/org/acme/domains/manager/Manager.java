package org.acme.domains.manager;

import org.acme.domains.account.Account;
import org.acme.domains.company.Company;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "managers")
public class Manager extends PanacheEntity {

    @Column(nullable = false)
    private String name;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "account_id", referencedColumnName = "profileId", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    private Boolean active;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Manager() {}

    private Manager(Builder builder) {
        this.name = builder.name;
        this.company = builder.company;
        this.account = builder.account;
    }

    public String getName() { return name; }
    public Account getAccount() { return account; }
    public Company getCompany() { return company; }
    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    private void setName(String name) { this.name = name; }
    private void setAccount(Account account) { this.account = account; }
    private void setCompany(Company company) { this.company = company; }
    private void setActive(Boolean active) { this.active = active; }
    private void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.active = Boolean.TRUE;
    }

    public void update(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    public void activeManager() {
        this.active = Boolean.TRUE;
    }

    public void deactivateManager() {
        this.active = Boolean.FALSE;
    }

    public static Builder builder(String name, Company company, Account account) {
        return new Builder(name, company, account);
    }

    public static class Builder {

        private String name;
        private Company company;
        private final Account account;

        public Builder(String name, Company company, Account account) {
            this.name = name;
            this.company = company;
            this.account = account;
        }

        public Manager build() {
            return new Manager(this);
        }
    }

    public void defineCompany(Company company) {
        this.company = company;
    }
}
