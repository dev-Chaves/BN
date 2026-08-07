package com.bnfix.ubm.domains.company;

import com.bnfix.ubm.domains.benefit.Benefit;
import com.bnfix.ubm.domains.employee.Employee;
import com.bnfix.ubm.domains.manager.Manager;
import com.bnfix.ubm.domains.shared.domain.CNPJ;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity @Table(name="companies") public class Company {
 @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="companiesSeq") @SequenceGenerator(name="companiesSeq",sequenceName="companies_SEQ",allocationSize=50) public Long id;
 @Column(nullable=false) private String name; @Embedded @AttributeOverride(name="value",column=@Column(name="value",nullable=false)) private CNPJ cnpj;
 @Column(name="employee_count") private Integer employeeCount; @OneToMany(mappedBy="company") private Set<Employee> employees=new HashSet<>(); @OneToMany(mappedBy="company") private Set<Manager> managers=new HashSet<>(); @OneToMany(mappedBy="provider") private Set<Benefit> offeredBenefits=new HashSet<>();
 @Column(nullable=false) private Boolean active; @Column(nullable=false,name="created_at") private LocalDateTime createdAt; protected Company(){}
 private Company(Builder b){name=b.name;cnpj=b.cnpj;}
 public String getName(){return name;} public CNPJ getCnpj(){return cnpj;} public Integer getEmployeeCount(){return employeeCount;} public Set<Employee> getEmployees(){return employees;} public Set<Manager> getManagers(){return managers;} public Set<Benefit> getOfferedBenefits(){return offeredBenefits;} public Boolean getActive(){return active;} public LocalDateTime getCreatedAt(){return createdAt;}
 @PrePersist public void onCreate(){createdAt=LocalDateTime.now();active=true;} public void update(String n){if(n!=null&&!n.isBlank())name=n;} public void activeCompany(){active=true;} public void deactivateCompany(){active=false;}
 public static Builder builder(String n,CNPJ c){return new Builder(n,c);} public static class Builder{private final String name;private final CNPJ cnpj;public Builder(String n,CNPJ c){name=Objects.requireNonNull(n);cnpj=Objects.requireNonNull(c);} public Builder employee(Employee e){return this;} public Builder manager(Manager m){return this;} public Builder benefit(Benefit b){return this;} public Company build(){return new Company(this);}}
}
