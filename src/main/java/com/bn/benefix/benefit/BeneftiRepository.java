package com.bn.benefix.benefit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BeneftiRepository extends JpaRepository<Benefit, Long> {
}
