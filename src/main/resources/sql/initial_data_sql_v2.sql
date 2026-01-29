-- ============================================
-- 1. ROLES
-- ============================================
INSERT INTO roles (name, description) VALUES
                                          ('ADMIN',   'System Administrator with full access'),
                                          ('MANAGER', 'Department Manager'),
                                          ('USER',    'Regular User'),
                                          ('VIEWER',  'Read-only access user')
ON CONFLICT (name) DO NOTHING;


-- ============================================
-- 2. USER GROUPS
-- ============================================
INSERT INTO user_groups (group_name, group_description, created_at, updated_at) VALUES
                                                                                    ('IT Team',     'Information Technology department team members', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                    ('Sales Team',  'Sales department team members',                  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                    ('HR Team',     'Human Resources department team members',        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                    ('Managers',    'All department managers',                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (group_name) DO NOTHING;


-- ============================================
-- 3. USERS
-- ============================================
INSERT INTO users (username, password, email, department, level, active) VALUES
                                                                             ('admin',   'admin123',   'admin@example.com',   'IT',    10, true),
                                                                             ('manager', 'manager123', 'manager@example.com', 'SALES',  5, true),
                                                                             ('john',    'john123',    'john@example.com',    'SALES',  3, true),
                                                                             ('jane',    'jane123',    'jane@example.com',    'HR',     3, true),
                                                                             ('bob',     'bob123',     'bob@example.com',     'IT',     3, true),
                                                                             ('alice',   'alice123',   'alice@example.com',   'IT',     2, true)
ON CONFLICT (username) DO NOTHING;


-- ============================================
-- 4. USER_ROLES
-- ============================================
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE (u.username = 'admin'   AND r.name = 'ADMIN')
   OR (u.username = 'manager' AND r.name = 'MANAGER')
   OR (u.username = 'john'    AND r.name = 'USER')
   OR (u.username = 'jane'    AND r.name = 'USER')
   OR (u.username = 'bob'     AND r.name = 'USER')
   OR (u.username = 'alice'   AND r.name = 'VIEWER')
ON CONFLICT DO NOTHING;


-- ============================================
-- 5. USER_GROUP_MEMBERS
-- ============================================
INSERT INTO user_group_members (user_id, group_id)
SELECT u.id, g.id
FROM users u, user_groups g
WHERE (u.username = 'admin'   AND g.group_name = 'IT Team')
   OR (u.username = 'bob'     AND g.group_name = 'IT Team')
   OR (u.username = 'alice'   AND g.group_name = 'IT Team')
   OR (u.username = 'manager' AND g.group_name = 'Sales Team')
   OR (u.username = 'john'    AND g.group_name = 'Sales Team')
   OR (u.username = 'jane'    AND g.group_name = 'HR Team')
   OR (u.username = 'manager' AND g.group_name = 'Managers')
ON CONFLICT DO NOTHING;


-- ============================================
-- 6. EQUIPMENT
-- ============================================
INSERT INTO equipment (
    device_name, host_name, ip_address, protocol, port,
    username, password, device_type, is_deleted, created_at, updated_at
) VALUES
      ('Linux Production Server',  'prod-server-01',   '192.168.1.10', 'SSH',       22,   'root',         'password123', 'LINUX_SERVER',   false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('Windows Server',           'win-server-01',    '192.168.1.20', 'RDP',     3389,   'administrator', 'password123', 'WINDOWS_SERVER', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('PostgreSQL Database',      'db-postgres-01',   '192.168.1.30', 'POSTGRESQL',5432,'postgres',      'password123', 'DATABASE',       false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('MySQL Database',           'db-mysql-01',      '192.168.1.31', 'MYSQL',    3306,  'root',         'password123', 'DATABASE',       false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('Oracle Database',          'db-oracle-01',     '192.168.1.32', 'ORACLE',   1521,  'system',       'password123', 'DATABASE',       false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('SQL Server',               'db-sqlserver-01',  '192.168.1.33', 'SQLSERVER',1433,  'sa',           'password123', 'DATABASE',       false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('Telnet Server',            'telnet-server-01', '192.168.1.40', 'TELNET',   23,    'admin',        'password123', 'NETWORK_DEVICE', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;


-- ============================================
-- 7. USER_EQUIPMENT (optional convenience mapping)
-- ============================================
INSERT INTO user_equipment (user_id, equipment_id)
SELECT u.id, e.id
FROM users u, equipment e
WHERE (u.username = 'admin' AND e.device_name = 'Linux Production Server')
   OR (u.username = 'admin' AND e.device_name = 'Windows Server')
   OR (u.username = 'bob'   AND e.device_name = 'Linux Production Server')
   OR (u.username = 'alice' AND e.device_name = 'PostgreSQL Database')
ON CONFLICT DO NOTHING;


-- ============================================
-- 8. COMMAND_LISTS
-- ============================================
INSERT INTO command_lists (list_name, list_type, protocol_type, created_at) VALUES
                                                                                ('Dangerous Commands Blacklist',      'blacklist', 'TELNET_SSH', CURRENT_TIMESTAMP),
                                                                                ('Database Admin Commands Blacklist', 'blacklist', 'DB',         CURRENT_TIMESTAMP),
                                                                                ('Allowed Commands Whitelist',        'whitelist', 'TELNET_SSH', CURRENT_TIMESTAMP),
                                                                                ('Safe Database Commands Whitelist',  'whitelist', 'DB',         CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;


-- ============================================
-- 9. COMMAND_LIST_ITEMS
-- ============================================

-- Dangerous Commands Blacklist (SSH/TELNET)
INSERT INTO command_list_items (command_list_id, command_text, created_at)
SELECT cl.id, cmd, CURRENT_TIMESTAMP
FROM command_lists cl,
     (VALUES
          ('rm -rf /'),
          ('dd if=/dev/zero of=/dev/sda'),
          ('mkfs.ext4 /dev/sda'),
          ('fdisk /dev/sda'),
          ('chmod 777 /'),
          ('chown root:root /etc/passwd')
     ) AS commands(cmd)
WHERE cl.list_name = 'Dangerous Commands Blacklist'
ON CONFLICT DO NOTHING;

-- Database Admin Commands Blacklist
INSERT INTO command_list_items (command_list_id, command_text, created_at)
SELECT cl.id, cmd, CURRENT_TIMESTAMP
FROM command_lists cl,
     (VALUES
          ('DROP DATABASE'),
          ('DROP TABLE'),
          ('TRUNCATE TABLE'),
          ('DELETE FROM'),
          ('ALTER TABLE DROP'),
          ('GRANT ALL PRIVILEGES')
     ) AS commands(cmd)
WHERE cl.list_name = 'Database Admin Commands Blacklist'
ON CONFLICT DO NOTHING;

-- Allowed Commands Whitelist (SSH/TELNET)
INSERT INTO command_list_items (command_list_id, command_text, created_at)
SELECT cl.id, cmd, CURRENT_TIMESTAMP
FROM command_lists cl,
     (VALUES
          ('ls'),
          ('pwd'),
          ('cd'),
          ('cat'),
          ('grep'),
          ('tail'),
          ('head'),
          ('ps'),
          ('top'),
          ('df'),
          ('du')
     ) AS commands(cmd)
WHERE cl.list_name = 'Allowed Commands Whitelist'
ON CONFLICT DO NOTHING;

-- Safe Database Commands Whitelist
INSERT INTO command_list_items (command_list_id, command_text, created_at)
SELECT cl.id, cmd, CURRENT_TIMESTAMP
FROM command_lists cl,
     (VALUES
          ('SELECT'),
          ('SHOW TABLES'),
          ('DESCRIBE'),
          ('EXPLAIN'),
          ('SHOW DATABASES'),
          ('SHOW COLUMNS')
     ) AS commands(cmd)
WHERE cl.list_name = 'Safe Database Commands Whitelist'
ON CONFLICT DO NOTHING;

-- ============================================
-- 10. POLICY TYPES (Equipment login / Session timeout / Concurrent session)
-- ============================================
INSERT INTO policy_types (
    type_code,
    type_name,
    description,
    is_active,
    created_at,
    updated_at
) VALUES
      ('equipmentLogin',    'Equipment Login Method', 'Default login method for equipment (auto)',     true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('sessionTimeout',    'Session Timeout',        'Default session timeout configuration (seconds)',        true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('concurrentSession', 'Concurrent Session',     'Default concurrent session limits per equipment / user', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('loginControl',      'Login Control',          'User login control settings (MFA, IP filtering, account lock)', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('commonSettings',    'Common Settings',        'Common policy settings (protocols, DBMS, sessions)',     true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('allowedTime',       'Allowed Time',           'Time-based access restrictions (days, hours)',           true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('commandSettings',   'Command Settings',       'Command blocking/whitelisting settings',                 true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;


-- ============================================
-- 11. DEFAULT EQUIPMENT POLICIES (3 policies, one per type)
-- ============================================
-- NOTE: These are default policies; assign them as needed via your API
-- or additional SQL for policy_group_members / assignments.

-- 11.1 Equipment Login Method Policy (auto / manual)
INSERT INTO equipment_policies (
    policy_name,
    description,
    policy_classification,
    policy_application,
    policy_type_id,
    policy_config,
    enabled,
    priority,
    version_no,
    created_at,
    updated_at
)
SELECT
    'Default Equipment Login Method',
    'Default login method for equipment (auto / manual)',
    'common',
    'apply',
    pt.id,
    '{
      "equipmentLogin": {
        "allowedLoginMethods": ["auto"]
      },
      "customMetadata": {
        "notes": "System-wide default equipment login method",
        "createdBy": "system"
      }
    }'::jsonb,
    true,
    100,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM policy_types pt
WHERE pt.type_code = 'equipmentLogin'
ON CONFLICT (policy_name) DO NOTHING;


-- 11.2 Session Timeout Policy (600 seconds)
INSERT INTO equipment_policies (
    policy_name,
    description,
    policy_classification,
    policy_application,
    policy_type_id,
    policy_config,
    enabled,
    priority,
    version_no,
    created_at,
    updated_at
)
SELECT
    'Default Session Timeout',
    'Default session timeout of 600 seconds (10 minutes)',
    'common',
    'apply',
    pt.id,
    '{
      "sessionTimeout": {
        "timeoutSeconds": 600
      },
      "customMetadata": {
        "notes": "System-wide default session timeout",
        "createdBy": "system"
      }
    }'::jsonb,
    true,
    90,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM policy_types pt
WHERE pt.type_code = 'sessionTimeout'
ON CONFLICT (policy_name) DO NOTHING;


-- 11.3 Concurrent Session Policy (50 simultaneous sessions)
INSERT INTO equipment_policies (
    policy_name,
    description,
    policy_classification,
    policy_application,
    policy_type_id,
    policy_config,
    enabled,
    priority,
    version_no,
    created_at,
    updated_at
)
SELECT
    'Default Concurrent Session Limit',
    'Default maximum of 50 concurrent sessions',
    'common',
    'apply',
    pt.id,
    '{
      "concurrentSession": {
        "maxConcurrentSessions": 50
      },
      "customMetadata": {
        "notes": "System-wide default concurrent session limit",
        "createdBy": "system"
      }
    }'::jsonb,
    true,
    80,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM policy_types pt
WHERE pt.type_code = 'concurrentSession'
ON CONFLICT (policy_name) DO NOTHING;

-- ============================================
-- 12. BASIC POLICY GROUP (links default equipment policies)
-- ============================================

-- 12.1 Create Basic Policy Group
INSERT INTO policy_groups (
    group_name,
    description,
    enabled,
    created_by,
    last_updated_by,
    created_at,
    updated_at
) VALUES (
             'Basic Policy Group',
             'Default equipment policies: login method, session timeout, concurrent session',
             true,
             NULL,
             NULL,
             CURRENT_TIMESTAMP,
             CURRENT_TIMESTAMP
         )
ON CONFLICT (group_name) DO NOTHING;


-- 12.2 Assign the 3 default equipment policies to Basic Policy Group
-- Policies:
--  - Default Equipment Login Method
--  - Default Session Timeout
--  - Default Concurrent Session Limit

INSERT INTO policy_group_members (policy_group_id, policy_id)
SELECT pg.id, ep.id
FROM policy_groups pg
         JOIN equipment_policies ep ON ep.policy_name IN (
                                                          'Default Equipment Login Method',
                                                          'Default Session Timeout',
                                                          'Default Concurrent Session Limit'
    )
WHERE pg.group_name = 'Basic Policy Group'
ON CONFLICT (policy_group_id, policy_id) DO NOTHING;

-- ============================================
-- 13. USER TYPES (Phase 1)
-- ============================================
INSERT INTO user_types (type_code, type_name, description, is_active) VALUES
    ('SUPER_ADMIN', 'Super Admin User', 'Super administrator with highest privileges', TRUE),
    ('MIDDLE_MANAGER', 'Middle Manager', 'Department or team manager', TRUE),
    ('REGULAR_USER', 'Regular User', 'Standard user with normal access', TRUE),
    ('OCCASIONAL_USER', 'Occasional User', 'User with limited or occasional access', TRUE)
ON CONFLICT (type_code) DO NOTHING;

-- ============================================
-- 14. UPDATE EXISTING USERS WITH USER TYPES (Phase 1)
-- ============================================
UPDATE users u
SET user_type_id = (
    SELECT id FROM user_types WHERE type_code = 
        CASE 
            WHEN u.username = 'admin' THEN 'SUPER_ADMIN'
            WHEN u.username = 'manager' THEN 'MIDDLE_MANAGER'
            WHEN u.level >= 5 THEN 'REGULAR_USER'
            ELSE 'OCCASIONAL_USER'
        END
    LIMIT 1
)
WHERE user_type_id IS NULL;

-- ============================================
-- 15. ACCOUNT TYPES (Phase 1)
-- ============================================
INSERT INTO account_types (type_code, type_name, description, is_active) VALUES
    ('COLLECTION', 'Collection Account', 'Account used for collecting data or resources', TRUE),
    ('PRIVILEGED', 'Privileged Account', 'Account with elevated privileges', TRUE),
    ('PERSONAL', 'Personal Account', 'Personal user account', TRUE),
    ('SOLUTION', 'Solution Account', 'Account for specific solution or service', TRUE),
    ('PUBLIC', 'Public Account', 'Public or shared account', TRUE)
ON CONFLICT (type_code) DO NOTHING;

-- ============================================
-- 16. SAMPLE ACCOUNTS FOR EQUIPMENT (Phase 1)
-- ============================================
-- Root account for Linux Production Server
INSERT INTO accounts (account_name, account_type_id, equipment_id, username, password, description, is_active)
SELECT 
    'root',
    (SELECT id FROM account_types WHERE type_code = 'PRIVILEGED' LIMIT 1),
    e.id,
    'root',
    'password123',
    'Root account for Linux server',
    TRUE
FROM equipment e
WHERE e.device_type = 'LINUX_SERVER' AND e.device_name = 'Linux Production Server'
ON CONFLICT (account_name, equipment_id) DO NOTHING;

-- Web root account for Linux Production Server
INSERT INTO accounts (account_name, account_type_id, equipment_id, username, password, description, is_active)
SELECT 
    'web_root',
    (SELECT id FROM account_types WHERE type_code = 'PRIVILEGED' LIMIT 1),
    e.id,
    'webadmin',
    'password123',
    'Web root account',
    TRUE
FROM equipment e
WHERE e.device_type = 'LINUX_SERVER' AND e.device_name = 'Linux Production Server'
ON CONFLICT (account_name, equipment_id) DO NOTHING;

-- Web user account for Linux Production Server
INSERT INTO accounts (account_name, account_type_id, equipment_id, username, password, description, is_active)
SELECT 
    'web_user',
    (SELECT id FROM account_types WHERE type_code = 'PERSONAL' LIMIT 1),
    e.id,
    'webuser',
    'password123',
    'Web user account',
    TRUE
FROM equipment e
WHERE e.device_type = 'LINUX_SERVER' AND e.device_name = 'Linux Production Server'
ON CONFLICT (account_name, equipment_id) DO NOTHING;

-- ============================================
-- 17. WORK GROUPS (Phase 2)
-- ============================================
INSERT INTO work_groups (work_group_name, description, enabled, created_at, updated_at) VALUES
    ('Web Development Team', 'Web development project workspace with Linux servers', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Database Administration', 'Database administration workspace', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('IT Operations', 'General IT operations workspace', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (work_group_name) DO NOTHING;

-- ============================================
-- 18. WORK GROUP USERS (Phase 2)
-- ============================================
-- Web Development Team: admin, bob
INSERT INTO work_group_users (work_group_id, user_id)
SELECT wg.id, u.id
FROM work_groups wg, users u
WHERE wg.work_group_name = 'Web Development Team'
  AND u.username IN ('admin', 'bob')
ON CONFLICT (work_group_id, user_id) DO NOTHING;

-- Database Administration: admin, alice
INSERT INTO work_group_users (work_group_id, user_id)
SELECT wg.id, u.id
FROM work_groups wg, users u
WHERE wg.work_group_name = 'Database Administration'
  AND u.username IN ('admin', 'alice')
ON CONFLICT (work_group_id, user_id) DO NOTHING;

-- IT Operations: admin, manager, bob
INSERT INTO work_group_users (work_group_id, user_id)
SELECT wg.id, u.id
FROM work_groups wg, users u
WHERE wg.work_group_name = 'IT Operations'
  AND u.username IN ('admin', 'manager', 'bob')
ON CONFLICT (work_group_id, user_id) DO NOTHING;

-- ============================================
-- 19. WORK GROUP EQUIPMENT (Phase 2)
-- ============================================
-- Web Development Team: Linux Production Server
INSERT INTO work_group_equipment (work_group_id, equipment_id)
SELECT wg.id, e.id
FROM work_groups wg, equipment e
WHERE wg.work_group_name = 'Web Development Team'
  AND e.device_name = 'Linux Production Server'
ON CONFLICT (work_group_id, equipment_id) DO NOTHING;

-- Database Administration: PostgreSQL Database, MySQL Database
INSERT INTO work_group_equipment (work_group_id, equipment_id)
SELECT wg.id, e.id
FROM work_groups wg, equipment e
WHERE wg.work_group_name = 'Database Administration'
  AND e.device_name IN ('PostgreSQL Database', 'MySQL Database')
ON CONFLICT (work_group_id, equipment_id) DO NOTHING;

-- IT Operations: Linux Production Server, Windows Server
INSERT INTO work_group_equipment (work_group_id, equipment_id)
SELECT wg.id, e.id
FROM work_groups wg, equipment e
WHERE wg.work_group_name = 'IT Operations'
  AND e.device_name IN ('Linux Production Server', 'Windows Server')
ON CONFLICT (work_group_id, equipment_id) DO NOTHING;

-- ============================================
-- 20. WORK GROUP ACCOUNTS (Phase 2)
-- ============================================
-- Web Development Team: root, web_root, web_user accounts on Linux Production Server
INSERT INTO work_group_accounts (work_group_id, account_id)
SELECT wg.id, a.id
FROM work_groups wg, accounts a, equipment e
WHERE wg.work_group_name = 'Web Development Team'
  AND a.equipment_id = e.id
  AND e.device_name = 'Linux Production Server'
ON CONFLICT (work_group_id, account_id) DO NOTHING;

-- ============================================
-- 21. WORK GROUP POLICIES (Phase 2 - Policy Catalog)
-- ============================================
-- Assign default policies to Web Development Team
INSERT INTO work_group_policies (work_group_id, policy_id)
SELECT wg.id, ep.id
FROM work_groups wg, equipment_policies ep
WHERE wg.work_group_name = 'Web Development Team'
  AND ep.policy_name IN ('Default Equipment Login Method', 'Default Session Timeout', 'Default Concurrent Session Limit')
ON CONFLICT (work_group_id, policy_id) DO NOTHING;

-- Assign default policies to Database Administration
INSERT INTO work_group_policies (work_group_id, policy_id)
SELECT wg.id, ep.id
FROM work_groups wg, equipment_policies ep
WHERE wg.work_group_name = 'Database Administration'
  AND ep.policy_name IN ('Default Equipment Login Method', 'Default Session Timeout')
ON CONFLICT (work_group_id, policy_id) DO NOTHING;

-- Assign default policies to IT Operations
INSERT INTO work_group_policies (work_group_id, policy_id)
SELECT wg.id, ep.id
FROM work_groups wg, equipment_policies ep
WHERE wg.work_group_name = 'IT Operations'
  AND ep.policy_name IN ('Default Equipment Login Method', 'Default Session Timeout', 'Default Concurrent Session Limit')
ON CONFLICT (work_group_id, policy_id) DO NOTHING;


-- ============================================
-- 22. USER TYPE BASED POLICIES (Phase 3)
-- ============================================
-- Enhanced login policy for Super Admin users (credential + MFA with OTP)
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
) SELECT
    'Super Admin Enhanced Login',
    'Enhanced login security for super admin users: credential + MFA (OTP)',
    'common',
    'apply',
    pt.id,
    '{"loginControl": {"loginMethod": "credential_mfa", "mfaType": "otp", "ipFilteringType": "no_restrictions", "allowedIps": [], "twoFactorType": "otp", "accountLockEnabled": true, "maxFailureAttempts": 3, "lockoutDurationMinutes": 30}}',
    true,
    200,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM policy_types pt
WHERE pt.type_code = 'loginControl'
ON CONFLICT (policy_name) DO NOTHING;

-- Standard login policy for Normal User (credential + MFA with email)
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
) SELECT
    'Normal User Standard Login',
    'Standard login for normal users: credential + MFA (email)',
    'common',
    'apply',
    pt.id,
    '{"loginControl": {"loginMethod": "credential_mfa", "mfaType": "email", "ipFilteringType": "no_restrictions", "allowedIps": [], "twoFactorType": "email", "accountLockEnabled": true, "maxFailureAttempts": 5, "lockoutDurationMinutes": 15}}',
    true,
    100,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM policy_types pt
WHERE pt.type_code = 'loginControl'
ON CONFLICT (policy_name) DO NOTHING;


-- ============================================
-- 23. ACCOUNT TYPE BASED POLICIES (Phase 3)
-- ============================================
-- Strict session policy for Privileged accounts
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
) SELECT
    'Privileged Account Strict Session',
    'Strict session limits for privileged accounts: shorter timeout, fewer concurrent sessions',
    'common',
    'apply',
    pt.id,
    '{"sessionTimeout": {"timeoutMinutes": 30, "idleTimeMinutes": 10, "warningBeforeMinutes": 5}}',
    true,
    150,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM policy_types pt
WHERE pt.type_code = 'sessionTimeout'
ON CONFLICT (policy_name) DO NOTHING;


-- ============================================
-- 24. POLICY USER TYPE ASSIGNMENTS (Phase 3)
-- ============================================
-- Assign "Super Admin Enhanced Login" to SUPER_ADMIN user type
INSERT INTO policy_user_type_assignments (policy_id, user_type_id)
SELECT ep.id, ut.id
FROM equipment_policies ep, user_types ut
WHERE ep.policy_name = 'Super Admin Enhanced Login'
  AND ut.type_code = 'SUPER_ADMIN'
ON CONFLICT (policy_id, user_type_id) DO NOTHING;

-- Assign "Normal User Standard Login" to NORMAL_USER user type
INSERT INTO policy_user_type_assignments (policy_id, user_type_id)
SELECT ep.id, ut.id
FROM equipment_policies ep, user_types ut
WHERE ep.policy_name = 'Normal User Standard Login'
  AND ut.type_code = 'NORMAL_USER'
ON CONFLICT (policy_id, user_type_id) DO NOTHING;


-- ============================================
-- 25. POLICY ACCOUNT TYPE ASSIGNMENTS (Phase 3)
-- ============================================
-- Assign "Privileged Account Strict Session" to PRIVILEGED account type
INSERT INTO policy_account_type_assignments (policy_id, account_type_id)
SELECT ep.id, at.id
FROM equipment_policies ep, account_types at
WHERE ep.policy_name = 'Privileged Account Strict Session'
  AND at.type_code = 'PRIVILEGED'
ON CONFLICT (policy_id, account_type_id) DO NOTHING;


-- ============================================
-- 26. UPDATE USERS WITH USER TYPES (Phase 3)
-- ============================================
-- Set admin as SUPER_ADMIN
UPDATE users SET user_type_id = (SELECT id FROM user_types WHERE type_code = 'SUPER_ADMIN')
WHERE username = 'admin' AND user_type_id IS NULL;

-- Set manager as NORMAL_USER
UPDATE users SET user_type_id = (SELECT id FROM user_types WHERE type_code = 'NORMAL_USER')
WHERE username = 'manager' AND user_type_id IS NULL;

-- Set other users as NORMAL_USER
UPDATE users SET user_type_id = (SELECT id FROM user_types WHERE type_code = 'NORMAL_USER')
WHERE username IN ('john', 'jane', 'bob', 'alice') AND user_type_id IS NULL;