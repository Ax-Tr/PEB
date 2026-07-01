-- Sprint 1 product schema: catalog products/services, HSN/SAC master, and price history.
-- Monetary amounts are integer paise stored in *_minor BIGINT columns (ADR-0005).

CREATE TABLE hsn_sac_master (
    code        varchar(10) PRIMARY KEY,
    description text          NOT NULL,
    gst_rate    numeric(5,2)  NOT NULL,
    kind        text          NOT NULL                -- HSN (goods) | SAC (services)
);

CREATE TABLE products (
    id                   char(26)     PRIMARY KEY,
    tenant_id            char(26)     NOT NULL,
    name                 text         NOT NULL,
    type                 text         NOT NULL,        -- GOOD | SERVICE
    hsn_sac              text         NOT NULL,        -- HSN for goods, SAC for services
    gst_rate             numeric(5,2) NOT NULL,        -- 0, 0.25, 3, 5, 12, 18, 28
    unit                 text         NOT NULL,        -- e.g. PCS, HOUR
    sale_price_minor     bigint       NOT NULL,        -- paise
    purchase_price_minor bigint       NOT NULL,        -- paise
    margin_default       numeric(5,2),                 -- optional default margin %
    created_at           timestamptz  NOT NULL DEFAULT now(),
    updated_at           timestamptz  NOT NULL DEFAULT now(),
    version              bigint       NOT NULL DEFAULT 0
);
CREATE INDEX ix_product_tenant ON products (tenant_id, created_at DESC);

CREATE TABLE price_history (
    id               char(26)    PRIMARY KEY,
    tenant_id        char(26)    NOT NULL,
    product_id       char(26)    NOT NULL REFERENCES products(id),
    sale_price_minor bigint      NOT NULL,             -- paise
    effective_at     timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_price_history_product ON price_history (product_id, effective_at DESC);

-- Seed a handful of common HSN/SAC codes for lookup/prefill.
INSERT INTO hsn_sac_master (code, description, gst_rate, kind) VALUES
    ('998314', 'IT design and development services', 18, 'SAC'),
    ('8471',   'Computers/data processing units',     18, 'HSN'),
    ('9983',   'Other professional services',         18, 'SAC'),
    ('4820',   'Registers, account books',            12, 'HSN'),
    ('9963',   'Accommodation/food services',          5, 'SAC');
