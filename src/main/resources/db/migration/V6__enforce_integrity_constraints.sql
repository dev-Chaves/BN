-- Production may contain duplicates created before these rules existed. Preserve
-- legacy rows, but enforce uniqueness for every new insert/change. Once legacy
-- duplicates are reconciled, the fallback indexes can be replaced by unique ones.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM accounts
        WHERE email IS NOT NULL
        GROUP BY lower(trim(email)) HAVING count(*) > 1
    ) THEN
        CREATE INDEX idx_accounts_email_normalized ON accounts (lower(trim(email)));
    ELSE
        CREATE UNIQUE INDEX uq_accounts_email_normalized ON accounts (lower(trim(email)));
    END IF;

    IF EXISTS (
        SELECT 1 FROM accounts
        WHERE cpf IS NOT NULL
        GROUP BY cpf HAVING count(*) > 1
    ) THEN
        CREATE INDEX idx_accounts_cpf ON accounts (cpf);
    ELSE
        CREATE UNIQUE INDEX uq_accounts_cpf ON accounts (cpf);
    END IF;

    IF EXISTS (
        SELECT 1 FROM companies
        WHERE value IS NOT NULL
        GROUP BY value HAVING count(*) > 1
    ) THEN
        CREATE INDEX idx_companies_cnpj ON companies (value);
    ELSE
        CREATE UNIQUE INDEX uq_companies_cnpj ON companies (value);
    END IF;

    IF EXISTS (
        SELECT 1 FROM partnerships
        WHERE client_company_id IS NOT NULL AND benefit_id IS NOT NULL
        GROUP BY client_company_id, benefit_id HAVING count(*) > 1
    ) THEN
        CREATE INDEX idx_partnership_client_benefit ON partnerships (client_company_id, benefit_id);
    ELSE
        CREATE UNIQUE INDEX uq_partnership_client_benefit ON partnerships (client_company_id, benefit_id);
    END IF;
END $$;

CREATE FUNCTION reject_duplicate_account_identity() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'INSERT' OR NEW.email IS DISTINCT FROM OLD.email THEN
        PERFORM pg_advisory_xact_lock(hashtextextended('account-email:' || lower(trim(NEW.email)), 0));
        IF EXISTS (
            SELECT 1 FROM accounts account
            WHERE lower(trim(account.email)) = lower(trim(NEW.email))
              AND account.id <> NEW.id
        ) THEN
            RAISE EXCEPTION 'Account email already exists' USING ERRCODE = '23505';
        END IF;
    END IF;

    IF TG_OP = 'INSERT' OR NEW.cpf IS DISTINCT FROM OLD.cpf THEN
        PERFORM pg_advisory_xact_lock(hashtextextended('account-cpf:' || NEW.cpf, 0));
        IF EXISTS (
            SELECT 1 FROM accounts account
            WHERE account.cpf = NEW.cpf AND account.id <> NEW.id
        ) THEN
            RAISE EXCEPTION 'Account CPF already exists' USING ERRCODE = '23505';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reject_duplicate_account_identity
    BEFORE INSERT OR UPDATE OF email, cpf ON accounts
    FOR EACH ROW EXECUTE FUNCTION reject_duplicate_account_identity();

CREATE FUNCTION reject_duplicate_company_cnpj() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'UPDATE' AND NEW.value IS NOT DISTINCT FROM OLD.value THEN
        RETURN NEW;
    END IF;

    PERFORM pg_advisory_xact_lock(hashtextextended('company-cnpj:' || NEW.value, 0));
    IF EXISTS (
        SELECT 1 FROM companies company
        WHERE company.value = NEW.value AND company.id <> NEW.id
    ) THEN
        RAISE EXCEPTION 'Company CNPJ already exists' USING ERRCODE = '23505';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reject_duplicate_company_cnpj
    BEFORE INSERT OR UPDATE OF value ON companies
    FOR EACH ROW EXECUTE FUNCTION reject_duplicate_company_cnpj();

CREATE FUNCTION reject_duplicate_partnership() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'UPDATE'
       AND NEW.client_company_id IS NOT DISTINCT FROM OLD.client_company_id
       AND NEW.benefit_id IS NOT DISTINCT FROM OLD.benefit_id THEN
        RETURN NEW;
    END IF;

    PERFORM pg_advisory_xact_lock(hashtextextended(
        'partnership:' || NEW.client_company_id::text || ':' || NEW.benefit_id::text, 0
    ));
    IF EXISTS (
        SELECT 1 FROM partnerships partnership
        WHERE partnership.client_company_id = NEW.client_company_id
          AND partnership.benefit_id = NEW.benefit_id
          AND partnership.id <> NEW.id
    ) THEN
        RAISE EXCEPTION 'Partnership already exists' USING ERRCODE = '23505';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reject_duplicate_partnership
    BEFORE INSERT OR UPDATE OF client_company_id, benefit_id ON partnerships
    FOR EACH ROW EXECUTE FUNCTION reject_duplicate_partnership();

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
