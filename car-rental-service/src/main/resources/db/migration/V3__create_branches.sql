-- ============================================================
-- V3 – Create branches table
-- ============================================================
-- Physical rental locations where cars are picked up and dropped off.
-- lat/lon enable geo-proximity queries in Elasticsearch and native SQL.
-- active flag allows soft-delete (inactive branches hidden from UI).
-- ============================================================

CREATE TABLE branches (
    id       BIGSERIAL PRIMARY KEY,
    name     VARCHAR(100)  NOT NULL,           -- human-readable branch name
    address  VARCHAR(500)  NOT NULL,           -- full postal address
    city     VARCHAR(100)  NOT NULL,           -- city for search/filter
    state    VARCHAR(100)  NOT NULL,           -- state/province
    latitude  DOUBLE PRECISION NOT NULL,       -- geo-coordinate for proximity search
    longitude DOUBLE PRECISION NOT NULL,       -- geo-coordinate for proximity search
    phone    VARCHAR(20),                      -- branch contact number (optional)
    active   BOOLEAN NOT NULL DEFAULT TRUE     -- FALSE = soft-deleted / closed
);

-- Index on city for fast branch-by-city queries
CREATE INDEX idx_branches_city ON branches(city);

COMMENT ON TABLE branches IS 'Physical car rental locations (offices/depots)';

-- Seed sample branches
INSERT INTO branches (name, address, city, state, latitude, longitude, phone) VALUES
    ('Mumbai Airport Branch',  'Terminal 2, CSIA, Andheri East, Mumbai', 'Mumbai', 'Maharashtra', 19.0896, 72.8656, '+91-22-12345678'),
    ('Delhi Connaught Place',  'N-Block, Connaught Place, New Delhi',    'Delhi',  'Delhi',       28.6315, 77.2167, '+91-11-23456789'),
    ('Bangalore Koramangala',  '5th Block, Koramangala, Bangalore',      'Bangalore', 'Karnataka', 12.9279, 77.6271, '+91-80-34567890');

