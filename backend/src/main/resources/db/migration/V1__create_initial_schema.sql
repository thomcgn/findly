CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE analysis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ
);

CREATE TABLE listing (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    analysis_id UUID NOT NULL UNIQUE,
    external_url VARCHAR(2048) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    listing_price NUMERIC(10,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'EUR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_listing_analysis FOREIGN KEY (analysis_id) REFERENCES analysis(id) ON DELETE CASCADE
);

CREATE TABLE listing_image_urls (
    listing_id UUID NOT NULL,
    image_url VARCHAR(2048) NOT NULL,
    CONSTRAINT fk_listing_image_urls_listing FOREIGN KEY (listing_id) REFERENCES listing(id) ON DELETE CASCADE
);

CREATE TABLE identified_product (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    analysis_id UUID NOT NULL UNIQUE,
    brand VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    model VARCHAR(255),
    model_number VARCHAR(255),
    category VARCHAR(255) NOT NULL,
    confidence NUMERIC(5,2) NOT NULL,
    CONSTRAINT fk_identified_product_analysis FOREIGN KEY (analysis_id) REFERENCES analysis(id) ON DELETE CASCADE
);

CREATE TABLE price_source (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    analysis_id UUID NOT NULL,
    source_name VARCHAR(255) NOT NULL,
    product_title VARCHAR(255) NOT NULL,
    price NUMERIC(10,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    condition VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_price_source_analysis FOREIGN KEY (analysis_id) REFERENCES analysis(id) ON DELETE CASCADE
);

CREATE INDEX idx_listing_analysis_id ON listing (analysis_id);
CREATE INDEX idx_price_source_analysis_id ON price_source (analysis_id);
