-- Unique indexes also close check-then-insert race conditions.
CREATE UNIQUE INDEX uq_accounts_email_normalized ON accounts (lower(trim(email)));
CREATE UNIQUE INDEX uq_accounts_cpf ON accounts (cpf);
CREATE UNIQUE INDEX uq_companies_cnpj ON companies (value);
CREATE UNIQUE INDEX uq_partnership_client_benefit ON partnerships (client_company_id, benefit_id);

ALTER TABLE benefits ALTER COLUMN description TYPE VARCHAR(500);

ALTER TABLE accounts
    ADD CONSTRAINT ck_accounts_name_required CHECK (name IS NOT NULL AND btrim(name) <> '') NOT VALID,
    ADD CONSTRAINT ck_accounts_email_required CHECK (email IS NOT NULL AND btrim(email) <> '') NOT VALID,
    ADD CONSTRAINT ck_accounts_password_required CHECK (password IS NOT NULL AND btrim(password) <> '') NOT VALID,
    ADD CONSTRAINT ck_accounts_cpf_required CHECK (cpf IS NOT NULL AND btrim(cpf) <> '') NOT VALID,
    ADD CONSTRAINT ck_accounts_role_required CHECK (role IS NOT NULL) NOT VALID;

ALTER TABLE companies
    ADD CONSTRAINT ck_companies_cnpj_required CHECK (value IS NOT NULL AND btrim(value) <> '') NOT VALID;

ALTER TABLE partnerships
    ADD CONSTRAINT ck_partnership_client_required CHECK (client_company_id IS NOT NULL) NOT VALID,
    ADD CONSTRAINT ck_partnership_benefit_required CHECK (benefit_id IS NOT NULL) NOT VALID,
    ADD CONSTRAINT ck_partnership_status_required CHECK (status IS NOT NULL) NOT VALID,
    ADD CONSTRAINT ck_partnership_created_required CHECK (created_at IS NOT NULL) NOT VALID;

ALTER TABLE benefits
    ADD CONSTRAINT ck_benefit_provider_required CHECK (provider_id IS NOT NULL) NOT VALID,
    ADD CONSTRAINT ck_benefit_max_uses CHECK (max_uses_per_user >= 1) NOT VALID,
    ADD CONSTRAINT ck_benefit_validity CHECK (valid_until IS NULL OR valid_from IS NULL OR valid_until > valid_from) NOT VALID;
