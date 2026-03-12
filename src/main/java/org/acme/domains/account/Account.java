package org.acme.domains.account;

import org.acme.domains.shared.domain.CPF;
import org.acme.domains.shared.enums.Role;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "cpf"))
    private CPF cpf;

    @Enumerated(EnumType.STRING)
    private Role role;

    protected Account() {}

    private Account(Builder builder) {
        this.name = builder.name;
        this.cpf = builder.cpf;
        this.password = builder.password;
        this.email = builder.email;
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

    public static class Builder {

        private final String name;
        private final CPF cpf;
        private final String password;
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
