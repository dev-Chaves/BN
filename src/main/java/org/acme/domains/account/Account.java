package org.acme.domains.account;

import org.acme.domains.shared.domain.CPF;
import org.acme.domains.shared.enums.Role;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.UUID;
import java.util.Locale;

@Entity
@Table(name = "accounts")
public class Account extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "cpf", nullable = false))
    private CPF cpf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    protected Account() {}

    private Account(Builder builder) {
        this.name = builder.name;
        this.cpf = builder.cpf;
        this.password = builder.password;
        this.email = builder.email.trim().toLowerCase(Locale.ROOT);
        this.role = builder.role;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public CPF getCpf() { return cpf; }
    public Role getRole() { return role; }

    private void setName(String name) { this.name = name; }
    private void setEmail(String email) { this.email = email; }
    private void setPassword(String password) { this.password = password; }
    private void setCpf(CPF cpf) { this.cpf = cpf; }
    private void setRole(Role role) { this.role = role; }

    public void updateEmail(String email) {
        this.email = email.trim().toLowerCase(Locale.ROOT);
    }

    public void updatePassword(String passwordHash) {
        this.password = passwordHash;
    }

    public static Builder builder(String name, CPF cpf, String password, String email, Role role) {
        return new Builder(name, cpf, password, email, role);
    }  

    public static class Builder {

        private String name;
        private CPF cpf;
        private String password;
        private final String email;
        private final Role role;

        public Builder(String name, CPF cpf, String password, String email, Role role) {
            this.name = name;
            this.cpf = cpf;
            this.password = password;
            this.email = email;
            this.role = role;
        }

        public Account build() {
            return new Account(this);
        }
    }
}
