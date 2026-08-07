-- An account is a global identity. A manager row is its membership in one
-- company, so the same account may manage multiple companies while duplicate
-- memberships remain forbidden.
ALTER TABLE managers
    DROP CONSTRAINT IF EXISTS uq_managers_account;

ALTER TABLE managers
    ADD CONSTRAINT uq_managers_account_company UNIQUE (account_id, company_id);

CREATE INDEX IF NOT EXISTS idx_managers_account_active
    ON managers (account_id, active);

CREATE INDEX IF NOT EXISTS idx_managers_company_active
    ON managers (company_id, active);

UPDATE companies SET active = TRUE WHERE active IS NULL;
UPDATE managers SET active = TRUE WHERE active IS NULL;

ALTER TABLE companies ALTER COLUMN active SET DEFAULT TRUE;
ALTER TABLE companies ALTER COLUMN active SET NOT NULL;
ALTER TABLE managers ALTER COLUMN active SET DEFAULT TRUE;
ALTER TABLE managers ALTER COLUMN active SET NOT NULL;
