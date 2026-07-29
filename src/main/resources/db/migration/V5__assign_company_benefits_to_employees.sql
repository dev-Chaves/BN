INSERT INTO subscriptions (id, benefit_id, employee_id, created_at)
SELECT nextval('subscriptions_SEQ'), benefit.id, employee.id, CURRENT_TIMESTAMP
FROM benefits benefit
JOIN employees employee ON employee.company_id = benefit.provider_id
WHERE benefit.active = TRUE
  AND employee.active = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1
      FROM subscriptions subscription
      WHERE subscription.employee_id = employee.id
        AND subscription.benefit_id = benefit.id
  );

CREATE UNIQUE INDEX uq_subscription_employee_benefit
    ON subscriptions(employee_id, benefit_id);
