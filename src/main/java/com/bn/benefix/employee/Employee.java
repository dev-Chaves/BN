package com.bn.benefix.employee;

import com.bn.benefix.company.Company;
import com.bn.benefix.shared.domain.CPF;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.EmbeddedColumnNaming;

import java.time.LocalDateTime;

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

    @Embedded
    @EmbeddedColumnNaming("")
    private CPF cpf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    private EmployeeStatus active;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    private Employee(Builder builder){
        this.name = builder.name;
        this.cpf = builder.cpf;
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
        private final CPF cpf;

        private Boolean active;

        public Builder(String name, CPF cpf) {
            this.name = name;
            this.cpf = cpf;
        }

        public Employee build(){
            return new Employee(this);
        }

    }

    public void defineCompany(Company company){
        this.company = company;
    }

    public void activeEmployee(EmployeeStatus val){
        if(val == EmployeeStatus.DISABLE) throw new IllegalArgumentException("This employee is disabled");
        this.active = val;
    }

    public void disableEmployee(EmployeeStatus val){
        if(val == EmployeeStatus.ACTIVE) throw new IllegalArgumentException("This employee is activated");
        this.active = val;
    }


}
