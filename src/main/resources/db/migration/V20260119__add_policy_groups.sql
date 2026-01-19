-- Rename existing table (if not done already)
-- ALTER TABLE policy_group_assignments RENAME TO policy_user_group_assignments;

-- Create policy_groups table
CREATE TABLE IF NOT EXISTS policy_groups (
    id BIGSERIAL PRIMARY KEY,
    group_name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_by UUID,
    last_updated_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create policy_group_members table (links PolicyGroup to EquipmentPolicy)
CREATE TABLE IF NOT EXISTS policy_group_members (
    id BIGSERIAL PRIMARY KEY,
    policy_group_id BIGINT NOT NULL REFERENCES policy_groups(id) ON DELETE CASCADE,
    policy_id BIGINT NOT NULL REFERENCES equipment_policies(id) ON DELETE CASCADE,
    UNIQUE(policy_group_id, policy_id)
);

-- Create policy_group_user_assignments table
CREATE TABLE IF NOT EXISTS policy_group_user_assignments (
    id BIGSERIAL PRIMARY KEY,
    policy_group_id BIGINT NOT NULL REFERENCES policy_groups(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(policy_group_id, user_id)
);

-- Create policy_group_user_group_assignments table
CREATE TABLE IF NOT EXISTS policy_group_user_group_assignments (
    id BIGSERIAL PRIMARY KEY,
    policy_group_id BIGINT NOT NULL REFERENCES policy_groups(id) ON DELETE CASCADE,
    user_group_id BIGINT NOT NULL REFERENCES user_groups(id) ON DELETE CASCADE,
    UNIQUE(policy_group_id, user_group_id)
);

-- Create policy_group_role_assignments table
CREATE TABLE IF NOT EXISTS policy_group_role_assignments (
    id BIGSERIAL PRIMARY KEY,
    policy_group_id BIGINT NOT NULL REFERENCES policy_groups(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    UNIQUE(policy_group_id, role_id)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_policy_group_members_group ON policy_group_members(policy_group_id);
CREATE INDEX IF NOT EXISTS idx_policy_group_members_policy ON policy_group_members(policy_id);
CREATE INDEX IF NOT EXISTS idx_policy_group_user_assignments_group ON policy_group_user_assignments(policy_group_id);
CREATE INDEX IF NOT EXISTS idx_policy_group_user_assignments_user ON policy_group_user_assignments(user_id);
CREATE INDEX IF NOT EXISTS idx_policy_group_user_group_assignments_group ON policy_group_user_group_assignments(policy_group_id);
CREATE INDEX IF NOT EXISTS idx_policy_group_user_group_assignments_user_group ON policy_group_user_group_assignments(user_group_id);
CREATE INDEX IF NOT EXISTS idx_policy_group_role_assignments_group ON policy_group_role_assignments(policy_group_id);
CREATE INDEX IF NOT EXISTS idx_policy_group_role_assignments_role ON policy_group_role_assignments(role_id);
