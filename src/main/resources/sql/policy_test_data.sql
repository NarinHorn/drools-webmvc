-- ============================================
-- POLICY MANAGEMENT TEST DATA
-- Based on requirements from diagram
-- Priority: WORK_GROUP(300) > USER(200) > USER_TYPE(100) > GLOBAL(0)
-- ============================================

-- ============================================
-- 1. ROLES
-- ============================================
INSERT INTO roles (name, description) VALUES
    ('ADMIN', 'System Administrator'),
    ('USER', 'Regular User'),
    ('USER_VIEW_ONLY', 'Read-only User')
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- 2. USER TYPES (from requirement)
-- ============================================
INSERT INTO user_types (type_code, type_name, description, is_active, created_at, updated_at) VALUES
    ('SUPER_ADMIN', 'Super Admin User', 'Super administrator with highest privileges', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MIDDLE_MANAGER', 'Middle Manager', 'Department manager level', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('REGULAR_USER', 'Regular User', 'Standard user access', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('OCCASIONAL_USER', 'Occasional User', 'Limited/temporary access user', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

-- ============================================
-- 3. ACCOUNT TYPES (from requirement)
-- ============================================
INSERT INTO account_types (type_code, type_name, description, is_active, created_at, updated_at) VALUES
    ('COLLECTION', 'Collection Account', 'Shared collection account', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PRIVILEGED', 'Privileged Account', 'High-privilege root/admin account', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PERSONAL', 'Personal Account', 'Individual user account', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SOLUTION', 'Solution Account', 'Application/service account', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PUBLIC', 'Public Account', 'Public shared account', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

-- ============================================
-- 4. USERS (from requirement)
-- ============================================
-- admin: system admin (for API access control)
-- yhkim: admin/super admin
-- yang: admin/super admin
-- huy: user/middle manager
-- sokhim: user/regular user
-- lee: occasional user

-- System admin user for API access
INSERT INTO users (username, password, email, department, level, active, user_type_id)
SELECT 'admin', 'admin123', 'admin@example.com', 'IT', 10, true, ut.id
FROM user_types ut WHERE ut.type_code = 'SUPER_ADMIN'
ON CONFLICT (username) DO UPDATE SET user_type_id = EXCLUDED.user_type_id;

INSERT INTO users (username, password, email, department, level, active, user_type_id)
SELECT 'yhkim', 'yhkim123', 'yhkim@example.com', 'IT', 10, true, ut.id
FROM user_types ut WHERE ut.type_code = 'SUPER_ADMIN'
ON CONFLICT (username) DO UPDATE SET user_type_id = EXCLUDED.user_type_id;

INSERT INTO users (username, password, email, department, level, active, user_type_id)
SELECT 'yang', 'yang123', 'yang@example.com', 'IT', 10, true, ut.id
FROM user_types ut WHERE ut.type_code = 'SUPER_ADMIN'
ON CONFLICT (username) DO UPDATE SET user_type_id = EXCLUDED.user_type_id;

INSERT INTO users (username, password, email, department, level, active, user_type_id)
SELECT 'huy', 'huy123', 'huy@example.com', 'SALES', 5, true, ut.id
FROM user_types ut WHERE ut.type_code = 'MIDDLE_MANAGER'
ON CONFLICT (username) DO UPDATE SET user_type_id = EXCLUDED.user_type_id;

INSERT INTO users (username, password, email, department, level, active, user_type_id)
SELECT 'sokhim', 'sokhim123', 'sokhim@example.com', 'DEV', 3, true, ut.id
FROM user_types ut WHERE ut.type_code = 'REGULAR_USER'
ON CONFLICT (username) DO UPDATE SET user_type_id = EXCLUDED.user_type_id;

INSERT INTO users (username, password, email, department, level, active, user_type_id)
SELECT 'lee', 'lee123', 'lee@example.com', 'GUEST', 1, true, ut.id
FROM user_types ut WHERE ut.type_code = 'OCCASIONAL_USER'
ON CONFLICT (username) DO UPDATE SET user_type_id = EXCLUDED.user_type_id;

-- ============================================
-- 5. USER ROLES
-- ============================================
-- admin, yhkim, yang -> ADMIN
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username IN ('admin', 'yhkim', 'yang') AND r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- huy, sokhim, lee -> USER
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username IN ('huy', 'sokhim', 'lee') AND r.name = 'USER'
ON CONFLICT DO NOTHING;

-- ============================================
-- 6. USER GROUPS
-- ============================================
INSERT INTO user_groups (group_name, group_description, created_at, updated_at) VALUES
    ('Web Development Team', 'Web project team members', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (group_name) DO NOTHING;

-- Add yhkim, lee, sokhim to Web Development Team (from requirement: work group: web)
INSERT INTO user_group_members (group_id, user_id)
SELECT ug.id, u.id
FROM user_groups ug, users u
WHERE ug.group_name = 'Web Development Team'
  AND u.username IN ('yhkim', 'lee', 'sokhim')
ON CONFLICT DO NOTHING;

-- ============================================
-- 7. EQUIPMENT (devices from requirement)
-- ============================================
INSERT INTO equipment (device_name, device_type, host_name, ip_address, protocol, port, is_deleted, created_at, updated_at) VALUES
    ('Web Server 1', 'GENERAL', 'web-server-1', '192.168.10.1', 'SSH', 22, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Web Server 2', 'GENERAL', 'web-server-2', '192.168.10.2', 'SSH', 22, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Oracle DB Server', 'ORACLE', 'oracle-db', '192.168.10.100', 'SSH', 22, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Public Server', 'PUBLIC', 'public-server', '192.168.10.200', 'SSH', 22, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- ============================================
-- 8. ACCOUNTS (on equipment)
-- ============================================
-- web_root (privileged) and web_user (personal) on Web Server 1
INSERT INTO accounts (account_name, username, equipment_id, account_type_id, is_active, created_at, updated_at)
SELECT 'web_root', 'root', e.id, at.id, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM equipment e, account_types at
WHERE e.device_name = 'Web Server 1' AND at.type_code = 'PRIVILEGED'
ON CONFLICT DO NOTHING;

INSERT INTO accounts (account_name, username, equipment_id, account_type_id, is_active, created_at, updated_at)
SELECT 'web_user', 'webuser', e.id, at.id, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM equipment e, account_types at
WHERE e.device_name = 'Web Server 1' AND at.type_code = 'PERSONAL'
ON CONFLICT DO NOTHING;

-- public_account on Public Server
INSERT INTO accounts (account_name, username, equipment_id, account_type_id, is_active, created_at, updated_at)
SELECT 'public_account', 'public', e.id, at.id, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM equipment e, account_types at
WHERE e.device_name = 'Public Server' AND at.type_code = 'PUBLIC'
ON CONFLICT DO NOTHING;

-- ============================================
-- 9. POLICY TYPES
-- ============================================
INSERT INTO policy_types (type_code, type_name, description, is_active, created_at, updated_at) VALUES
    ('loginMethods', 'Login Methods', 'Client login methods (credential, MFA settings)', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('sessionTimeout', 'Session Timeout', 'Session timeout configuration (seconds)', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('concurrentSessions', 'Concurrent Sessions', 'Maximum concurrent sessions allowed', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bannedCommands', 'Banned Commands', 'Commands that are blocked', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('serverLoginMethods', 'Login Methods for Server', 'Server-side login configuration', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

-- ============================================
-- 10. EQUIPMENT POLICIES
-- ============================================

-- 10.1 GLOBAL Default Login Methods Policy (for all users) - Priority 0
-- Default: id/password + MFA(sms, email, otp) - email highlighted as default
INSERT INTO equipment_policies (
    policy_name, description, policy_classification, policy_application,
    policy_type_id, policy_config, enabled, priority, created_at, updated_at
) SELECT
    'Global Default Login Methods',
    'Default login policy for all users: id/password + MFA (email default)',
    'common', 'apply', pt.id,
    '{"loginMethods": {"credential": true, "mfa": {"enabled": true, "methods": ["sms", "email", "otp"], "default": "email"}, "autoLogin": true}}',
    true, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM policy_types pt WHERE pt.type_code = 'loginMethods'
ON CONFLICT (policy_name) DO NOTHING;

-- 10.2 Super Admin Login Methods (MFA: otp required) - Priority 100 (USER_TYPE level)
INSERT INTO equipment_policies (
    policy_name, description, policy_classification, policy_application,
    policy_type_id, policy_config, enabled, priority, created_at, updated_at
) SELECT
    'Super Admin Login Methods',
    'Login policy for super admin: id/password + MFA (otp required)',
    'common', 'apply', pt.id,
    '{"loginMethods": {"credential": true, "mfa": {"enabled": true, "methods": ["sms", "email", "otp"], "default": "otp", "required": "otp"}, "autoLogin": true}}',
    true, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM policy_types pt WHERE pt.type_code = 'loginMethods'
ON CONFLICT (policy_name) DO NOTHING;

-- 10.3 Occasional User Login Methods (MFA: sms required) - Priority 100 (USER_TYPE level)
INSERT INTO equipment_policies (
    policy_name, description, policy_classification, policy_application,
    policy_type_id, policy_config, enabled, priority, created_at, updated_at
) SELECT
    'Occasional User Login Methods',
    'Login policy for occasional users: id/password + MFA (sms required)',
    'common', 'apply', pt.id,
    '{"loginMethods": {"credential": true, "mfa": {"enabled": true, "methods": ["sms", "email", "otp"], "default": "sms", "required": "sms"}, "autoLogin": true}}',
    true, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM policy_types pt WHERE pt.type_code = 'loginMethods'
ON CONFLICT (policy_name) DO NOTHING;

-- 10.4 User Lee - Credential Only (no MFA) - Priority 200 (USER level)
INSERT INTO equipment_policies (
    policy_name, description, policy_classification, policy_application,
    policy_type_id, policy_config, enabled, priority, created_at, updated_at
) SELECT
    'User Lee Login Methods',
    'Login policy for user Lee: id/password only, no MFA',
    'common', 'apply', pt.id,
    '{"loginMethods": {"credential": true, "mfa": {"enabled": false}, "autoLogin": false}}',
    true, 200, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM policy_types pt WHERE pt.type_code = 'loginMethods'
ON CONFLICT (policy_name) DO NOTHING;

-- 10.5 User yhkim - Credential Only (no MFA) - Priority 200 (USER level)
INSERT INTO equipment_policies (
    policy_name, description, policy_classification, policy_application,
    policy_type_id, policy_config, enabled, priority, created_at, updated_at
) SELECT
    'User yhkim Login Methods',
    'Login policy for user yhkim: id/password only, no MFA',
    'common', 'apply', pt.id,
    '{"loginMethods": {"credential": true, "mfa": {"enabled": false}, "autoLogin": false}}',
    true, 200, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM policy_types pt WHERE pt.type_code = 'loginMethods'
ON CONFLICT (policy_name) DO NOTHING;

-- 10.6 Global Default Session Timeout (600 secs) - Priority 0
INSERT INTO equipment_policies (
    policy_name, description, policy_classification, policy_application,
    policy_type_id, policy_config, enabled, priority, created_at, updated_at
) SELECT
    'Global Default Session Timeout',
    'Default session timeout: 600 seconds',
    'common', 'apply', pt.id,
    '{"sessionTimeout": {"timeoutSeconds": 600}}',
    true, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM policy_types pt WHERE pt.type_code = 'sessionTimeout'
ON CONFLICT (policy_name) DO NOTHING;

-- 10.7 Privileged Account Session Timeout (3600 secs) - Priority 100
INSERT INTO equipment_policies (
    policy_name, description, policy_classification, policy_application,
    policy_type_id, policy_config, enabled, priority, created_at, updated_at
) SELECT
    'Privileged Account Session Timeout',
    'Session timeout for privileged accounts: 3600 seconds',
    'common', 'apply', pt.id,
    '{"sessionTimeout": {"timeoutSeconds": 3600}}',
    true, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM policy_types pt WHERE pt.type_code = 'sessionTimeout'
ON CONFLICT (policy_name) DO NOTHING;

-- 10.8 Public Account Session Timeout (300 secs) - Priority 100
INSERT INTO equipment_policies (
    policy_name, description, policy_classification, policy_application,
    policy_type_id, policy_config, enabled, priority, created_at, updated_at
) SELECT
    'Public Account Session Timeout',
    'Session timeout for public accounts: 300 seconds',
    'common', 'apply', pt.id,
    '{"sessionTimeout": {"timeoutSeconds": 300}}',
    true, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM policy_types pt WHERE pt.type_code = 'sessionTimeout'
ON CONFLICT (policy_name) DO NOTHING;

-- 10.9 Oracle Device Session Timeout (1200 secs) - Priority 100
INSERT INTO equipment_policies (
    policy_name, description, policy_classification, policy_application,
    policy_type_id, policy_config, enabled, priority, created_at, updated_at
) SELECT
    'Oracle Device Session Timeout',
    'Session timeout for Oracle devices: 1200 seconds',
    'common', 'apply', pt.id,
    '{"sessionTimeout": {"timeoutSeconds": 1200}}',
    true, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM policy_types pt WHERE pt.type_code = 'sessionTimeout'
ON CONFLICT (policy_name) DO NOTHING;

-- 10.10 Work Group Concurrent Sessions (30) - Priority 300 (WORK_GROUP level - HIGHEST)
INSERT INTO equipment_policies (
    policy_name, description, policy_classification, policy_application,
    policy_type_id, policy_config, enabled, priority, created_at, updated_at
) SELECT
    'Web Team Concurrent Sessions',
    'Concurrent sessions limit for web team: 30',
    'common', 'apply', pt.id,
    '{"concurrentSessions": {"maxSessions": 30}}',
    true, 300, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM policy_types pt WHERE pt.type_code = 'concurrentSessions'
ON CONFLICT (policy_name) DO NOTHING;

-- 10.11 Work Group Login Methods (credential only, no MFA) - Priority 300 (WORK_GROUP level - HIGHEST)
INSERT INTO equipment_policies (
    policy_name, description, policy_classification, policy_application,
    policy_type_id, policy_config, enabled, priority, created_at, updated_at
) SELECT
    'Web Team Login Methods',
    'Login methods for web team: id/password only',
    'common', 'apply', pt.id,
    '{"loginMethods": {"credential": true, "mfa": {"enabled": false}, "autoLogin": false}}',
    true, 300, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM policy_types pt WHERE pt.type_code = 'loginMethods'
ON CONFLICT (policy_name) DO NOTHING;

-- ============================================
-- 11. POLICY ASSIGNMENTS
-- ============================================

-- 11.1 USER_TYPE Assignments (priority 100)
-- Super Admin Login -> SUPER_ADMIN user type
INSERT INTO policy_user_type_assignments (policy_id, user_type_id)
SELECT ep.id, ut.id
FROM equipment_policies ep, user_types ut
WHERE ep.policy_name = 'Super Admin Login Methods'
  AND ut.type_code = 'SUPER_ADMIN'
ON CONFLICT (policy_id, user_type_id) DO NOTHING;

-- Occasional User Login -> OCCASIONAL_USER user type
INSERT INTO policy_user_type_assignments (policy_id, user_type_id)
SELECT ep.id, ut.id
FROM equipment_policies ep, user_types ut
WHERE ep.policy_name = 'Occasional User Login Methods'
  AND ut.type_code = 'OCCASIONAL_USER'
ON CONFLICT (policy_id, user_type_id) DO NOTHING;

-- 11.2 USER Assignments (priority 200)
-- User Lee Login -> user lee
INSERT INTO policy_user_assignments (policy_id, user_id)
SELECT ep.id, u.id
FROM equipment_policies ep, users u
WHERE ep.policy_name = 'User Lee Login Methods'
  AND u.username = 'lee'
ON CONFLICT DO NOTHING;

-- User yhkim Login -> user yhkim
INSERT INTO policy_user_assignments (policy_id, user_id)
SELECT ep.id, u.id
FROM equipment_policies ep, users u
WHERE ep.policy_name = 'User yhkim Login Methods'
  AND u.username = 'yhkim'
ON CONFLICT DO NOTHING;

-- 11.3 ACCOUNT_TYPE Assignments
-- Privileged Account Session Timeout -> PRIVILEGED account type
INSERT INTO policy_account_type_assignments (policy_id, account_type_id)
SELECT ep.id, at.id
FROM equipment_policies ep, account_types at
WHERE ep.policy_name = 'Privileged Account Session Timeout'
  AND at.type_code = 'PRIVILEGED'
ON CONFLICT (policy_id, account_type_id) DO NOTHING;

-- Public Account Session Timeout -> PUBLIC account type
INSERT INTO policy_account_type_assignments (policy_id, account_type_id)
SELECT ep.id, at.id
FROM equipment_policies ep, account_types at
WHERE ep.policy_name = 'Public Account Session Timeout'
  AND at.type_code = 'PUBLIC'
ON CONFLICT (policy_id, account_type_id) DO NOTHING;

-- 11.4 EQUIPMENT Assignments (Device Type based)
-- Oracle Device Session Timeout -> Oracle DB Server
INSERT INTO policy_equipment_assignments (policy_id, equipment_id)
SELECT ep.id, e.id
FROM equipment_policies ep, equipment e
WHERE ep.policy_name = 'Oracle Device Session Timeout'
  AND e.device_type = 'ORACLE'
ON CONFLICT DO NOTHING;

-- ============================================
-- 12. WORK GROUP SETUP
-- ============================================
INSERT INTO work_groups (work_group_name, description, enabled, created_at, updated_at) VALUES
    ('Web Development Team', 'Web project workspace', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Work Group Users: yhkim, lee, sokhim
INSERT INTO work_group_users (work_group_id, user_id)
SELECT wg.id, u.id
FROM work_groups wg, users u
WHERE wg.work_group_name = 'Web Development Team'
  AND u.username IN ('yhkim', 'lee', 'sokhim')
ON CONFLICT (work_group_id, user_id) DO NOTHING;

-- Work Group Equipment: 192.168.10.1, 192.168.10.2
INSERT INTO work_group_equipment (work_group_id, equipment_id)
SELECT wg.id, e.id
FROM work_groups wg, equipment e
WHERE wg.work_group_name = 'Web Development Team'
  AND e.ip_address IN ('192.168.10.1', '192.168.10.2')
ON CONFLICT (work_group_id, equipment_id) DO NOTHING;

-- Work Group Accounts: web_root, web_user
INSERT INTO work_group_accounts (work_group_id, account_id)
SELECT wg.id, a.id
FROM work_groups wg, accounts a
WHERE wg.work_group_name = 'Web Development Team'
  AND a.account_name IN ('web_root', 'web_user')
ON CONFLICT (work_group_id, account_id) DO NOTHING;

-- Work Group Policy Catalog (USER_GROUP priority 150)
INSERT INTO work_group_policies (work_group_id, policy_id)
SELECT wg.id, ep.id
FROM work_groups wg, equipment_policies ep
WHERE wg.work_group_name = 'Web Development Team'
  AND ep.policy_name IN ('Web Team Login Methods', 'Web Team Concurrent Sessions')
ON CONFLICT (work_group_id, policy_id) DO NOTHING;

UPDATE equipment_policies SET priority = 300 WHERE policy_name IN ('Web Team Login Methods', 'Web Team Concurrent Sessions');
