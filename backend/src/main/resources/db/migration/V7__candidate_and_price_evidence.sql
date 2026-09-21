CREATE TABLE product (
 id UUID PRIMARY KEY,
 candidate JSONB NOT NULL
);
CREATE TABLE extracted_attribute (
 id UUID PRIMARY KEY,
 analysis_id UUID NOT NULL REFERENCES analysis(id) ON DELETE CASCADE,
 evidence JSONB NOT NULL
);
CREATE INDEX extracted_attribute_analysis_idx ON extracted_attribute(analysis_id);
CREATE TABLE product_match (
 id UUID PRIMARY KEY,
 analysis_id UUID NOT NULL REFERENCES analysis(id) ON DELETE CASCADE,
 product_id UUID NOT NULL UNIQUE REFERENCES product(id),
 score JSONB NOT NULL,
 selected BOOLEAN NOT NULL,
 position INTEGER NOT NULL CHECK(position >= 0),
 UNIQUE(analysis_id, position)
);
CREATE UNIQUE INDEX product_match_one_selected ON product_match(analysis_id) WHERE selected;
CREATE TABLE price_evidence (
 id UUID PRIMARY KEY,
 analysis_id UUID NOT NULL REFERENCES analysis(id) ON DELETE CASCADE,
 quote JSONB NOT NULL,
 CHECK ((quote->>'amount')::numeric > 0),
 CHECK ((quote->>'confidence')::numeric BETWEEN 0.75 AND 1),
 CHECK (quote ?& ARRAY['amount','currency','sourceName','source','retrievedAt','kind','confidence','sourceType'])
);
CREATE INDEX price_evidence_analysis_idx ON price_evidence(analysis_id);
