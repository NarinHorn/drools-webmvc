

-- ============================================
-- Initial Data SQL Script for AccessPolicy (ABAC)
-- Matches current AccessPolicy entity:
--   access_policies(id, policy_name, description, endpoint, http_method,
--                   allowed_roles, conditions JSONB, effect, priority,
--                   enabled, generated_drl, created_at, updated_at)
-- ============================================

-- ============================================
-- 0. Ensure conditions is JSONB (idempotent)
-- ============================================
DO $$
    BEGIN
        -- If column is TEXT, convert to JSONB
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'access_policies'
              AND column_name = 'conditions'
              AND data_type = 'text'
        ) THEN
            UPDATE access_policies
            SET conditions = conditions::jsonb
            WHERE conditions IS NOT NULL
              AND conditions != 'null'
              AND conditions != '';

            ALTER TABLE access_policies
                ALTER COLUMN conditions TYPE JSONB
                    USING CASE
                              WHEN conditions IS NULL OR conditions = '' OR conditions = 'null' THEN NULL
                              ELSE conditions::jsonb
                    END;
        END IF;
    END $$;

-- ============================================
-- 1. INSERT ROLES
-- ============================================
INSERT INTO roles (name, description) VALUES
                                          ('ADMIN',   'System Administrator with full access'),
                                          ('MANAGER', 'Department Manager'),
                                          ('USER',    'Regular User'),
                                          ('VIEWER',  'Read-only access')
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- 2. INSERT USERS
-- ============================================
-- Note: IDs are auto-generated, we reference them via username in user_roles
INSERT INTO users (username, password, email, department, level, active) VALUES
                                                                             ('admin',   'admin123',   'admin@example.com',   'IT',    10, true),
                                                                             ('manager', 'manager123', 'manager@example.com', 'SALES',  5, true),
                                                                             ('john',    'john123',    'john@example.com',    'SALES',  3, true),
                                                                             ('jane',    'jane123',    'jane@example.com',    'HR',     3, true),
                                                                             ('viewer',  'viewer123',  'viewer@example.com',  'GUEST',  1, true)
ON CONFLICT (username) DO NOTHING;

-- ============================================
-- 3. INSERT USER_ROLES (Many-to-Many)
-- ============================================
-- Admin → ADMIN
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- Manager → MANAGER
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'manager' AND r.name = 'MANAGER'
ON CONFLICT DO NOTHING;

-- John → USER
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'john' AND r.name = 'USER'
ON CONFLICT DO NOTHING;

-- Jane → USER
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'jane' AND r.name = 'USER'
ON CONFLICT DO NOTHING;

-- Viewer → VIEWER
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'viewer' AND r.name = 'VIEWER'
ON CONFLICT DO NOTHING;

-- ============================================
-- 4. INSERT ACCESS POLICIES (matches AccessPolicy.java)
-- ============================================
-- Fields:
--   policy_name, description, endpoint, http_method, allowed_roles (TEXT JSON string),
--   conditions (JSONB or NULL), effect, priority, enabled, generated_drl, created_at, updated_at

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

-- Policy 4: Sales Department Data Access (with JSONB conditions)
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

-- Policy 5: Management Level Access (uses userLevel condition)
INSERT INTO access_policies (
    policy_name, description, endpoint, http_method, allowed_roles,
    conditions, effect, priority, enabled, generated_drl, created_at, updated_at
) VALUES (
             'Management Level Access',
             'Users with level 5+ can access management endpoints',
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
-- 5. OPTIONAL VERIFICATION QUERIES (commented)
-- ============================================
-- SELECT COUNT(*) AS role_count   FROM roles;
-- SELECT COUNT(*) AS user_count   FROM users;
-- SELECT COUNT(*) AS user_role_count FROM user_roles;
-- SELECT COUNT(*) AS policy_count FROM access_policies;
-- SELECT policy_name, endpoint, http_method, allowed_roles, enabled
-- FROM access_policies
-- ORDER BY priority DESC;