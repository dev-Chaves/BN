package com.bn.benefix.manager;

import com.bn.benefix.account.Account;
import com.bn.benefix.company.Company;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "managers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter(AccessLevel.PRIVATE)
public class Manager {

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

    private Boolean active;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private Manager (Builder builder){
        this.name = builder.name;
        this.company = builder.company;
        this.account = builder.account;
    }

    @PrePersist
    public void onCreate(){
        this.createdAt = LocalDateTime.now();
        this.active = Boolean.TRUE;
    }

    public void update(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    public void activeManager(){
        this.active = Boolean.TRUE;
    }

    public void deactivateManager(){
        this.active = Boolean.FALSE;
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

        public Manager build(){
            return new Manager(this);
        }

    }

    public void defineCompany(Company company){
        this.company = company;
    }

}
