package org.acme.domains.manager;

import org.acme.domains.account.Account;
import org.acme.domains.company.Company;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "managers",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_managers_account_company",
                columnNames = {"account_id", "company_id"}
        )
)
public class Manager extends PanacheEntityBase {

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

    private Manager(Builder builder) {
        this.name = builder.name;
        this.company = builder.company;
        this.account = builder.account;
        this.companyOwner = builder.companyOwner;
    }

    public String getName() { return name; }
    public Account getAccount() { return account; }
    public Company getCompany() { return company; }
    public Boolean getActive() { return active; }
    public Boolean getCompanyOwner() { return companyOwner; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    private void setName(String name) { this.name = name; }
    private void setAccount(Account account) { this.account = account; }
    private void setCompany(Company company) { this.company = company; }
    private void setActive(Boolean active) { this.active = active; }
    private void setCompanyOwner(Boolean companyOwner) { this.companyOwner = companyOwner; }
    private void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.active = Boolean.TRUE;
        if (this.companyOwner == null) this.companyOwner = Boolean.FALSE;
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
        private Boolean companyOwner = Boolean.FALSE;

        public Builder(String name, Company company, Account account) {
            this.name = name;
            this.company = company;
            this.account = account;
        }

        public Builder companyOwner() {
            this.companyOwner = Boolean.TRUE;
            return this;
        }

        public Manager build() {
            return new Manager(this);
        }
    }

    public void defineCompany(Company company) {
        this.company = company;
    }
}
