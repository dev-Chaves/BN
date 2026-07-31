package org.acme.domains.company;

import org.acme.domains.benefit.Benefit;
import org.acme.domains.employee.Employee;
import org.acme.domains.manager.Manager;
import org.acme.domains.shared.domain.CNPJ;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "companies")
public class Company extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "companiesSeq")
    @SequenceGenerator(name = "companiesSeq", sequenceName = "companies_SEQ", allocationSize = 50)
    public Long id;

    @Column(nullable = false)
    private String name;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "value", nullable = false, unique = true))
    private CNPJ cnpj;

    @Column(name = "employee_count")
    private Integer employeeCount;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Employee> employees = new HashSet<>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Manager> managers = new HashSet<>();

    @OneToMany(mappedBy = "provider")
    private Set<Benefit> offeredBenefits;

    private Boolean active;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    protected Company() {}

    private Company(Builder builder) {
        this.name = builder.name;
        this.cnpj = builder.cnpj;

        builder.employees.forEach(this::addEmployee);
        builder.managers.forEach(this::addManager);
        builder.benefits.forEach(this::addBenefit);
    }

    public String getName() { return name; }
    public CNPJ getCnpj() { return cnpj; }
    public Integer getEmployeeCount() { return employeeCount; }
    public Set<Employee> getEmployees() { return employees; }
    public Set<Manager> getManagers() { return managers; }
    public Set<Benefit> getOfferedBenefits() { return offeredBenefits; }
    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    private void setName(String name) { this.name = name; }
    private void setCnpj(CNPJ cnpj) { this.cnpj = cnpj; }
    private void setEmployeeCount(Integer employeeCount) { this.employeeCount = employeeCount; }
    private void setEmployees(Set<Employee> employees) { this.employees = employees; }
    private void setManagers(Set<Manager> managers) { this.managers = managers; }
    private void setOfferedBenefits(Set<Benefit> offeredBenefits) { this.offeredBenefits = offeredBenefits; }
    private void setActive(Boolean active) { this.active = active; }
    private void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    private void addEmployee(Employee val) {
        this.employees.add(val);
        val.defineCompany(this);
    }

    private void addManager(Manager manager) {
        this.managers.add(manager);
        manager.defineCompany(this);
    }

    private void addBenefit(Benefit val) {
        this.offeredBenefits.add(val);
    }

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

    public void activeCompany() {
        this.active = Boolean.TRUE;
    }

    public void deactivateCompany() {
        this.active = Boolean.FALSE;
    }

    public static Builder builder(String name, CNPJ cnpj) {
        return new Builder(name, cnpj);
    }

    public static class Builder {

        private final String name;
        private final CNPJ cnpj;

        private final Set<Manager> managers = new HashSet<>();
        private final Set<Employee> employees = new HashSet<>();
        private final Set<Benefit> benefits = new HashSet<>();

        public Builder(String name, CNPJ cnpj) {
            this.name = Objects.requireNonNull(name);
            this.cnpj = Objects.requireNonNull(cnpj);
        }

        public Builder employee(Employee val) {
            this.employees.add(val);
            return this;
        }

        public Builder manager(Manager manager) {
            this.managers.add(manager);
            return this;
        }

        public Builder benefit(Benefit val) {
            this.benefits.add(val);
            return this;
        }

        public Company build() {
            return new Company(this);
        }
    }
}
