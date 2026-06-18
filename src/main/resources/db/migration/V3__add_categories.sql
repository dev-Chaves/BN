-- =============================================
-- V3 – Adiciona categorias aos benefícios
-- Categorias: catálogo fixo do sistema (seed)
-- Relação N:M via benefit_categories
-- =============================================

-- 1. Tabela de categorias
CREATE TABLE categories (
    id          BIGSERIAL    NOT NULL,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id)
);

-- 2. Sequência para Hibernate
CREATE SEQUENCE IF NOT EXISTS categories_SEQ START WITH 1 INCREMENT BY 50;

-- 3. Tabela de junção N:M benefits <-> categories
CREATE TABLE benefit_categories (
    benefit_id  BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    CONSTRAINT pk_benefit_categories PRIMARY KEY (benefit_id, category_id),
    CONSTRAINT fk_bcat_benefit  FOREIGN KEY (benefit_id)  REFERENCES benefits (id),
    CONSTRAINT fk_bcat_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

-- 4. Seed: categorias pré-definidas do sistema
INSERT INTO categories (name, created_at) VALUES
    ('Saúde', NOW()),
    ('Educação', NOW()),
    ('Alimentação', NOW()),
    ('Transporte', NOW()),
    ('Lazer', NOW()),
    ('Bem-estar', NOW());
