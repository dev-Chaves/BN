package com.bn.benefix.partnership;

import com.bn.benefix.benefit.Benefit;
import com.bn.benefix.company.Company;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "partnerships")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter(AccessLevel.PRIVATE)
public class Partnership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_company_id")
    private Company clientCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benefit_id")
    private Benefit benefit;

    @Enumerated(EnumType.STRING)
    private PartnershipStatus status;

    private LocalDateTime createdAt;

    private Partnership(Builder builder){
        this.clientCompany = builder.clientCompany;
        this.benefit = builder.benefit;
    }

    @PrePersist
    public void onCreate(){
        this.createdAt = LocalDateTime.now();
        this.status = PartnershipStatus.PENDING;
    }

    public void updateStatus(PartnershipStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    public static class Builder {
        private final Company clientCompany;
        private final Benefit benefit;

        public Builder(Company clientCompany, Benefit benefit) {
            this.clientCompany = clientCompany;
            this.benefit = benefit;
        }

        public Partnership build(){
            return new Partnership(this);
        }
    }
}
