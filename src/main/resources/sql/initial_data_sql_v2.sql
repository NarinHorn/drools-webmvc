-- ============================================
-- Initial Data SQL Script V2 - JSONB Implementation
-- ============================================
-- 
-- This script inserts comprehensive test data for:
-- - Roles, Users, User Groups
-- - Access Policies (with group assignments)
-- - Equipment Policies (with JSONB policy_config)
-- - Equipment, Command Lists
-- - Policy Assignments (user, group, equipment, role)
--
-- Key Features:
-- - EquipmentPolicy uses JSONB policy_config (no normalized tables)
-- - AccessPolicy supports both roles and groups
-- - Full test coverage for API endpoints
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
INSERT INTO users (username, password, email, department, level, active) VALUES
('admin', 'admin123', 'admin@example.com', 'IT', 10, true),
('manager', 'manager123', 'manager@example.com', 'SALES', 5, true),
('john', 'john123', 'john@example.com', 'SALES', 3, true),
('jane', 'jane123', 'jane@example.com', 'HR', 3, true),
('viewer', 'viewer123', 'viewer@example.com', 'GUEST', 1, true),
('ituser', 'ituser123', 'ituser@example.com', 'IT', 4, true),
('salesuser', 'salesuser123', 'salesuser@example.com', 'SALES', 2, true)
ON CONFLICT (username) DO NOTHING;

-- ============================================
-- 3. INSERT USER_ROLES (Many-to-Many relationships)
-- ============================================
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE (u.username = 'admin' AND r.name = 'ADMIN')
   OR (u.username = 'manager' AND r.name = 'MANAGER')
   OR (u.username = 'john' AND r.name = 'USER')
   OR (u.username = 'jane' AND r.name = 'USER')
   OR (u.username = 'viewer' AND r.name = 'VIEWER')
   OR (u.username = 'ituser' AND r.name = 'USER')
   OR (u.username = 'salesuser' AND r.name = 'USER')
ON CONFLICT DO NOTHING;

-- ============================================
-- 4. INSERT USER GROUPS
-- ============================================
INSERT INTO user_groups (group_name, group_description, created_at, updated_at) VALUES
('IT Team', 'IT department team members', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Sales Team', 'Sales department team members', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Managers', 'Management group', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Developers', 'Development team', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (group_name) DO NOTHING;

-- ============================================
-- 5. INSERT USER_GROUP_MEMBERS (Many-to-Many relationships)
-- ============================================
INSERT INTO user_group_members (user_id, group_id)
SELECT u.id, g.id
FROM users u, user_groups g
WHERE (u.username = 'admin' AND g.group_name = 'IT Team')
   OR (u.username = 'ituser' AND g.group_name = 'IT Team')
   OR (u.username = 'manager' AND g.group_name = 'Managers')
   OR (u.username = 'manager' AND g.group_name = 'Sales Team')
   OR (u.username = 'john' AND g.group_name = 'Sales Team')
   OR (u.username = 'salesuser' AND g.group_name = 'Sales Team')
   OR (u.username = 'jane' AND g.group_name = 'Developers')
ON CONFLICT DO NOTHING;

-- ============================================
-- 6. INSERT EQUIPMENT
-- ============================================
INSERT INTO equipment (device_name, host_name, ip_address, protocol, port, username, password, device_type, is_deleted, created_at, updated_at) VALUES
('Linux Production Server', 'prod-linux-01', '192.168.1.10', 'SSH', 22, 'root', 'password123', 'LINUX_SERVER', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Windows Server 2019', 'win-server-01', '192.168.1.20', 'RDP', 3389, 'administrator', 'password123', 'WINDOWS_SERVER', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PostgreSQL Database', 'db-postgres-01', '192.168.1.30', 'POSTGRESQL', 5432, 'postgres', 'password123', 'DATABASE', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MySQL Database', 'db-mysql-01', '192.168.1.31', 'MYSQL', 3306, 'root', 'password123', 'DATABASE', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Linux Dev Server', 'dev-linux-01', '192.168.1.40', 'SSH', 22, 'devuser', 'password123', 'LINUX_SERVER', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- ============================================
-- 7. INSERT COMMAND LISTS (for Equipment Policies)
-- ============================================
INSERT INTO command_lists (list_name, list_type, protocol_type, created_at) VALUES
('Dangerous Commands Blacklist', 'blacklist', 'TELNET_SSH', CURRENT_TIMESTAMP),
('Safe Commands Whitelist', 'whitelist', 'TELNET_SSH', CURRENT_TIMESTAMP),
('Database Admin Commands', 'whitelist', 'DB', CURRENT_TIMESTAMP),
('Read-Only Commands', 'whitelist', 'DB', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Insert command list items
INSERT INTO command_list_items (command_list_id, command_text, created_at)
SELECT cl.id, cmd, CURRENT_TIMESTAMP
FROM command_lists cl,
     (VALUES 
         ('Dangerous Commands Blacklist', 'rm -rf /'),
         ('Dangerous Commands Blacklist', 'dd if=/dev/zero'),
         ('Dangerous Commands Blacklist', 'mkfs'),
         ('Safe Commands Whitelist', 'ls'),
         ('Safe Commands Whitelist', 'cd'),
         ('Safe Commands Whitelist', 'pwd'),
         ('Safe Commands Whitelist', 'cat'),
         ('Database Admin Commands', 'SELECT'),
         ('Database Admin Commands', 'INSERT'),
         ('Database Admin Commands', 'UPDATE'),
         ('Database Admin Commands', 'DELETE'),
         ('Read-Only Commands', 'SELECT'),
         ('Read-Only Commands', 'SHOW')
     ) AS commands(list_name, cmd)
WHERE cl.list_name = commands.list_name
ON CONFLICT DO NOTHING;

-- ============================================
-- 8. INSERT ACCESS POLICIES (with group assignments)
-- ============================================

-- Policy 1: Admin Full Access (Role-based)
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

-- Policy 2: Managers or IT Team Access (Role + Group)
INSERT INTO access_policies (
    policy_name, description, endpoint, http_method, allowed_roles, 
    conditions, effect, priority, enabled, generated_drl, created_at, updated_at
) VALUES (
    'Managers or IT Team Access',
    'Access for managers (role) or IT team (group)',
    '/api/admin/**',
    '*',
    '["MANAGER"]',
    NULL,
    'ALLOW',
    80,
    true,
    'package rules.dynamic;

import com.hunesion.drool_v2.model.AccessRequest;
import com.hunesion.drool_v2.model.AccessResult;

rule "Managers or IT Team Access"
    salience 80
    when
        $request : AccessRequest(
            endpointMatches("/api/admin(/.*)?"),
            (hasRole("MANAGER") || hasGroup("IT Team"))
        )
        $result : AccessResult(evaluated == false)
    then
        $result.allow("Managers or IT Team Access");
        System.out.println("✓ Access ALLOWED by policy: Managers or IT Team Access");
end
',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (policy_name) DO NOTHING;

-- Assign IT Team group to "Managers or IT Team Access" policy
INSERT INTO access_policy_group_assignments (access_policy_id, group_id)
SELECT ap.id, g.id
FROM access_policies ap, user_groups g
WHERE ap.policy_name = 'Managers or IT Team Access' AND g.group_name = 'IT Team'
ON CONFLICT DO NOTHING;

-- Policy 3: Sales Team Reports Access (Group-based)
INSERT INTO access_policies (
    policy_name, description, endpoint, http_method, allowed_roles, 
    conditions, effect, priority, enabled, generated_drl, created_at, updated_at
) VALUES (
    'Sales Team Reports Access',
    'Sales team can access reports',
    '/api/reports/sales/**',
    'GET',
    NULL,
    NULL,
    'ALLOW',
    50,
    true,
    'package rules.dynamic;

import com.hunesion.drool_v2.model.AccessRequest;
import com.hunesion.drool_v2.model.AccessResult;

rule "Sales Team Reports Access"
    salience 50
    when
        $request : AccessRequest(
            endpointMatches("/api/reports/sales(/.*)?"),
            httpMethod == "GET",
            (hasGroup("Sales Team"))
        )
        $result : AccessResult(evaluated == false)
    then
        $result.allow("Sales Team Reports Access");
        System.out.println("✓ Access ALLOWED by policy: Sales Team Reports Access");
end
',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (policy_name) DO NOTHING;

-- Assign Sales Team group to "Sales Team Reports Access" policy
INSERT INTO access_policy_group_assignments (access_policy_id, group_id)
SELECT ap.id, g.id
FROM access_policies ap, user_groups g
WHERE ap.policy_name = 'Sales Team Reports Access' AND g.group_name = 'Sales Team'
ON CONFLICT DO NOTHING;

-- Policy 4: User Profile Access (Role-based)
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

-- ============================================
-- 9. INSERT EQUIPMENT POLICIES (with JSONB policy_config)
-- ============================================

-- Policy 1: IT Team SSH Access Policy (with full JSONB config)
INSERT INTO equipment_policies (
    policy_name, description, policy_classification, policy_application,
    enabled, priority, version_no, policy_config, generated_rule_drl,
    created_at, updated_at
) VALUES (
    'IT Team SSH Access Policy',
    'IT team can access Linux servers via SSH during business hours',
    'common',
    'apply',
    true,
    100,
    1,
    '{
        "commonSettings": {
            "servicePort": 22,
            "idleTimeMinutes": 30,
            "timeoutMinutes": 60,
            "blockingPolicyType": "none",
            "sessionBlockingCount": 0,
            "maxTelnetSessions": 0,
            "telnetBorderless": false,
            "maxSshSessions": 5,
            "sshBorderless": false,
            "maxRdpSessions": 0,
            "rdpBorderless": false,
            "allowedProtocols": ["SSH"],
            "allowedDbms": []
        },
        "allowedTime": {
            "startDate": null,
            "endDate": null,
            "borderless": false,
            "timeZone": "Asia/Bangkok",
            "timeSlots": [
                {"dayOfWeek": 1, "hourStart": 9, "hourEnd": 18},
                {"dayOfWeek": 2, "hourStart": 9, "hourEnd": 18},
                {"dayOfWeek": 3, "hourStart": 9, "hourEnd": 18},
                {"dayOfWeek": 4, "hourStart": 9, "hourEnd": 18},
                {"dayOfWeek": 5, "hourStart": 9, "hourEnd": 18}
            ]
        },
        "loginControl": {
            "ipFilteringType": "whitelist",
            "accountLockEnabled": true,
            "maxFailureAttempts": 3,
            "lockoutDurationMinutes": 30,
            "twoFactorType": "none",
            "allowedIps": ["192.168.1.0/24", "10.0.0.0/8"]
        },
        "commandSettings": [
            {
                "protocolType": "TELNET_SSH",
                "controlMethod": "blacklist",
                "controlTarget": "command",
                "commandListIds": [1]
            }
        ],
        "customConditions": {
            "deviceType": {
                "operator": "equals",
                "value": "LINUX_SERVER"
            }
        },
        "customMetadata": {
            "createdBy": "admin",
            "department": "IT",
            "notes": "Standard IT team access policy"
        }
    }',
    'package rules.equipment;

import com.hunesion.drool_v2.model.EquipmentAccessRequest;
import com.hunesion.drool_v2.model.EquipmentAccessResult;

rule "IT Team SSH Access Policy"
    salience 100
    when
        $request : EquipmentAccessRequest(
            isAssignedToPolicy(1),
            (hasProtocol("SSH")),
            isWithinAllowedTime(),
            isIpAllowed(clientIp),
            !isCommandBlocked(command),
            attributes.get("deviceType") != null && attributes.get("deviceType").toString().equals("LINUX_SERVER")
        )
        $result : EquipmentAccessResult(evaluated == false)
    then
        modify($result) {
            setAllowed(true),
            setEvaluated(true),
            setMatchedPolicyName("IT Team SSH Access Policy")
        }
        System.out.println("✓ Equipment access ALLOWED by policy: IT Team SSH Access Policy");
end
',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (policy_name) DO NOTHING;

-- Assign IT Team group to "IT Team SSH Access Policy"
INSERT INTO policy_group_assignments (policy_id, group_id)
SELECT ep.id, g.id
FROM equipment_policies ep, user_groups g
WHERE ep.policy_name = 'IT Team SSH Access Policy' AND g.group_name = 'IT Team'
ON CONFLICT DO NOTHING;

-- Policy 2: Sales Team Database Read-Only Access
INSERT INTO equipment_policies (
    policy_name, description, policy_classification, policy_application,
    enabled, priority, version_no, policy_config, generated_rule_drl,
    created_at, updated_at
) VALUES (
    'Sales Team Database Read-Only',
    'Sales team can access databases with read-only commands',
    'common',
    'apply',
    true,
    80,
    1,
    '{
        "commonSettings": {
            "servicePort": 5432,
            "idleTimeMinutes": 15,
            "timeoutMinutes": 30,
            "blockingPolicyType": "none",
            "sessionBlockingCount": 0,
            "maxTelnetSessions": 0,
            "telnetBorderless": false,
            "maxSshSessions": 0,
            "sshBorderless": false,
            "maxRdpSessions": 0,
            "rdpBorderless": false,
            "allowedProtocols": [],
            "allowedDbms": ["POSTGRESQL", "MYSQL"]
        },
        "allowedTime": {
            "startDate": null,
            "endDate": null,
            "borderless": true,
            "timeZone": "Asia/Bangkok",
            "timeSlots": []
        },
        "loginControl": {
            "ipFilteringType": "no_restrictions",
            "accountLockEnabled": false,
            "maxFailureAttempts": 0,
            "lockoutDurationMinutes": 0,
            "twoFactorType": "none",
            "allowedIps": []
        },
        "commandSettings": [
            {
                "protocolType": "DB",
                "controlMethod": "whitelist",
                "controlTarget": "command",
                "commandListIds": [4]
            }
        ],
        "customConditions": {},
        "customMetadata": {
            "createdBy": "admin",
            "department": "SALES",
            "notes": "Read-only database access for sales team"
        }
    }',
    'package rules.equipment;

import com.hunesion.drool_v2.model.EquipmentAccessRequest;
import com.hunesion.drool_v2.model.EquipmentAccessResult;

rule "Sales Team Database Read-Only"
    salience 80
    when
        $request : EquipmentAccessRequest(
            isAssignedToPolicy(2),
            (hasDbmsType("POSTGRESQL") || hasDbmsType("MYSQL"))
        )
        $result : EquipmentAccessResult(evaluated == false)
    then
        modify($result) {
            setAllowed(true),
            setEvaluated(true),
            setMatchedPolicyName("Sales Team Database Read-Only")
        }
        System.out.println("✓ Equipment access ALLOWED by policy: Sales Team Database Read-Only");
end
',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (policy_name) DO NOTHING;

-- Assign Sales Team group to "Sales Team Database Read-Only" policy
INSERT INTO policy_group_assignments (policy_id, group_id)
SELECT ep.id, g.id
FROM equipment_policies ep, user_groups g
WHERE ep.policy_name = 'Sales Team Database Read-Only' AND g.group_name = 'Sales Team'
ON CONFLICT DO NOTHING;

-- Assign PostgreSQL and MySQL equipment to this policy
INSERT INTO policy_equipment_assignments (policy_id, equipment_id)
SELECT ep.id, e.id
FROM equipment_policies ep, equipment e
WHERE ep.policy_name = 'Sales Team Database Read-Only' 
  AND e.device_name IN ('PostgreSQL Database', 'MySQL Database')
ON CONFLICT DO NOTHING;

-- Policy 3: Manager Full Access Policy (Role-based)
INSERT INTO equipment_policies (
    policy_name, description, policy_classification, policy_application,
    enabled, priority, version_no, policy_config, generated_rule_drl,
    created_at, updated_at
) VALUES (
    'Manager Full Access Policy',
    'Managers have full access to all equipment',
    'common',
    'apply',
    true,
    90,
    1,
    '{
        "commonSettings": {
            "servicePort": null,
            "idleTimeMinutes": 60,
            "timeoutMinutes": 120,
            "blockingPolicyType": "none",
            "sessionBlockingCount": 0,
            "maxTelnetSessions": 10,
            "telnetBorderless": true,
            "maxSshSessions": 10,
            "sshBorderless": true,
            "maxRdpSessions": 10,
            "rdpBorderless": true,
            "allowedProtocols": ["SSH", "RDP", "TELNET"],
            "allowedDbms": ["POSTGRESQL", "MYSQL", "ORACLE", "SQLSERVER"]
        },
        "allowedTime": {
            "startDate": null,
            "endDate": null,
            "borderless": true,
            "timeZone": "Asia/Bangkok",
            "timeSlots": []
        },
        "loginControl": {
            "ipFilteringType": "no_restrictions",
            "accountLockEnabled": false,
            "maxFailureAttempts": 0,
            "lockoutDurationMinutes": 0,
            "twoFactorType": "none",
            "allowedIps": []
        },
        "commandSettings": [],
        "customConditions": {},
        "customMetadata": {
            "createdBy": "admin",
            "department": "ALL",
            "notes": "Full access for managers"
        }
    }',
    'package rules.equipment;

import com.hunesion.drool_v2.model.EquipmentAccessRequest;
import com.hunesion.drool_v2.model.EquipmentAccessResult;

rule "Manager Full Access Policy"
    salience 90
    when
        $request : EquipmentAccessRequest(
            isAssignedToPolicy(3),
            (hasProtocol("SSH") || hasProtocol("RDP") || hasProtocol("TELNET") || hasDbmsType("POSTGRESQL") || hasDbmsType("MYSQL") || hasDbmsType("ORACLE") || hasDbmsType("SQLSERVER"))
        )
        $result : EquipmentAccessResult(evaluated == false)
    then
        modify($result) {
            setAllowed(true),
            setEvaluated(true),
            setMatchedPolicyName("Manager Full Access Policy")
        }
        System.out.println("✓ Equipment access ALLOWED by policy: Manager Full Access Policy");
end
',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (policy_name) DO NOTHING;

-- Assign MANAGER role to "Manager Full Access Policy"
INSERT INTO policy_role_assignments (policy_id, role_id)
SELECT ep.id, r.id
FROM equipment_policies ep, roles r
WHERE ep.policy_name = 'Manager Full Access Policy' AND r.name = 'MANAGER'
ON CONFLICT DO NOTHING;

-- ============================================
-- 10. VERIFICATION QUERIES
-- ============================================
-- Uncomment to verify the data was inserted correctly:

-- SELECT COUNT(*) as role_count FROM roles;
-- SELECT COUNT(*) as user_count FROM users;
-- SELECT COUNT(*) as user_role_count FROM user_roles;
-- SELECT COUNT(*) as group_count FROM user_groups;
-- SELECT COUNT(*) as user_group_member_count FROM user_group_members;
-- SELECT COUNT(*) as access_policy_count FROM access_policies;
-- SELECT COUNT(*) as equipment_policy_count FROM equipment_policies;
-- SELECT COUNT(*) as equipment_count FROM equipment;
-- SELECT COUNT(*) as command_list_count FROM command_lists;

-- Verify Access Policies with Groups
-- SELECT ap.policy_name, ap.allowed_roles, g.group_name
-- FROM access_policies ap
-- LEFT JOIN access_policy_group_assignments apga ON ap.id = apga.access_policy_id
-- LEFT JOIN user_groups g ON apga.group_id = g.id
-- ORDER BY ap.policy_name;

-- Verify Equipment Policies with JSONB config
-- SELECT policy_name, policy_config::jsonb->'commonSettings'->>'allowedProtocols' as protocols
-- FROM equipment_policies
-- WHERE policy_config IS NOT NULL;

-- Verify Policy Assignments
-- SELECT ep.policy_name, 'GROUP' as assignment_type, g.group_name as assigned_to
-- FROM equipment_policies ep
-- JOIN policy_group_assignments pga ON ep.id = pga.policy_id
-- JOIN user_groups g ON pga.group_id = g.id
-- UNION ALL
-- SELECT ep.policy_name, 'ROLE' as assignment_type, r.name as assigned_to
-- FROM equipment_policies ep
-- JOIN policy_role_assignments pra ON ep.id = pra.policy_id
-- JOIN roles r ON pra.role_id = r.id
-- UNION ALL
-- SELECT ep.policy_name, 'EQUIPMENT' as assignment_type, e.device_name as assigned_to
-- FROM equipment_policies ep
-- JOIN policy_equipment_assignments pea ON ep.id = pea.policy_id
-- JOIN equipment e ON pea.equipment_id = e.id
-- ORDER BY policy_name, assignment_type;
