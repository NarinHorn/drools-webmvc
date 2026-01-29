-- ============================================
-- Phase 2 Migration: Work Groups
-- ============================================

-- ============================================
-- 1. WORK_GROUPS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS work_groups (
    id BIGSERIAL PRIMARY KEY,
    work_group_name VARCHAR(200) NOT NULL UNIQUE,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID,
    last_updated_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_work_groups_name ON work_groups(work_group_name);
CREATE INDEX IF NOT EXISTS idx_work_groups_enabled ON work_groups(enabled);

-- Create trigger to auto-update updated_at
DROP TRIGGER IF EXISTS update_work_groups_updated_at ON work_groups;
CREATE TRIGGER update_work_groups_updated_at BEFORE UPDATE ON work_groups
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 2. WORK_GROUP_USERS TABLE (Many-to-Many)
-- ============================================
CREATE TABLE IF NOT EXISTS work_group_users (
    work_group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (work_group_id, user_id),
    FOREIGN KEY (work_group_id) REFERENCES work_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_wgu_work_group_id ON work_group_users(work_group_id);
CREATE INDEX IF NOT EXISTS idx_wgu_user_id ON work_group_users(user_id);

-- ============================================
-- 3. WORK_GROUP_EQUIPMENT TABLE (Many-to-Many)
-- ============================================
CREATE TABLE IF NOT EXISTS work_group_equipment (
    work_group_id BIGINT NOT NULL,
    equipment_id BIGINT NOT NULL,
    PRIMARY KEY (work_group_id, equipment_id),
    FOREIGN KEY (work_group_id) REFERENCES work_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (equipment_id) REFERENCES equipment(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_wge_work_group_id ON work_group_equipment(work_group_id);
CREATE INDEX IF NOT EXISTS idx_wge_equipment_id ON work_group_equipment(equipment_id);

-- ============================================
-- 4. WORK_GROUP_ACCOUNTS TABLE (Many-to-Many)
-- ============================================
CREATE TABLE IF NOT EXISTS work_group_accounts (
    work_group_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    PRIMARY KEY (work_group_id, account_id),
    FOREIGN KEY (work_group_id) REFERENCES work_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_wga_work_group_id ON work_group_accounts(work_group_id);
CREATE INDEX IF NOT EXISTS idx_wga_account_id ON work_group_accounts(account_id);

-- ============================================
-- 5. WORK_GROUP_POLICIES TABLE (Many-to-Many - Policy Catalog)
-- ============================================
CREATE TABLE IF NOT EXISTS work_group_policies (
    work_group_id BIGINT NOT NULL,
    policy_id BIGINT NOT NULL,
    PRIMARY KEY (work_group_id, policy_id),
    FOREIGN KEY (work_group_id) REFERENCES work_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (policy_id) REFERENCES equipment_policies(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_wgp_work_group_id ON work_group_policies(work_group_id);
CREATE INDEX IF NOT EXISTS idx_wgp_policy_id ON work_group_policies(policy_id);
