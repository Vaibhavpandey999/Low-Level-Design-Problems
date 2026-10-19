-- ============================================================
-- V2 – Create car_categories table
-- ============================================================
-- Master data for car classification tiers.
-- Seeds the five standard categories with base daily rates.
-- base_daily_rate is the default; individual cars can override.
-- ============================================================

CREATE TABLE car_categories (
    id              BIGSERIAL PRIMARY KEY,
    type            VARCHAR(20)       NOT NULL UNIQUE,   -- ECONOMY/COMPACT/SUV/LUXURY/VAN
    description     VARCHAR(255)      NOT NULL,          -- human-readable description for UI
    base_daily_rate NUMERIC(10, 2)    NOT NULL           -- default daily rental rate in INR
);

COMMENT ON TABLE car_categories IS 'Car classification tiers with pricing';

-- Seed standard categories
INSERT INTO car_categories (type, description, base_daily_rate) VALUES
    ('ECONOMY',  'Budget hatchbacks – perfect for city commutes',              999.00),
    ('COMPACT',  'Mid-size sedans – comfort for short road trips',            1499.00),
    ('SUV',      'Sport utility vehicles – 7-seater family travel',           2499.00),
    ('LUXURY',   'Premium sedans and coupes – business class travel',         4999.00),
    ('VAN',      'Multi-passenger and cargo vans – group and cargo transport', 3499.00);

