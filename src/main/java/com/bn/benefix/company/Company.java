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
import java.util.List;
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
    private List<Benefit> offeredBenefits;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate(){
        this.createdAt = LocalDateTime.now();
    }

    public void addEmployee(Employee employee){
        employees.add(employee);
        employee.defineCompany(this);
    }

    public void addManager(Manager manager){
        managers.add(manager);
        manager.defineCompany(this);
    }


}
