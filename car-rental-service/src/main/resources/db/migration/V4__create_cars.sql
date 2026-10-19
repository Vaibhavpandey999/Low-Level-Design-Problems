-- ============================================================
-- V4 – Create cars table
-- ============================================================
-- Central fleet entity.
-- version column is the @Version optimistic locking key.
-- When two transactions read version=0 and both try to UPDATE,
-- only one succeeds; the other gets ObjectOptimisticLockingFailureException.
-- ============================================================

CREATE TABLE cars (
    id             BIGSERIAL PRIMARY KEY,
    make           VARCHAR(50)       NOT NULL,           -- manufacturer brand, e.g. "Toyota"
    model          VARCHAR(50)       NOT NULL,           -- model name, e.g. "Camry"
    year           INTEGER           NOT NULL,           -- manufacturing year, e.g. 2023
    license_plate  VARCHAR(20)       NOT NULL UNIQUE,    -- vehicle registration number
    status         VARCHAR(20)       NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE/RENTED/MAINTENANCE
    daily_rate     NUMERIC(10, 2)    NOT NULL,           -- per-day rental price
    fuel_type      VARCHAR(20)       NOT NULL,           -- PETROL/DIESEL/ELECTRIC/HYBRID
    mileage        INTEGER,                              -- current odometer reading in km
    features       VARCHAR(500),                         -- comma-separated, e.g. "GPS,AC,Bluetooth"
    category_id    BIGINT            NOT NULL REFERENCES car_categories(id),  -- FK to category

    -- OPTIMISTIC LOCKING KEY: Hibernate auto-increments this on every UPDATE.
    -- Prevents double-booking race conditions without pessimistic row locks.
    version        BIGINT            NOT NULL DEFAULT 0,  -- ← @Version field in Car.java

    created_at     TIMESTAMP         NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP         NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cars_status      ON cars(status);       -- fast AVAILABLE car filter
CREATE INDEX idx_cars_category_id ON cars(category_id);  -- fast category filter

COMMENT ON COLUMN cars.version IS '@Version optimistic lock – auto-incremented by Hibernate on UPDATE';

-- Seed sample cars for dev testing
INSERT INTO cars (make, model, year, license_plate, status, daily_rate, fuel_type, mileage, features, category_id) VALUES
    ('Maruti',   'Alto K10',   2022, 'MH01AB1234', 'AVAILABLE', 899.00,  'PETROL',   12000, 'AC,Bluetooth',          1), -- ECONOMY
    ('Honda',    'City',       2023, 'MH02CD5678', 'AVAILABLE', 1299.00, 'PETROL',    5000, 'AC,GPS,Bluetooth',      2), -- COMPACT
    ('Hyundai',  'Creta',      2023, 'MH03EF9012', 'AVAILABLE', 2299.00, 'DIESEL',    8000, 'AC,GPS,Sunroof,360Cam', 3), -- SUV
    ('BMW',      '5 Series',   2024, 'DL01GH3456', 'AVAILABLE', 4799.00, 'PETROL',    3000, 'AC,GPS,Leather,Sunroof',4), -- LUXURY
    ('Toyota',   'Innova',     2022, 'KA01IJ7890', 'AVAILABLE', 3299.00, 'DIESEL',   20000, 'AC,GPS,7Seater',        5); -- VAN

