-- =============================================
-- V1 – Schema inicial do Benefix
-- Criado a partir das entidades JPA existentes
-- =============================================

-- 1. accounts (PK: UUID)
CREATE TABLE accounts (
    id    UUID         NOT NULL DEFAULT gen_random_uuid(),
    name  VARCHAR(255),
    email VARCHAR(255) CONSTRAINT uq_accounts_email UNIQUE,
    password VARCHAR(255),
    cpf   VARCHAR(255),
    role  VARCHAR(255),
    CONSTRAINT pk_accounts PRIMARY KEY (id)
);

-- 2. companies (PK: BIGSERIAL)
CREATE TABLE companies (
    id             BIGSERIAL    NOT NULL,
    name           VARCHAR(255) NOT NULL,
    value          VARCHAR(255),          -- CNPJ embeddable (@EmbeddedColumnNaming(""))
    employee_count INTEGER,
    active         BOOLEAN,
    created_at     TIMESTAMP    NOT NULL,
    CONSTRAINT pk_companies PRIMARY KEY (id)
);

-- 3. employees (PK: BIGSERIAL)
CREATE TABLE employees (
    id         BIGSERIAL    NOT NULL,
    name       VARCHAR(255) NOT NULL,
    account_id UUID         NOT NULL CONSTRAINT uq_employees_account UNIQUE,
    company_id BIGINT       NOT NULL,
    active     VARCHAR(255),              -- EmployeeStatus enum (STRING)
    created_at TIMESTAMP    NOT NULL,
    CONSTRAINT pk_employees PRIMARY KEY (id),
    CONSTRAINT fk_employees_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_employees_company FOREIGN KEY (company_id) REFERENCES companies (id)
);

-- 4. managers (PK: BIGSERIAL)
CREATE TABLE managers (
    id         BIGSERIAL    NOT NULL,
    name       VARCHAR(255) NOT NULL,
    account_id UUID         NOT NULL CONSTRAINT uq_managers_account UNIQUE,
    company_id BIGINT       NOT NULL,
    active     BOOLEAN,
    created_at TIMESTAMP    NOT NULL,
    CONSTRAINT pk_managers PRIMARY KEY (id),
    CONSTRAINT fk_managers_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_managers_company FOREIGN KEY (company_id) REFERENCES companies (id)
);

-- 5. benefits (PK: BIGSERIAL)
CREATE TABLE benefits (
    id          BIGSERIAL    NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    provider_id BIGINT,
    active      BOOLEAN,
    created_at  TIMESTAMP,
    CONSTRAINT pk_benefits PRIMARY KEY (id),
    CONSTRAINT fk_benefits_provider FOREIGN KEY (provider_id) REFERENCES companies (id)
);

-- 6. partnerships (PK: BIGSERIAL)
CREATE TABLE partnerships (
    id                BIGSERIAL NOT NULL,
    client_company_id BIGINT,
    benefit_id        BIGINT,
    status            VARCHAR(255),    -- PartnershipStatus enum (STRING)
    created_at        TIMESTAMP,
    CONSTRAINT pk_partnerships PRIMARY KEY (id),
    CONSTRAINT fk_partnerships_client  FOREIGN KEY (client_company_id) REFERENCES companies (id),
    CONSTRAINT fk_partnerships_benefit FOREIGN KEY (benefit_id)        REFERENCES benefits (id)
);

-- 7. subscriptions (PK: BIGSERIAL)
CREATE TABLE subscriptions (
    id          BIGSERIAL NOT NULL,
    benefit_id  BIGINT    NOT NULL,
    employee_id BIGINT    NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    CONSTRAINT pk_subscriptions PRIMARY KEY (id),
    CONSTRAINT fk_subscriptions_benefit  FOREIGN KEY (benefit_id)  REFERENCES benefits (id),
    CONSTRAINT fk_subscriptions_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
);
