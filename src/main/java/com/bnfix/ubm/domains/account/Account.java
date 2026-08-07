package com.bnfix.ubm.domains.account;

import com.bnfix.ubm.domains.shared.domain.CPF;
import com.bnfix.ubm.domains.shared.enums.Role;
import jakarta.persistence.*;
import java.util.Locale;
import java.util.UUID;

@Entity @Table(name = "accounts")
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.UUID) public UUID id;
    @Column(nullable = false) private String name;
    @Column(unique = true, nullable = false) private String email;
    @Column(nullable = false) private String password;
    @Embedded @AttributeOverride(name = "value", column = @Column(name = "cpf", nullable = false)) private CPF cpf;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role;
    protected Account() {}
    private Account(Builder b) { name=b.name; cpf=b.cpf; password=b.password; email=b.email.trim().toLowerCase(Locale.ROOT); role=b.role; }
    public String getName(){return name;} public String getEmail(){return email;} public String getPassword(){return password;} public CPF getCpf(){return cpf;} public Role getRole(){return role;}
    public void updateEmail(String email){this.email=email.trim().toLowerCase(Locale.ROOT);} public void updatePassword(String password){this.password=password;}
    public static Builder builder(String name, CPF cpf, String password, String email, Role role){return new Builder(name,cpf,password,email,role);}
    public static class Builder { private final String name,email; private final CPF cpf; private final String password; private final Role role; public Builder(String n,CPF c,String p,String e,Role r){name=n;cpf=c;password=p;email=e;role=r;} public Account build(){return new Account(this);} }
}
