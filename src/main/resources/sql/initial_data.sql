

SELECT
    ap.policy_name,
    ap.allowed_roles,
    ug.group_name
FROM access_policies ap
         LEFT JOIN access_policy_group_assignments apga ON ap.id = apga.access_policy_id
         LEFT JOIN user_groups ug ON apga.group_id = ug.id
WHERE ap.policy_name = 'Manager Reports Access (Role)';

SELECT
    policy_name,
    endpoint,
    http_method,
    allowed_roles,
    enabled,
    generated_drl
FROM access_policies
WHERE endpoint LIKE '/api/admin%';

select * from access_policies where id = 13;

-- Check both role and group assignments
SELECT
    ap.policy_name,
    ap.allowed_roles,
    ug.group_name
FROM access_policies ap
         LEFT JOIN access_policy_group_assignments apga ON ap.id = apga.access_policy_id
         LEFT JOIN user_groups ug ON apga.group_id = ug.id
WHERE ap.policy_name = 'Managers or IT Team Access';

-- Check group assignments
SELECT
    apga.id,
    ap.policy_name,
    ug.group_name
FROM access_policy_group_assignments apga
         JOIN access_policies ap ON apga.access_policy_id = ap.id
         JOIN user_groups ug ON apga.group_id = ug.id
WHERE ap.policy_name = 'Sales Team Reports Access (Group)';

-- Check foreign key constraints
SELECT
    tc.constraint_name,
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
         JOIN information_schema.key_column_usage AS kcu
              ON tc.constraint_name = kcu.constraint_name
         JOIN information_schema.constraint_column_usage AS ccu
              ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_name = 'access_policy_group_assignments';

-- Check if the table exists
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name = 'access_policy_group_assignments';

-- Check table structure
\d access_policy_group_assignments

-- Expected columns:
-- id (BIGSERIAL PRIMARY KEY)
-- access_policy_id (BIGINT NOT NULL)
-- group_id (BIGINT NOT NULL)

-- Verify user is in group
SELECT u.username, g.group_name
FROM users u
         JOIN user_group_members ugm ON u.id = ugm.user_id
         JOIN user_groups g ON ugm.group_id = g.id
WHERE u.username = 'admin';

-- Get all protocols for a policy
SELECT p.policy_name, pap.protocol
FROM equipment_policies p
         JOIN policy_common_settings pcs ON p.id = pcs.policy_id
         JOIN policy_allowed_protocols pap ON pcs.id = pap.policy_id
WHERE p.id = 1;

SELECT e.device_name, p.policy_name
FROM equipment e
         JOIN policy_equipment_assignments pea ON e.id = pea.equipment_id
         JOIN equipment_policies p ON pea.policy_id = p.id
WHERE p.policy_name = 'Equipment-Specific Policy';

-- Check time slots were created
SELECT day_of_week, hour_start, hour_end
FROM policy_time_slots
WHERE policy_id = (SELECT id FROM policy_allowed_time WHERE policy_id = 3)
ORDER BY day_of_week, hour_start;

-- Check policy was created
SELECT * FROM equipment_policies WHERE policy_name = 'IT Team Multi-Protocol Access';

-- Check protocols were added
SELECT protocol FROM policy_allowed_protocols
WHERE policy_id = (SELECT id FROM policy_common_settings WHERE policy_id = 2);

-- Check if all tables exist
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
                     'user_groups',
                     'user_group_members',
                     'equipment_policies',
                     'policy_user_assignments',
                     'policy_group_assignments',
                     'policy_equipment_assignments',
                     'policy_role_assignments',
                     'policy_common_settings',
                     'policy_allowed_protocols',
                     'policy_allowed_dbms',
                     'policy_allowed_time',
                     'policy_time_slots',
                     'policy_command_settings',
                     'command_lists',
                     'command_list_items',
                     'policy_command_lists',
                     'policy_login_control',
                     'policy_allowed_ips'
    )
ORDER BY table_name;

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_policy_user_assignments_user ON policy_user_assignments(user_id);
CREATE INDEX IF NOT EXISTS idx_policy_user_assignments_policy ON policy_user_assignments(policy_id);
CREATE INDEX IF NOT EXISTS idx_policy_group_assignments_group ON policy_group_assignments(group_id);
CREATE INDEX IF NOT EXISTS idx_policy_equipment_assignments_equipment ON policy_equipment_assignments(equipment_id);
CREATE INDEX IF NOT EXISTS idx_policy_enabled ON equipment_policies(enabled) WHERE enabled = true;
CREATE INDEX IF NOT EXISTS idx_policy_time_slots_policy ON policy_time_slots(policy_id);
CREATE INDEX IF NOT EXISTS idx_command_list_items_list ON command_list_items(command_list_id);

-- Migration: Convert conditions column from TEXT to JSONB
-- Run this script on your database

-- Step 1: Convert existing TEXT data to JSONB
-- This validates that existing data is valid JSON and converts it
UPDATE access_policies
SET conditions = conditions::jsonb
WHERE conditions IS NOT NULL
  AND conditions != 'null'
  AND conditions != '';

-- Step 2: Alter the column type
ALTER TABLE access_policies
    ALTER COLUMN conditions TYPE JSONB
        USING CASE
                  WHEN conditions IS NULL OR conditions = '' OR conditions = 'null' THEN NULL
                  ELSE conditions::jsonb
        END;

-- ============================================
-- Initial Data SQL Script for ABAC System
-- Updated to match current PolicyService.generateDrl() implementation
-- ============================================
-- 
-- This script inserts sample data:
-- - 4 Roles: ADMIN, MANAGER, USER, VIEWER
-- - 5 Users with their role assignments
-- - 6 Access Policies with DRL generated using current logic
-- - Sample Equipment entries
--
-- Note: 
-- - Conditions column uses JSONB type (migrated from TEXT)
-- - DRL uses package: rules.dynamic
-- - Endpoint patterns use regex conversion: /api/** -> /api(/.*)?
-- - Conditions JSON format: {"attribute":{"operator":"value","value":"value"}}
--
-- Execute this script after the database schema is created
-- ============================================

-- ============================================
-- 1. INSERT ROLES
-- ============================================
INSERT INTO roles (name, description) VALUES
('ADMIN', 'System Administrator with full access'),
('MANAGER', 'Department Manager'),
('USER', 'Regular User'),
('VIEWER', 'Read-only access')
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- 2. INSERT USERS
-- ============================================
-- Note: IDs are auto-generated, we'll reference them by username in user_roles
INSERT INTO users (username, password, email, department, level, active) VALUES
('admin', 'admin123', 'admin@example.com', 'IT', 10, true),
('manager', 'manager123', 'manager@example.com', 'SALES', 5, true),
('john', 'john123', 'john@example.com', 'SALES', 3, true),
('jane', 'jane123', 'jane@example.com', 'HR', 3, true),
('viewer', 'viewer123', 'viewer@example.com', 'GUEST', 1, true)
ON CONFLICT (username) DO NOTHING;

-- ============================================
-- 3. INSERT USER_ROLES (Many-to-Many relationships)
-- ============================================
-- Admin user gets ADMIN role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- Manager user gets MANAGER role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'manager' AND r.name = 'MANAGER'
ON CONFLICT DO NOTHING;

-- John gets USER role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'john' AND r.name = 'USER'
ON CONFLICT DO NOTHING;

-- Jane gets USER role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'jane' AND r.name = 'USER'
ON CONFLICT DO NOTHING;

-- Viewer gets VIEWER role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'viewer' AND r.name = 'VIEWER'
ON CONFLICT DO NOTHING;

-- ============================================
-- 4. INSERT ACCESS POLICIES
-- ============================================
-- Note: DRL is generated based on PolicyService.generateDrl() logic
-- The generated_drl column contains the Drools rule language code

-- Policy 1: Admin Full Access
INSERT INTO access_policies (
    policy_name, description, endpoint, http_method, allowed_roles, 
    conditions, effect, priority, enabled, generated_drl, created_at, updated_at
) VALUES (
    'Admin Full Access',
    'Administrators have full access to all endpoints',
    '/api/**',
    '*',
    '["ADMIN"]',
    NULL,
    'ALLOW',
    100,
    true,
    'package rules.dynamic;

import com.hunesion.drool_v2.model.AccessRequest;
import com.hunesion.drool_v2.model.AccessResult;

rule "Admin Full Access"
    salience 100
    when
        $request : AccessRequest(
            endpointMatches("/api(/.*)?"),
            (hasRole("ADMIN"))
        )
        $result : AccessResult(evaluated == false)
    then
        $result.allow("Admin Full Access");
        System.out.println("✓ Access ALLOWED by policy: Admin Full Access");
end
',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (policy_name) DO NOTHING;

-- Policy 2: Manager Reports Access
INSERT INTO access_policies (
    policy_name, description, endpoint, http_method, allowed_roles, 
    conditions, effect, priority, enabled, generated_drl, created_at, updated_at
) VALUES (
    'Manager Reports Access',
    'Managers can view reports',
    '/api/reports/**',
    'GET',
    '["MANAGER"]',
    NULL,
    'ALLOW',
    50,
    true,
    'package rules.dynamic;

import com.hunesion.drool_v2.model.AccessRequest;
import com.hunesion.drool_v2.model.AccessResult;

rule "Manager Reports Access"
    salience 50
    when
        $request : AccessRequest(
            endpointMatches("/api/reports(/.*)?"),
            httpMethod == "GET",
            (hasRole("MANAGER"))
        )
        $result : AccessResult(evaluated == false)
    then
        $result.allow("Manager Reports Access");
        System.out.println("✓ Access ALLOWED by policy: Manager Reports Access");
end
',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (policy_name) DO NOTHING;

-- Policy 3: User Profile Access
INSERT INTO access_policies (
    policy_name, description, endpoint, http_method, allowed_roles, 
    conditions, effect, priority, enabled, generated_drl, created_at, updated_at
) VALUES (
    'User Profile Access',
    'Users can access profile endpoints',
    '/api/profile/**',
    '*',
    '["USER","MANAGER","VIEWER"]',
    NULL,
    'ALLOW',
    30,
    true,
    'package rules.dynamic;

import com.hunesion.drool_v2.model.AccessRequest;
import com.hunesion.drool_v2.model.AccessResult;

rule "User Profile Access"
    salience 30
    when
        $request : AccessRequest(
            endpointMatches("/api/profile(/.*)?"),
            (hasRole("USER") || hasRole("MANAGER") || hasRole("VIEWER"))
        )
        $result : AccessResult(evaluated == false)
    then
        $result.allow("User Profile Access");
        System.out.println("✓ Access ALLOWED by policy: User Profile Access");
end
',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (policy_name) DO NOTHING;

-- Policy 4: Sales Department Data Access
INSERT INTO access_policies (
    policy_name, description, endpoint, http_method, allowed_roles, 
    conditions, effect, priority, enabled, generated_drl, created_at, updated_at
) VALUES (
    'Sales Department Data Access',
    'Sales department users can access sales data',
    '/api/sales/**',
    'GET',
    '["USER","MANAGER"]',
    '{"department":{"operator":"equals","value":"SALES"}}',
    'ALLOW',
    40,
    true,
    'package rules.dynamic;

import com.hunesion.drool_v2.model.AccessRequest;
import com.hunesion.drool_v2.model.AccessResult;

rule "Sales Department Data Access"
    salience 40
    when
        $request : AccessRequest(
            endpointMatches("/api/sales(/.*)?"),
            httpMethod == "GET",
            (hasRole("USER") || hasRole("MANAGER")),
            department == "SALES"
        )
        $result : AccessResult(evaluated == false)
    then
        $result.allow("Sales Department Data Access");
        System.out.println("✓ Access ALLOWED by policy: Sales Department Data Access");
end
',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (policy_name) DO NOTHING;

-- Policy 5: Management Level Access
INSERT INTO access_policies (
    policy_name, description, endpoint, http_method, allowed_roles, 
    conditions, effect, priority, enabled, generated_drl, created_at, updated_at
) VALUES (
    'Management Level Access',
    'Users with level 5+ can access management',
    '/api/management/**',
    '*',
    '["USER","MANAGER"]',
    '{"userLevel":{"operator":"greaterThanOrEqual","value":"5"}}',
    'ALLOW',
    35,
    true,
    'package rules.dynamic;

import com.hunesion.drool_v2.model.AccessRequest;
import com.hunesion.drool_v2.model.AccessResult;

rule "Management Level Access"
    salience 35
    when
        $request : AccessRequest(
            endpointMatches("/api/management(/.*)?"),
            (hasRole("USER") || hasRole("MANAGER")),
            userLevel != null && userLevel >= 5
        )
        $result : AccessResult(evaluated == false)
    then
        $result.allow("Management Level Access");
        System.out.println("✓ Access ALLOWED by policy: Management Level Access");
end
',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (policy_name) DO NOTHING;

-- Policy 6: Viewer Public Data Access
INSERT INTO access_policies (
    policy_name, description, endpoint, http_method, allowed_roles, 
    conditions, effect, priority, enabled, generated_drl, created_at, updated_at
) VALUES (
    'Viewer Public Data Access',
    'Viewers can access public data endpoints',
    '/api/public/**',
    'GET',
    '["VIEWER","USER","MANAGER"]',
    NULL,
    'ALLOW',
    20,
    true,
    'package rules.dynamic;

import com.hunesion.drool_v2.model.AccessRequest;
import com.hunesion.drool_v2.model.AccessResult;

rule "Viewer Public Data Access"
    salience 20
    when
        $request : AccessRequest(
            endpointMatches("/api/public(/.*)?"),
            httpMethod == "GET",
            (hasRole("VIEWER") || hasRole("USER") || hasRole("MANAGER"))
        )
        $result : AccessResult(evaluated == false)
    then
        $result.allow("Viewer Public Data Access");
        System.out.println("✓ Access ALLOWED by policy: Viewer Public Data Access");
end
',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (policy_name) DO NOTHING;

-- ============================================
-- 5. INSERT EQUIPMENT
-- ============================================
-- Note: Equipment represents devices/servers that users can access
-- Device types: LINUX_SERVER, DATABASE, WINDOWS_SERVER, etc.

-- Equipment 1: Linux Server with SSH



-- ============================================
-- Verification Queries (Optional)
-- ============================================
-- Uncomment to verify the data was inserted correctly:

-- SELECT COUNT(*) as role_count FROM roles;
-- SELECT COUNT(*) as user_count FROM users;
-- SELECT COUNT(*) as user_role_count FROM user_roles;
-- SELECT COUNT(*) as policy_count FROM access_policies;
-- SELECT COUNT(*) as equipment_count FROM equipment;
-- SELECT COUNT(*) as user_equipment_count FROM user_equipment;

-- SELECT r.name, COUNT(ur.user_id) as user_count
-- FROM roles r
-- LEFT JOIN user_roles ur ON r.id = ur.role_id
-- GROUP BY r.id, r.name
-- ORDER BY r.name;

-- SELECT e.device_name, e.device_type, COUNT(ue.user_id) as user_count
-- FROM equipment e
-- LEFT JOIN user_equipment ue ON e.id = ue.equipment_id
-- WHERE e.is_deleted = false
-- GROUP BY e.id, e.device_name, e.device_type
-- ORDER BY e.device_name;

