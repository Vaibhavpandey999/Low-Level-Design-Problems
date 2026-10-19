-- ============================================================
-- V5 – Create car_branch_assignments table
-- ============================================================
-- Tracks which branch each car currently belongs to.
-- Separate table (not FK on cars) because cars can be relocated
-- between branches after one-way rentals.
-- ============================================================

CREATE TABLE car_branch_assignments (
    id         BIGSERIAL PRIMARY KEY,
    car_id     BIGINT NOT NULL UNIQUE REFERENCES cars(id),      -- one car has one current branch
    branch_id  BIGINT NOT NULL REFERENCES branches(id)          -- the branch it currently belongs to
);

-- Seed: assign sample cars to branches
INSERT INTO car_branch_assignments (car_id, branch_id) VALUES
    (1, 1),  -- Alto   → Mumbai Airport
    (2, 1),  -- City   → Mumbai Airport
    (3, 2),  -- Creta  → Delhi CP
    (4, 2),  -- BMW    → Delhi CP
    (5, 3);  -- Innova → Bangalore Koramangala

-- ============================================================
-- V6 – Create rentals table
-- ============================================================
-- Core booking entity – tracks the full lifecycle of each rental.
-- rental_reference is the idempotency key (unique per booking attempt).
-- ============================================================

CREATE TABLE rentals (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT          NOT NULL REFERENCES users(id),
    car_id             BIGINT          NOT NULL REFERENCES cars(id),
    pickup_branch_id   BIGINT          NOT NULL REFERENCES branches(id),
    dropoff_branch_id  BIGINT          NOT NULL REFERENCES branches(id),
    start_date         DATE            NOT NULL,               -- rental start date
    end_date           DATE            NOT NULL,               -- planned return date
    actual_return_date DATE,                                   -- actual return (null until returned)
    status             VARCHAR(20)     NOT NULL DEFAULT 'PENDING', -- state machine
    total_amount       NUMERIC(10, 2),                         -- base rental cost
    damage_charge      NUMERIC(10, 2)  DEFAULT 0,              -- damage fee assessed on return
    overstay_charge    NUMERIC(10, 2)  DEFAULT 0,              -- late return penalty
    rental_reference   VARCHAR(50)     NOT NULL UNIQUE,        -- idempotency key e.g. "CR-AB12CD34"
    created_at         TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rentals_user_id ON rentals(user_id);          -- fast user history lookup
CREATE INDEX idx_rentals_car_id  ON rentals(car_id);           -- fast car rental lookup
CREATE INDEX idx_rentals_status  ON rentals(status);           -- fast status filter

COMMENT ON COLUMN rentals.rental_reference IS 'Idempotency key – prevents duplicate bookings on retry';
COMMENT ON COLUMN rentals.status           IS 'PENDING|ACTIVE|COMPLETED|CANCELLED';

-- ============================================================
-- V7 – Create payments table
-- ============================================================

CREATE TABLE payments (
    id                     BIGSERIAL PRIMARY KEY,
    rental_id              BIGINT         NOT NULL UNIQUE REFERENCES rentals(id), -- one-to-one
    amount                 NUMERIC(10, 2) NOT NULL,
    method                 VARCHAR(20)    NOT NULL,              -- CREDIT_CARD|UPI|WALLET
    status                 VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    transaction_id         VARCHAR(100),                         -- gateway reference (null until charged)
    refund_transaction_id  VARCHAR(100),                         -- refund reference (null until refunded)
    idempotency_key        VARCHAR(50)    NOT NULL UNIQUE,       -- same as rental_reference
    created_at             TIMESTAMP      NOT NULL DEFAULT NOW(),
    completed_at           TIMESTAMP                             -- null until SUCCESS/FAILED
);

CREATE INDEX idx_payments_rental_id    ON payments(rental_id);
CREATE INDEX idx_payments_transaction  ON payments(transaction_id);

-- ============================================================
-- V8 – Create cancellation_policies table
-- ============================================================

CREATE TABLE cancellation_policies (
    id                   BIGSERIAL PRIMARY KEY,
    category_type        VARCHAR(20)    NOT NULL,               -- which car category this applies to
    hours_before_pickup  INTEGER        NOT NULL,               -- threshold window in hours
    penalty_percent      NUMERIC(5, 2)  NOT NULL                -- % of total amount charged as penalty
);

-- Seed cancellation policies:
-- ECONOMY: cancel within 12h → 25% penalty; within 2h → 50%
-- SUV:     cancel within 24h → 30% penalty; within 6h  → 60%
-- LUXURY:  cancel within 48h → 40% penalty; within 12h → 75%
INSERT INTO cancellation_policies (category_type, hours_before_pickup, penalty_percent) VALUES
    ('ECONOMY', 12, 25.00),
    ('ECONOMY',  2, 50.00),
    ('COMPACT', 12, 25.00),
    ('COMPACT',  2, 50.00),
    ('SUV',     24, 30.00),
    ('SUV',      6, 60.00),
    ('LUXURY',  48, 40.00),
    ('LUXURY',  12, 75.00),
    ('VAN',     24, 35.00),
    ('VAN',      6, 65.00);

