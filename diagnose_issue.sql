-- ============================================
-- Diagnostic SQL Script
-- Run this to check what's wrong with your data
-- ============================================

-- 1. Check if users exist
SELECT '=== USERS ===' as section;
SELECT id, username, email, department, level, active FROM users ORDER BY username;

-- 2. Check if roles exist
SELECT '=== ROLES ===' as section;
SELECT id, name, description FROM roles ORDER BY name;

-- 3. Check user-role mappings (CRITICAL - this might be missing!)
SELECT '=== USER-ROLE MAPPINGS ===' as section;
SELECT 
    u.id as user_id,
    u.username,
    r.id as role_id,
    r.name as role_name
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
ORDER BY u.username, r.name;

-- 4. Check if admin user has ADMIN role
SELECT '=== ADMIN USER ROLE CHECK ===' as section;
SELECT 
    u.username,
    r.name as role_name
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.username = 'admin';

-- 5. Check if policies exist and are enabled
SELECT '=== ACCESS POLICIES ===' as section;
SELECT 
    id,
    policy_name,
    endpoint,
    http_method,
    allowed_roles,
    enabled,
    priority,
    CASE 
        WHEN generated_drl IS NULL OR generated_drl = '' THEN 'MISSING DRL'
        ELSE 'HAS DRL'
    END as drl_status
FROM access_policies
ORDER BY priority DESC;

-- 6. Check the Admin Full Access policy specifically
SELECT '=== ADMIN FULL ACCESS POLICY DETAILS ===' as section;
SELECT 
    policy_name,
    endpoint,
    http_method,
    allowed_roles,
    enabled,
    priority,
    LEFT(generated_drl, 200) as drl_preview
FROM access_policies
WHERE policy_name = 'Admin Full Access';

-- 7. Count summary
SELECT '=== SUMMARY COUNTS ===' as section;
SELECT 
    (SELECT COUNT(*) FROM users) as user_count,
    (SELECT COUNT(*) FROM roles) as role_count,
    (SELECT COUNT(*) FROM user_roles) as user_role_mapping_count,
    (SELECT COUNT(*) FROM access_policies) as policy_count,
    (SELECT COUNT(*) FROM access_policies WHERE enabled = true) as enabled_policy_count;

