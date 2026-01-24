package com.bn.benefix.account;

import com.bn.benefix.shared.domain.CPF;
import com.bn.benefix.shared.enums.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter(AccessLevel.PRIVATE)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "cpf"))
    private CPF cpf;

    @Enumerated(EnumType.STRING)
    private Role role;

    private Account(Builder builder){
        this.name = builder.name;
        this.cpf = builder.cpf;
        this.password = builder.password;
        this.email = builder.email;
        this.role = builder.role;
    }

    public static class Builder{

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

        public Account build(){
            return new Account(this);
        }

    }


}
