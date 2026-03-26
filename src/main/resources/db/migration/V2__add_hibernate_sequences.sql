-- Hibernate 6 + PanacheEntity uses sequence-based id generation by default.
-- The initial schema was created with BIGSERIAL columns only, so we create
-- the expected sequences to keep schema validation consistent.

CREATE SEQUENCE IF NOT EXISTS benefits_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS companies_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS employees_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS managers_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS partnerships_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS subscriptions_SEQ START WITH 1 INCREMENT BY 50;
