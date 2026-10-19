-- ============================================================
-- V1 – Create users table
-- ============================================================
-- Stores registered customer and admin accounts.
-- email is the unique login identifier (no username).
-- password_hash stores BCrypt hash – plaintext NEVER stored.
-- role is stored as a string enum for readability.
-- ============================================================

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,           -- auto-incrementing primary key
    name            VARCHAR(100)  NOT NULL,          -- customer's full name
    email           VARCHAR(255)  NOT NULL UNIQUE,   -- login identifier + notification target
    password_hash   VARCHAR(255)  NOT NULL,          -- BCrypt 12-rounds hash
    phone           VARCHAR(20)   NOT NULL,          -- for SMS notifications
    licence_number  VARCHAR(50)   NOT NULL,          -- driving licence (required to rent)
    role            VARCHAR(20)   NOT NULL DEFAULT 'CUSTOMER', -- CUSTOMER or ADMIN
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),      -- immutable creation timestamp
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()       -- last update timestamp
);

-- Index on email for fast login lookups (AuthService.login)
CREATE INDEX idx_users_email ON users(email);

COMMENT ON TABLE  users               IS 'Registered platform users (customers and admins)';
COMMENT ON COLUMN users.password_hash IS 'BCrypt-hashed password – plaintext never stored';
COMMENT ON COLUMN users.role          IS 'CUSTOMER = end user, ADMIN = operator';

