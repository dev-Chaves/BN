ALTER TABLE benefits
    ADD COLUMN available_to_provider_employees BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE redemption_tokens
    ADD COLUMN employee_id BIGINT,
    ADD COLUMN benefit_id BIGINT;

UPDATE redemption_tokens token
SET employee_id = subscription.employee_id,
    benefit_id = subscription.benefit_id
FROM subscriptions subscription
WHERE token.subscription_id = subscription.id;

-- Existing tokens were issued under the old subscription-based authorization
-- model. Force a fresh eligibility check by requiring employees to issue again.
UPDATE redemption_tokens
SET status = 'REVOKED'
WHERE status = 'ACTIVE';

ALTER TABLE redemption_tokens
    ALTER COLUMN employee_id SET NOT NULL,
    ALTER COLUMN benefit_id SET NOT NULL,
    ADD CONSTRAINT fk_redemption_token_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    ADD CONSTRAINT fk_redemption_token_benefit FOREIGN KEY (benefit_id) REFERENCES benefits (id);

ALTER TABLE benefit_redemptions
    ADD COLUMN employee_id BIGINT,
    ADD COLUMN benefit_id BIGINT,
    ADD COLUMN beneficiary_company_id BIGINT;

UPDATE benefit_redemptions redemption
SET employee_id = subscription.employee_id,
    benefit_id = subscription.benefit_id,
    beneficiary_company_id = employee.company_id
FROM subscriptions subscription
JOIN employees employee ON employee.id = subscription.employee_id
WHERE redemption.subscription_id = subscription.id;

ALTER TABLE benefit_redemptions
    ALTER COLUMN employee_id SET NOT NULL,
    ALTER COLUMN benefit_id SET NOT NULL,
    ALTER COLUMN beneficiary_company_id SET NOT NULL,
    ADD CONSTRAINT fk_redemption_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    ADD CONSTRAINT fk_redemption_benefit FOREIGN KEY (benefit_id) REFERENCES benefits (id),
    ADD CONSTRAINT fk_redemption_beneficiary_company
        FOREIGN KEY (beneficiary_company_id) REFERENCES companies (id);

DROP INDEX IF EXISTS uq_active_redemption_token;
DROP INDEX IF EXISTS idx_redemptions_subscription;

ALTER TABLE redemption_tokens
    DROP CONSTRAINT fk_redemption_token_subscription,
    DROP COLUMN subscription_id;

ALTER TABLE benefit_redemptions
    DROP CONSTRAINT fk_redemption_subscription,
    DROP COLUMN subscription_id;

CREATE UNIQUE INDEX uq_active_redemption_token_employee_benefit
    ON redemption_tokens(employee_id, benefit_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_redemption_tokens_employee ON redemption_tokens(employee_id, status);
CREATE INDEX idx_redemption_tokens_benefit ON redemption_tokens(benefit_id, status);
CREATE INDEX idx_redemptions_employee_benefit
    ON benefit_redemptions(employee_id, benefit_id, redeemed_at DESC);

DROP TABLE benefit_access_requests;
DROP TABLE subscriptions;
DROP SEQUENCE IF EXISTS benefit_access_requests_seq;
DROP SEQUENCE IF EXISTS subscriptions_seq;
