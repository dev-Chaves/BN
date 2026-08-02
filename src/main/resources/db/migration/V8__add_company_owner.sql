-- Closing a tenant is more destructive than ordinary manager operations.
-- Keep one explicit owner membership per existing company and reserve this
-- permission for the creator of every new company.
ALTER TABLE managers
    ADD COLUMN company_owner BOOLEAN;

UPDATE managers
SET company_owner = FALSE
WHERE company_owner IS NULL;

WITH first_manager AS (
    SELECT DISTINCT ON (company_id) id
    FROM managers
    ORDER BY company_id, active DESC, created_at, id
)
UPDATE managers
SET company_owner = TRUE
WHERE id IN (SELECT id FROM first_manager);

ALTER TABLE managers
    ALTER COLUMN company_owner SET DEFAULT FALSE,
    ALTER COLUMN company_owner SET NOT NULL;

CREATE UNIQUE INDEX uq_managers_one_owner_per_company
    ON managers (company_id)
    WHERE company_owner = TRUE;
