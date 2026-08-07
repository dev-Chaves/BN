ALTER TABLE benefits
    ADD COLUMN publicly_visible BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN valid_from TIMESTAMP,
    ADD COLUMN valid_until TIMESTAMP,
    ADD COLUMN max_uses_per_user INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN terms TEXT;

CREATE TABLE benefit_access_requests (
    id BIGSERIAL NOT NULL,
    employee_id BIGINT NOT NULL,
    benefit_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP,
    reviewed_by_manager_id BIGINT,
    rejection_reason VARCHAR(500),
    CONSTRAINT pk_benefit_access_requests PRIMARY KEY (id),
    CONSTRAINT fk_access_request_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_access_request_benefit FOREIGN KEY (benefit_id) REFERENCES benefits (id),
    CONSTRAINT fk_access_request_manager FOREIGN KEY (reviewed_by_manager_id) REFERENCES managers (id)
);

CREATE UNIQUE INDEX uq_open_access_request
    ON benefit_access_requests(employee_id, benefit_id)
    WHERE status = 'PENDING';

CREATE TABLE redemption_tokens (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    subscription_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP,
    CONSTRAINT pk_redemption_tokens PRIMARY KEY (id),
    CONSTRAINT uq_redemption_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_redemption_token_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions (id)
);

CREATE TABLE benefit_redemptions (
    id BIGSERIAL NOT NULL,
    subscription_id BIGINT NOT NULL,
    token_id UUID NOT NULL,
    provider_company_id BIGINT NOT NULL,
    redeemed_by_manager_id BIGINT NOT NULL,
    redeemed_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_benefit_redemptions PRIMARY KEY (id),
    CONSTRAINT uq_redemption_token UNIQUE (token_id),
    CONSTRAINT fk_redemption_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions (id),
    CONSTRAINT fk_redemption_token FOREIGN KEY (token_id) REFERENCES redemption_tokens (id),
    CONSTRAINT fk_redemption_provider FOREIGN KEY (provider_company_id) REFERENCES companies (id),
    CONSTRAINT fk_redemption_manager FOREIGN KEY (redeemed_by_manager_id) REFERENCES managers (id)
);

CREATE INDEX idx_access_requests_employee ON benefit_access_requests(employee_id, requested_at DESC);
CREATE INDEX idx_access_requests_provider ON benefit_access_requests(benefit_id, status);
CREATE INDEX idx_redemption_tokens_expiry ON redemption_tokens(status, expires_at);
CREATE UNIQUE INDEX uq_active_redemption_token
    ON redemption_tokens(subscription_id)
    WHERE status = 'ACTIVE';
CREATE INDEX idx_redemptions_subscription ON benefit_redemptions(subscription_id, redeemed_at DESC);

CREATE SEQUENCE IF NOT EXISTS benefit_access_requests_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS benefit_redemptions_SEQ START WITH 1 INCREMENT BY 50;
