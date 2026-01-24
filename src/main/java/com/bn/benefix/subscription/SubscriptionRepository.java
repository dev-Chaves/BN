package com.bn.benefix.subscription;

import com.bn.benefix.benefit.Benefit;
import com.bn.benefix.employee.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByEmployeeAndBenefit(Employee employee, Benefit benefit);
}