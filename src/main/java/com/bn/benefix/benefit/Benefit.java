package com.bn.benefix.benefit;

import com.bn.benefix.company.Company;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "benefits")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter(AccessLevel.PRIVATE)
public class Benefit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Company provider;

    private Boolean active;

    private LocalDateTime createdAt;

    private Benefit(Builder builder){
        this.name = builder.name;
        this.provider = builder.provider;
        this.description = builder.description;
    }

    @PrePersist
    public void onCreate(){
        this.createdAt = LocalDateTime.now();
        this.active = Boolean.FALSE;
    }

    public void update(String name, String description) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (description != null && !description.isBlank()) {
            this.description = description;
        }
    }

    public static class Builder{

        private final String name;
        private final Company provider;

        private String description;

        public Builder(String name, Company provider) {
            this.name = name;
            this.provider = provider;
        }

        public Builder description(String val){
            if(val.isBlank()) throw new IllegalArgumentException("Description cant be null");
            description = val;
            return this;
        }

        public Benefit build(){
            return new Benefit(this);
        }

    }

    public void activeBenefit(){
        this.active = Boolean.TRUE;
    }

    public void deactivateBenefit(){
        this.active = Boolean.FALSE;
    }

}
