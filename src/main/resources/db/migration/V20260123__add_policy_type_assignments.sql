-- ===============================================================
-- Phase 3: Policy assignments by UserType and AccountType
-- ===============================================================
-- This migration adds tables for assigning policies to user types
-- and account types, enabling type-based policy inheritance.
-- Example: Assign "MFA Required" policy to SUPER_ADMIN user type,
--          and all super admins will automatically have this policy.
-- ===============================================================

-- ---------------------------------------------------------------
-- Table: policy_user_type_assignments
-- Links policies to user types (e.g., SUPER_ADMIN, NORMAL_USER)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS policy_user_type_assignments (
    id BIGSERIAL PRIMARY KEY,
    policy_id BIGINT NOT NULL,
    user_type_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_puta_policy FOREIGN KEY (policy_id) REFERENCES equipment_policies(id) ON DELETE CASCADE,
    CONSTRAINT fk_puta_user_type FOREIGN KEY (user_type_id) REFERENCES user_types(id) ON DELETE CASCADE,
    CONSTRAINT uk_puta_policy_user_type UNIQUE (policy_id, user_type_id)
);

CREATE INDEX IF NOT EXISTS idx_puta_policy_id ON policy_user_type_assignments(policy_id);
CREATE INDEX IF NOT EXISTS idx_puta_user_type_id ON policy_user_type_assignments(user_type_id);

-- ---------------------------------------------------------------
-- Table: policy_account_type_assignments
-- Links policies to account types (e.g., PRIVILEGED, SERVICE)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS policy_account_type_assignments (
    id BIGSERIAL PRIMARY KEY,
    policy_id BIGINT NOT NULL,
    account_type_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pata_policy FOREIGN KEY (policy_id) REFERENCES equipment_policies(id) ON DELETE CASCADE,
    CONSTRAINT fk_pata_account_type FOREIGN KEY (account_type_id) REFERENCES account_types(id) ON DELETE CASCADE,
    CONSTRAINT uk_pata_policy_account_type UNIQUE (policy_id, account_type_id)
);

CREATE INDEX IF NOT EXISTS idx_pata_policy_id ON policy_account_type_assignments(policy_id);
CREATE INDEX IF NOT EXISTS idx_pata_account_type_id ON policy_account_type_assignments(account_type_id);

-- ---------------------------------------------------------------
-- Add missing policy types (if not already present)
-- ---------------------------------------------------------------
INSERT INTO policy_types (type_code, type_name, description, is_active, created_at, updated_at)
VALUES ('loginControl', 'Login Control', 'User login control settings (MFA, IP filtering, account lock)', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO policy_types (type_code, type_name, description, is_active, created_at, updated_at)
VALUES ('commonSettings', 'Common Settings', 'Common policy settings (protocols, DBMS, sessions)', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO policy_types (type_code, type_name, description, is_active, created_at, updated_at)
VALUES ('allowedTime', 'Allowed Time', 'Time-based access restrictions (days, hours)', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO policy_types (type_code, type_name, description, is_active, created_at, updated_at)
VALUES ('commandSettings', 'Command Settings', 'Command blocking/whitelisting settings', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

-- ---------------------------------------------------------------
-- Sample data: Assign default policies to user types
-- ---------------------------------------------------------------
-- Create a login policy for SUPER_ADMIN with enhanced security (MFA + OTP)
INSERT INTO equipment_policies (
    policy_name,
    description,
    policy_classification,
    policy_application,
    policy_type_id,
    policy_config,
    enabled,
    priority,
    created_at,
    updated_at
) VALUES (
    'Super Admin Enhanced Login',
    'Enhanced login security for super admin users: credential + MFA (OTP)',
    'common',
    'apply',
    (SELECT id FROM policy_types WHERE type_code = 'loginControl'),
    '{"loginControl": {"loginMethod": "credential_mfa", "mfaType": "otp", "ipFilteringType": "no_restrictions", "allowedIps": [], "twoFactorType": "otp", "accountLockEnabled": true, "maxFailureAttempts": 3, "lockoutDurationMinutes": 30}}',
    true,
    200,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;

-- Create a login policy for NORMAL_USER with standard security (credential only)
INSERT INTO equipment_policies (
    policy_name,
    description,
    policy_classification,
    policy_application,
    policy_type_id,
    policy_config,
    enabled,
    priority,
    created_at,
    updated_at
) VALUES (
    'Normal User Standard Login',
    'Standard login for normal users: credential + MFA (email)',
    'common',
    'apply',
    (SELECT id FROM policy_types WHERE type_code = 'loginControl'),
    '{"loginControl": {"loginMethod": "credential_mfa", "mfaType": "email", "ipFilteringType": "no_restrictions", "allowedIps": [], "twoFactorType": "email", "accountLockEnabled": true, "maxFailureAttempts": 5, "lockoutDurationMinutes": 15}}',
    true,
    100,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;

-- Create a strict session policy for PRIVILEGED accounts
INSERT INTO equipment_policies (
    policy_name,
    description,
    policy_classification,
    policy_application,
    policy_type_id,
    policy_config,
    enabled,
    priority,
    created_at,
    updated_at
) VALUES (
    'Privileged Account Strict Session',
    'Strict session limits for privileged accounts: shorter timeout, fewer concurrent sessions',
    'common',
    'apply',
    (SELECT id FROM policy_types WHERE type_code = 'sessionTimeout'),
    '{"sessionTimeout": {"timeoutMinutes": 30, "idleTimeMinutes": 10, "warningBeforeMinutes": 5}}',
    true,
    150,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;

-- Assign policies to user types
INSERT INTO policy_user_type_assignments (policy_id, user_type_id)
SELECT ep.id, ut.id
FROM equipment_policies ep, user_types ut
WHERE ep.policy_name = 'Super Admin Enhanced Login' 
  AND ut.type_code = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO policy_user_type_assignments (policy_id, user_type_id)
SELECT ep.id, ut.id
FROM equipment_policies ep, user_types ut
WHERE ep.policy_name = 'Normal User Standard Login' 
  AND ut.type_code = 'NORMAL_USER'
ON CONFLICT DO NOTHING;

-- Assign policies to account types
INSERT INTO policy_account_type_assignments (policy_id, account_type_id)
SELECT ep.id, at.id
FROM equipment_policies ep, account_types at
WHERE ep.policy_name = 'Privileged Account Strict Session' 
  AND at.type_code = 'PRIVILEGED'
ON CONFLICT DO NOTHING;
