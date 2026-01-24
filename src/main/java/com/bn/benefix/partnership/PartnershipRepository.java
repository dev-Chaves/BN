package com.bn.benefix.partnership;

import com.bn.benefix.benefit.Benefit;
import com.bn.benefix.company.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartnershipRepository extends JpaRepository<Partnership, Long> {

    Optional<Partnership> findByClientCompanyAndBenefit(Company clientCompany, Benefit benefit);
}

