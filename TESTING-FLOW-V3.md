# Complete Testing Flow Guide - AccessPolicy with User Groups Integration V3

This document provides a comprehensive step-by-step testing guide for the AccessPolicy system with User Groups integration. This version focuses on testing the flexibility of applying AccessPolicy to both roles and user groups, demonstrating how users can gain access through either their roles or their group memberships.

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Database Setup & Migration](#database-setup--migration)
3. [Testing User Groups](#testing-user-groups)
4. [Testing AccessPolicy with Groups](#testing-accesspolicy-with-groups)
5. [Testing Access Control - Role-Based](#testing-access-control---role-based)
6. [Testing Access Control - Group-Based](#testing-access-control---group-based)
7. [Testing Combined Scenarios](#testing-combined-scenarios)
8. [Testing Flexibility & Edge Cases](#testing-flexibility--edge-cases)
9. [Verifying DRL Generation](#verifying-drl-generation)
10. [Troubleshooting](#troubleshooting)

---

## 🎯 Prerequisites

Before starting the testing flow, ensure:

1. **Application is running** on `http://localhost:8081`
2. **Database is initialized** with initial data (roles, users)
3. **Swagger UI is accessible** at `http://localhost:8081/swagger-ui.html`
4. **Postman or cURL** is available for API testing
5. **PostgreSQL client** (psql) for database verification

### Verify Application Status

```bash
# Check if application is running
curl http://localhost:8081/api/public/info

# Expected response:
# {
#   "endpoint": "/api/public/info",
#   "company": "Hunesion Inc.",
#   "version": "1.0.0",
#   "message": "Public information endpoint"
# }
```

---

## ✅ Database Setup & Migration

### Step 1: Run Database Migration

**1.1 Execute the Migration Script**

```bash
# Run the access_policy_group_assignments table creation script
psql -U postgres -d abacdb -f src/main/resources/sql/create_access_policy_group_assignments_table.sql
```

**1.2 Verify Table Creation**

```sql
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
```

**1.3 Verify Foreign Keys**

```sql
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
```

**Expected Foreign Keys:**
- `fk_access_policy_group_policy` → `access_policies(id)`
- `fk_access_policy_group_group` → `user_groups(id)`

---

## 👥 Testing User Groups

### Step 2: Create User Groups

**2.1 Create Sales Team Group**

```bash
POST http://localhost:8081/api/user-groups
Content-Type: application/json

{
  "groupName": "Sales Team",
  "groupDescription": "Sales department team members",
  "memberIds": []
}
```

**Expected Response:**
```json
{
  "id": 1,
  "groupName": "Sales Team",
  "groupDescription": "Sales department team members",
  "createdAt": "2024-01-15T10:00:00",
  "updatedAt": "2024-01-15T10:00:00"
}
```

**2.2 Create IT Team Group**

```bash
POST http://localhost:8081/api/user-groups
Content-Type: application/json

{
  "groupName": "IT Team",
  "groupDescription": "IT department team members",
  "memberIds": []
}
```

**2.3 Create Managers Group**

```bash
POST http://localhost:8081/api/user-groups
Content-Type: application/json

{
  "groupName": "Managers",
  "groupDescription": "All department managers",
  "memberIds": []
}
```

**2.4 Verify Groups Created**

```bash
GET http://localhost:8081/api/user-groups
```

**Expected Response:** Should list all created groups

---

### Step 3: Assign Users to Groups

**3.1 Get User IDs**

```bash
GET http://localhost:8081/api/users
```

**Note the IDs for:**
- `john` (USER role, SALES department) - Let's say ID = 3
- `jane` (USER role, HR department) - Let's say ID = 4
- `manager` (MANAGER role) - Let's say ID = 2

**3.2 Add Users to Sales Team**

```bash
POST http://localhost:8081/api/user-groups/1/members
Content-Type: application/json

[3]
```

**Expected Response:**
```json
{
  "message": "Members added successfully",
  "count": "1"
}
```

**3.3 Add Manager to Managers Group**

```bash
POST http://localhost:8081/api/user-groups/3/members
Content-Type: application/json

[2]
```

**3.4 Verify Group Members**

```bash
GET http://localhost:8081/api/user-groups/1/members
```

**Expected Response:** Should return user `john`

```bash
GET http://localhost:8081/api/user-groups/3/members
```

**Expected Response:** Should return user `manager`

---

## 🔐 Testing AccessPolicy with Groups

### Step 4: Create AccessPolicy with Role Assignment (Baseline)

**4.1 Create Role-Based Policy**

```bash
POST http://localhost:8081/api/policies
Content-Type: application/json

{
  "policyName": "Manager Reports Access (Role)",
  "description": "Managers can access reports via role",
  "endpoint": "/api/reports/**",
  "httpMethod": "GET",
  "allowedRoles": ["MANAGER"],
  "groupIds": null,
  "effect": "ALLOW",
  "priority": 100,
  "enabled": true
}
```

**Expected Response:** Policy created with ID

**4.2 Verify Policy Created**

```bash
GET http://localhost:8081/api/policies/{id}
```

**Note:** The policy should have `allowedRoles` but no `groupAssignments`

---

### Step 5: Create AccessPolicy with Group Assignment

**5.1 Create Group-Based Policy**

```bash
POST http://localhost:8081/api/policies
Content-Type: application/json

{
  "policyName": "Sales Team Reports Access (Group)",
  "description": "Sales team members can access reports via group",
  "endpoint": "/api/reports/**",
  "httpMethod": "GET",
  "allowedRoles": null,
  "groupIds": [1],
  "effect": "ALLOW",
  "priority": 90,
  "enabled": true
}
```

**Expected Response:** Policy created with ID

**5.2 Verify Group Assignment in Database**

```sql
-- Check group assignments
SELECT 
    apga.id,
    ap.policy_name,
    ug.group_name
FROM access_policy_group_assignments apga
JOIN access_policies ap ON apga.access_policy_id = ap.id
JOIN user_groups ug ON apga.group_id = ug.id
WHERE ap.policy_name = 'Sales Team Reports Access (Group)';
```

**Expected Result:** Should show the policy linked to "Sales Team" group

**5.3 Get Policy with Group Assignments**

```bash
GET http://localhost:8081/api/policies/{id}
```

**Note:** The response may not show groupAssignments directly (lazy loading), but they exist in the database.

---

### Step 6: Create AccessPolicy with Both Roles and Groups

**6.1 Create Combined Policy**

```bash
POST http://localhost:8081/api/policies
Content-Type: application/json

{
  "policyName": "Managers or IT Team Access",
  "description": "Access for managers (role) or IT team (group)",
  "endpoint": "/api/admin/**",
  "httpMethod": "*",
  "allowedRoles": ["MANAGER"],
  "groupIds": [2],
  "effect": "ALLOW",
  "priority": 80,
  "enabled": true
}
```

**Expected Response:** Policy created with both role and group assignments

**6.2 Verify Combined Assignment**

```sql
-- Check both role and group assignments
SELECT 
    ap.policy_name,
    ap.allowed_roles,
    ug.group_name
FROM access_policies ap
LEFT JOIN access_policy_group_assignments apga ON ap.id = apga.access_policy_id
LEFT JOIN user_groups ug ON apga.group_id = ug.id
WHERE ap.policy_name = 'Managers or IT Team Access';
```

---

## 🧪 Testing Access Control - Role-Based

### Step 7: Test Role-Based Access (Baseline)

**7.1 Test Manager Access via Role**

```bash
GET http://localhost:8081/api/access/check?username=manager&endpoint=/api/reports/sales&method=GET
```

**Expected Response:**
```json
{
  "username": "manager",
  "endpoint": "/api/reports/sales",
  "method": "GET",
  "allowed": true,
  "matchedPolicy": "Manager Reports Access (Role)"
}
```

**7.2 Test Non-Manager Access (Should Fail)**

```bash
GET http://localhost:8081/api/access/check?username=jane&endpoint=/api/reports/sales&method=GET
```

**Expected Response:**
```json
{
  "username": "jane",
  "endpoint": "/api/reports/sales",
  "method": "GET",
  "allowed": false,
  "matchedPolicy": null,
  "reason": "No access policy found for this endpoint"
}
```

**Note:** `jane` doesn't have MANAGER role, so role-based policy doesn't apply.

---

## 🧪 Testing Access Control - Group-Based

### Step 8: Test Group-Based Access

**8.1 Test Sales Team Member Access via Group**

```bash
GET http://localhost:8081/api/access/check?username=john&endpoint=/api/reports/sales&method=GET
```

**Expected Response:**
```json
{
  "username": "john",
  "endpoint": "/api/reports/sales",
  "method": "GET",
  "allowed": true,
  "matchedPolicy": "Sales Team Reports Access (Group)"
}
```

**✅ Success!** `john` now has access because:
- He is a member of "Sales Team" group
- The policy is assigned to "Sales Team" group
- The DRL rule checks `hasGroup("Sales Team")`

**8.2 Test Non-Group Member Access (Should Fail)**

```bash
GET http://localhost:8081/api/access/check?username=jane&endpoint=/api/reports/sales&method=GET
```

**Expected Response:**
```json
{
  "username": "jane",
  "endpoint": "/api/reports/sales",
  "method": "GET",
  "allowed": false,
  "matchedPolicy": null,
  "reason": "No access policy found for this endpoint"
}
```

**Note:** `jane` is not in "Sales Team" group, so group-based policy doesn't apply.

**8.3 Verify User Groups are Loaded**

```bash
POST http://localhost:8081/api/access/check
Content-Type: application/json

{
  "username": "john",
  "userRoles": ["USER"],
  "userGroups": ["Sales Team"],
  "endpoint": "/api/reports/sales",
  "httpMethod": "GET"
}
```

**Expected Response:**
```json
{
  "allowed": true,
  "matchedPolicyName": "Sales Team Reports Access (Group)",
  "evaluated": true
}
```

---

## 🧪 Testing Combined Scenarios

### Step 9: Test Policies with Both Roles and Groups

**9.1 Test Manager Access via Role (Combined Policy)**

```bash
GET http://localhost:8081/api/access/check?username=manager&endpoint=/api/admin/users&method=GET
```

**Expected Response:**
```json
{
  "username": "manager",
  "endpoint": "/api/admin/users",
  "method": "GET",
  "allowed": true,
  "matchedPolicy": "Managers or IT Team Access"
}
```

**✅ Success!** Manager has access via role check.

**9.2 Test IT Team Member Access via Group (Combined Policy)**

First, add a user to IT Team group:

```bash
# Get a user ID (e.g., jane, ID = 4)
POST http://localhost:8081/api/user-groups/2/members
Content-Type: application/json

[4]
```

Then test:

```bash
GET http://localhost:8081/api/access/check?username=jane&endpoint=/api/admin/users&method=GET
```

**Expected Response:**
```json
{
  "username": "jane",
  "endpoint": "/api/admin/users",
  "method": "GET",
  "allowed": true,
  "matchedPolicy": "Managers or IT Team Access"
}
```

**✅ Success!** `jane` has access via group check, even though she doesn't have MANAGER role.

---

### Step 10: Test Multiple Policies (Priority & Flexibility)

**10.1 Create Multiple Overlapping Policies**

```bash
# Policy 1: Role-based (higher priority)
POST http://localhost:8081/api/policies
Content-Type: application/json

{
  "policyName": "Admin Full Access (Role)",
  "description": "Admins have full access",
  "endpoint": "/api/**",
  "httpMethod": "*",
  "allowedRoles": ["ADMIN"],
  "groupIds": null,
  "effect": "ALLOW",
  "priority": 200,
  "enabled": true
}

# Policy 2: Group-based (lower priority)
POST http://localhost:8081/api/policies
Content-Type: application/json

{
  "policyName": "Managers Group Access",
  "description": "Managers group has access",
  "endpoint": "/api/**",
  "httpMethod": "*",
  "allowedRoles": null,
  "groupIds": [3],
  "effect": "ALLOW",
  "priority": 150,
  "enabled": true
}
```

**10.2 Test User with Both Role and Group**

```bash
# Test manager who is also in Managers group
GET http://localhost:8081/api/access/check?username=manager&endpoint=/api/users&method=GET
```

**Expected Response:** Should match the highest priority policy (Admin Full Access if manager has ADMIN role, or Managers Group Access if not).

---

## 🔍 Testing Flexibility & Edge Cases

### Step 11: Test Policy Updates

**11.1 Update Policy to Add Groups**

```bash
# Get existing policy ID
GET http://localhost:8081/api/policies

# Update policy to add group assignment
PUT http://localhost:8081/api/policies/{id}
Content-Type: application/json

{
  "policyName": "Manager Reports Access (Role)",
  "description": "Managers can access reports via role OR group",
  "endpoint": "/api/reports/**",
  "httpMethod": "GET",
  "allowedRoles": ["MANAGER"],
  "groupIds": [3],
  "effect": "ALLOW",
  "priority": 100,
  "enabled": true
}
```

**11.2 Verify Update**

```sql
-- Check group assignments after update
SELECT 
    ap.policy_name,
    ap.allowed_roles,
    ug.group_name
FROM access_policies ap
LEFT JOIN access_policy_group_assignments apga ON ap.id = apga.access_policy_id
LEFT JOIN user_groups ug ON apga.group_id = ug.id
WHERE ap.policy_name = 'Manager Reports Access (Role)';
```

**11.3 Test Access After Update**

```bash
# Test manager via role
GET http://localhost:8081/api/access/check?username=manager&endpoint=/api/reports/sales&method=GET

# Test user in Managers group (but not manager role)
# create user request:
{
  "username": "manager2",
  "password": "password123",
  "email": "testuser@example.com",
  "department": "MANAGERS",
  "level": 1,
  "active": true
}
# First add a user to Managers group, then test
```

---

### Step 12: Test Policy Removal

**12.1 Remove Group Assignment**

```bash
PUT http://localhost:8081/api/policies/{id}
Content-Type: application/json

{
  "policyName": "Manager Reports Access (Role)",
  "description": "Managers can access reports via role only",
  "endpoint": "/api/reports/**",
  "httpMethod": "GET",
  "allowedRoles": ["MANAGER"],
  "groupIds": [],
  "effect": "ALLOW",
  "priority": 100,
  "enabled": true
}
```

**12.2 Verify Group Assignment Removed**

```sql
SELECT COUNT(*) 
FROM access_policy_group_assignments 
WHERE access_policy_id = {id};
```

**Expected Result:** 0 (no group assignments)

---

Create SSH Access Policy Request
POST http://localhost:8081/api/policies
Content-Type: application/json
X-Username: admin

{
"policyName": "SSH_Access_192.168.0.211",
"description": "Allow SSH access to server 192.168.0.211 for authorized users",
"endpoint": "/api/guacamole/connect/**",
"httpMethod": "POST",
"allowedRoles": ["ADMIN", "SSH_USER"],
"groupIds": null,
"conditions": {
"protocol": {
"operator": "equals",
"value": "ssh"
},
"hostname": {
"operator": "equals",
"value": "192.168.0.211"
},
"port": {
"operator": "equals",
"value": "22"
},
"username": {
"operator": "equals",
"value": "narin"
}
},
"effect": "ALLOW",
"priority": 50,
"enabled": true
}

TEST SSH ACCESS
curl -X POST http://localhost:8081/api/guacamole/connect/narin@192.168.0.211:22/ -d '{

















### Step 13: Test Edge Cases

**13.1 Test User in Multiple Groups**

```bash
# Add user to multiple groups
POST http://localhost:8081/api/user-groups/1/members
Content-Type: application/json

[4]  # Add jane to Sales Team

# Create policy for Sales Team
POST http://localhost:8081/api/policies
Content-Type: application/json

{
  "policyName": "Sales Team Access",
  "endpoint": "/api/sales/**",
  "httpMethod": "*",
  "allowedRoles": null,
  "groupIds": [1],
  "effect": "ALLOW",
  "priority": 50,
  "enabled": true
}

# Test jane (in both IT Team and Sales Team)
GET http://localhost:8081/api/access/check?username=jane&endpoint=/api/sales/data&method=GET
```

**Expected:** Should have access via Sales Team group

**13.2 Test User with Role but Not in Group**

```bash
# Test manager (has MANAGER role) accessing Sales Team endpoint
GET http://localhost:8081/api/access/check?username=manager&endpoint=/api/sales/data&method=GET
```

**Expected:** Should be denied (no matching policy)

**13.3 Test Disabled Policy**

```bash
# Disable a policy
PATCH http://localhost:8081/api/policies/{id}/toggle?enabled=false

# Test access
GET http://localhost:8081/api/access/check?username=john&endpoint=/api/reports/sales&method=GET
```

**Expected:** Should be denied (policy is disabled)

---

## 📝 Verifying DRL Generation

### Step 14: Verify Generated DRL Rules

**14.1 Preview DRL for Group-Based Policy**

```bash
POST http://localhost:8081/api/policies/preview-drl
Content-Type: application/json

{
  "policyName": "Sales Team Access",
  "endpoint": "/api/sales/**",
  "httpMethod": "*",
  "allowedRoles": null,
  "groupIds": [1],
  "effect": "ALLOW",
  "priority": 50,
  "enabled": true
}
```

**Expected DRL:**
```drl
package rules.dynamic;

import com.hunesion.drool_v2.model.AccessRequest;
import com.hunesion.drool_v2.model.AccessResult;

rule "Sales Team Access"
    salience 50
    when
        $request : AccessRequest(
            endpointMatches("/api/sales(/.*)?"),
            (hasGroup("Sales Team"))
        )
        $result : AccessResult(evaluated == false)
    then
        $result.allow("Sales Team Access");
        System.out.println("✓ Access ALLOWED by policy: Sales Team Access");
end
```

**Key Points:**
- ✅ Contains `hasGroup("Sales Team")` check
- ✅ Endpoint pattern converted to regex
- ✅ Priority (salience) set correctly

**14.2 Preview DRL for Combined Policy**

```bash
POST http://localhost:8081/api/policies/preview-drl
Content-Type: application/json

{
  "policyName": "Managers or IT Team Access",
  "endpoint": "/api/admin/**",
  "httpMethod": "*",
  "allowedRoles": ["MANAGER"],
  "groupIds": [2],
  "effect": "ALLOW",
  "priority": 80,
  "enabled": true
}
```

**Expected DRL:**
```drl
package rules.dynamic;

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
```

**Key Points:**
- ✅ Contains both `hasRole("MANAGER")` and `hasGroup("IT Team")`
- ✅ Uses `||` (OR) operator - user needs either role OR group
- ✅ Both conditions are checked

**14.3 Get Actual Generated DRL from Database**

```sql
SELECT 
    policy_name,
    generated_drl
FROM access_policies
WHERE policy_name = 'Sales Team Reports Access (Group)';
```

**Verify:**
- DRL contains `hasGroup()` calls
- Group names are correctly extracted from group IDs
- DRL syntax is valid

---

## 🔧 Troubleshooting

### Common Issues and Solutions

**Issue 1: User has group but access is denied**

**Check:**
1. Verify user is in the group:
   ```bash
   GET http://localhost:8081/api/user-groups/{groupId}/members
   ```

2. Verify policy is assigned to the group:
   ```sql
   SELECT * FROM access_policy_group_assignments 
   WHERE group_id = {groupId};
   ```

3. Verify policy is enabled:
   ```sql
   SELECT enabled FROM access_policies WHERE id = {policyId};
   ```

4. Check DRL generation:
   ```sql
   SELECT generated_drl FROM access_policies WHERE id = {policyId};
   ```
   Look for `hasGroup("GroupName")` in the DRL.

**Issue 2: Group assignment not created**

**Check:**
1. Verify group exists:
   ```bash
   GET http://localhost:8081/api/user-groups/{groupId}
   ```

2. Check for errors in application logs when creating policy

3. Verify database constraints:
   ```sql
   SELECT * FROM access_policy_group_assignments 
   WHERE access_policy_id = {policyId};
   ```

**Issue 3: DRL doesn't contain group checks**

**Check:**
1. Verify `groupIds` were provided in PolicyDTO:
   ```sql
   -- Check if assignments exist
   SELECT COUNT(*) FROM access_policy_group_assignments 
   WHERE access_policy_id = {policyId};
   ```

2. Regenerate DRL:
   ```bash
   # Update the policy to trigger DRL regeneration
   PUT http://localhost:8081/api/policies/{id}
   # (with same data)
   ```

3. Check application logs for errors during DRL generation

**Issue 4: User groups not loaded in AccessRequest**

**Check:**
1. Verify `AccessControlService.checkAccess()` loads groups:
   ```java
   // Should contain: request.setUserGroups(user.getGroupNames());
   ```

2. Test with explicit group names:
   ```bash
   POST http://localhost:8081/api/access/check
   Content-Type: application/json
   
   {
     "username": "john",
     "userRoles": ["USER"],
     "userGroups": ["Sales Team"],
     "endpoint": "/api/reports/sales",
     "httpMethod": "GET"
   }
   ```

**Issue 5: Foreign key constraint errors**

**Solution:**
- Ensure `access_policies` table exists
- Ensure `user_groups` table exists
- Verify foreign key constraints are created correctly:
  ```sql
  \d access_policy_group_assignments
  ```

---

## ✅ Testing Checklist

Use this checklist to ensure all functionality is tested:

### Setup
- [ ] Database migration script executed successfully
- [ ] `access_policy_group_assignments` table exists
- [ ] Foreign keys are properly configured
- [ ] User groups created
- [ ] Users assigned to groups

### Policy Creation
- [ ] Create policy with only roles (baseline)
- [ ] Create policy with only groups
- [ ] Create policy with both roles and groups
- [ ] Verify group assignments in database
- [ ] Verify DRL generation includes group checks

### Access Control Testing
- [ ] User with role can access (role-based policy)
- [ ] User with group can access (group-based policy)
- [ ] User with either role or group can access (combined policy)
- [ ] User without role or group is denied
- [ ] User in multiple groups can access multiple policies
- [ ] Disabled policies don't grant access

### Flexibility Testing
- [ ] Update policy to add groups
- [ ] Update policy to remove groups
- [ ] Update policy to change groups
- [ ] Policy with both roles and groups works correctly
- [ ] Multiple policies with different priorities work correctly

### DRL Verification
- [ ] DRL contains `hasGroup()` calls
- [ ] Group names are correctly extracted
- [ ] Combined policies use `||` operator correctly
- [ ] DRL syntax is valid

---

## 📊 Summary

This testing flow demonstrates:

1. **Flexibility**: AccessPolicy can be applied to:
   - Roles only (existing functionality)
   - User groups only (new functionality)
   - Both roles and groups (combined)

2. **User Access**: Users can gain access through:
   - Their roles
   - Their group memberships
   - Either roles or groups (when policy allows both)

3. **DRL Generation**: The system correctly:
   - Converts group IDs to group names
   - Generates `hasGroup()` checks in DRL
   - Combines role and group checks with OR logic

4. **Database Integration**: Group assignments are:
   - Properly stored in `access_policy_group_assignments`
   - Linked via foreign keys
   - Cascade deleted when policies are removed

---

## 🎯 Next Steps

After completing this testing flow:

1. **Performance Testing**: Test with large numbers of:
   - User groups
   - Group assignments
   - Policies with groups

2. **Integration Testing**: Test with:
   - Multiple policies for same endpoint
   - Conflicting policies (different priorities)
   - Complex group hierarchies (if implemented)

3. **Production Readiness**:
   - Review error handling
   - Add logging for group-based access
   - Monitor query performance
   - Consider caching group memberships

---

**Document Version:** 3.0  
**Last Updated:** 2024-01-15  
**Related Documents:** TESTING-FLOW.md, TESTING-FLOW-V2.md
