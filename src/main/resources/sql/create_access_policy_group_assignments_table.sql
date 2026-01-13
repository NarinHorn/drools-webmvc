-- Create access_policy_group_assignments table
-- This table links AccessPolicy entities to UserGroup entities
-- Similar to policy_group_assignments for EquipmentPolicy

CREATE TABLE IF NOT EXISTS access_policy_group_assignments (
    id BIGSERIAL PRIMARY KEY,
    access_policy_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    CONSTRAINT fk_access_policy_group_policy 
        FOREIGN KEY (access_policy_id) REFERENCES access_policies(id) ON DELETE CASCADE,
    CONSTRAINT fk_access_policy_group_group 
        FOREIGN KEY (group_id) REFERENCES user_groups(id) ON DELETE CASCADE,
    CONSTRAINT uk_access_policy_group UNIQUE (access_policy_id, group_id)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_access_policy_group_policy ON access_policy_group_assignments(access_policy_id);
CREATE INDEX IF NOT EXISTS idx_access_policy_group_group ON access_policy_group_assignments(group_id);

-- Add comment for documentation
COMMENT ON TABLE access_policy_group_assignments IS 'Links AccessPolicy to UserGroup, allowing policies to be assigned to user groups';
COMMENT ON COLUMN access_policy_group_assignments.access_policy_id IS 'Reference to the AccessPolicy';
COMMENT ON COLUMN access_policy_group_assignments.group_id IS 'Reference to the UserGroup';
