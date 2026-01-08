package com.bn.benefix.company;

import com.bn.benefix.benefit.Benefit;
import com.bn.benefix.employee.Employee;
import com.bn.benefix.management.Manager;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "companies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter(AccessLevel.PRIVATE)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String cnpj;

    private Integer employeeCount;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Employee> employees = new HashSet<>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Manager> managers = new HashSet<>();

    @OneToMany(mappedBy = "provider")
    private Set<Benefit> offeredBenefits;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    private Company(Builder builder){
        this.name = builder.name;
        this.cnpj = builder.cnpj;

        builder.employees.forEach(this::addEmployee);
        builder.managers.forEach(this::addManager);
        builder.benefits.forEach(this::addBenefit);
    }

    private void addEmployee(Employee val){
        this.employees.add(val);
        val.defineCompany(this);
    }

    private void addManager(Manager manager){
        this.managers.add(manager);
        manager.defineCompany(this);
    }

    private void addBenefit(Benefit val){
        this.offeredBenefits.add(val);
    }

    @PrePersist
    public void onCreate(){
        this.createdAt = LocalDateTime.now();
    }

    public static Builder builder(String name, String cnpj){
        return new Builder(name, cnpj);
    }

    public static class Builder{

        private final String name;
        private final String cnpj;

        private final Set<Manager> managers = new HashSet<>();
        private final Set<Employee> employees = new HashSet<>();
        private final Set<Benefit> benefits = new HashSet<>();

        private Integer employeeCount = 0;

        public Builder(String name, String cnpj) {
            this.name = Objects.requireNonNull(name);
            this.cnpj = Objects.requireNonNull(cnpj);
        }


        public Builder employee(Employee val){
            this.employees.add(val);
            return this;
        }

        public Builder manager(Manager manager){
            this.managers.add(manager);
            return this;
        }

        public Builder benefit(Benefit val){
            this.benefits.add(val);
            return this;
        }

        public Company build(){
            return new Company(this);
        }

    }
}
