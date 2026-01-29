-- ============================================
-- Phase 1 Migration: User Types and Accounts
-- ============================================

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- ============================================
-- 1. USER_TYPES TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS user_types (
    id BIGSERIAL PRIMARY KEY,
    type_code VARCHAR(50) NOT NULL UNIQUE,
    type_name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_types_type_code ON user_types(type_code);
CREATE INDEX IF NOT EXISTS idx_user_types_active ON user_types(is_active);

-- Create trigger to auto-update updated_at (drop if exists first)
DROP TRIGGER IF EXISTS update_user_types_updated_at ON user_types;
CREATE TRIGGER update_user_types_updated_at BEFORE UPDATE ON user_types
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 2. ACCOUNT_TYPES TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS account_types (
    id BIGSERIAL PRIMARY KEY,
    type_code VARCHAR(50) NOT NULL UNIQUE,
    type_name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_account_types_type_code ON account_types(type_code);
CREATE INDEX IF NOT EXISTS idx_account_types_active ON account_types(is_active);

-- Create trigger to auto-update updated_at (drop if exists first)
DROP TRIGGER IF EXISTS update_account_types_updated_at ON account_types;
CREATE TRIGGER update_account_types_updated_at BEFORE UPDATE ON account_types
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 3. ACCOUNTS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS accounts (
    id BIGSERIAL PRIMARY KEY,
    account_name VARCHAR(200) NOT NULL,
    account_type_id BIGINT NOT NULL,
    equipment_id BIGINT NOT NULL,
    username VARCHAR(255),
    password VARCHAR(500),
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_type_id) REFERENCES account_types(id),
    FOREIGN KEY (equipment_id) REFERENCES equipment(id) ON DELETE CASCADE,
    CONSTRAINT uk_account_equipment UNIQUE (account_name, equipment_id)
);

CREATE INDEX IF NOT EXISTS idx_accounts_account_type_id ON accounts(account_type_id);
CREATE INDEX IF NOT EXISTS idx_accounts_equipment_id ON accounts(equipment_id);
CREATE INDEX IF NOT EXISTS idx_accounts_active ON accounts(is_active);

-- Create trigger to auto-update updated_at (drop if exists first)
DROP TRIGGER IF EXISTS update_accounts_updated_at ON accounts;
CREATE TRIGGER update_accounts_updated_at BEFORE UPDATE ON accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 4. ADD USER_TYPE_ID TO USERS TABLE
-- ============================================
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS user_type_id BIGINT NULL;

-- Add foreign key constraint if it doesn't exist
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_users_user_type'
    ) THEN
        ALTER TABLE users
        ADD CONSTRAINT fk_users_user_type FOREIGN KEY (user_type_id) REFERENCES user_types(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_user_type_id ON users(user_type_id);

-- ============================================
-- 5. INSERT DEFAULT USER TYPES
-- ============================================
INSERT INTO user_types (type_code, type_name, description, is_active) VALUES
    ('SUPER_ADMIN', 'Super Admin User', 'Super administrator with highest privileges', TRUE),
    ('MIDDLE_MANAGER', 'Middle Manager', 'Department or team manager', TRUE),
    ('REGULAR_USER', 'Regular User', 'Standard user with normal access', TRUE),
    ('OCCASIONAL_USER', 'Occasional User', 'User with limited or occasional access', TRUE)
ON CONFLICT (type_code) DO UPDATE SET type_name = EXCLUDED.type_name;

-- ============================================
-- 6. INSERT DEFAULT ACCOUNT TYPES
-- ============================================
INSERT INTO account_types (type_code, type_name, description, is_active) VALUES
    ('COLLECTION', 'Collection Account', 'Account used for collecting data or resources', TRUE),
    ('PRIVILEGED', 'Privileged Account', 'Account with elevated privileges', TRUE),
    ('PERSONAL', 'Personal Account', 'Personal user account', TRUE),
    ('SOLUTION', 'Solution Account', 'Account for specific solution or service', TRUE),
    ('PUBLIC', 'Public Account', 'Public or shared account', TRUE)
ON CONFLICT (type_code) DO UPDATE SET type_name = EXCLUDED.type_name;
