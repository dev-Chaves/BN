CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_benefits_description_trgm
on benefits USING GIN(lower(description) gin_trgm_ops);