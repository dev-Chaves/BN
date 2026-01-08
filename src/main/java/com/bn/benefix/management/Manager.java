package com.bn.benefix.management;

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

    @Column(nullable = false, unique = true)
    private String cpf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private Manager (Builder builder){
        this.name = builder.name;
        this.cpf = builder.cpf;
        this.company = builder.company;
    }

    @PrePersist
    public void onCreate(){
        this.createdAt = LocalDateTime.now();
    }

    public static class Builder{

        private final String name;
        private final String cpf;
        private final Company company;

        public Builder(String name, String cpf, Company company) {
            this.name = name;
            this.cpf = cpf;
            this.company = company;
        }

        public Manager build(){
            return new Manager(this);
        }

    }

    public void defineCompany(Company company){
        this.company = company;
    }

}
