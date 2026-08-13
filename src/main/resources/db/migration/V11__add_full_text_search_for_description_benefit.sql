ALTER TABLE benefits ALTER COLUMN description TYPE TEXT;

ALTER TABLE benefits ADD COLUMN description_tsv tsvector GENERATED ALWAYS AS (to_tsvector('portuguese', description)) STORED;

CREATE INDEX idx_benefit_description_tsv ON benefits USING GIN (description_tsv);