# Policy Group Testing Guide

This document provides comprehensive test scenarios for the **PolicyGroup** feature to ensure it works correctly with equipment policy checking.

---

## Prerequisites

1. Application is running on `http://localhost:8080`
2. Database tables are created (run migration or let Flyway handle it)
3. You have existing test data:
   - Users (e.g., `admin`, `viewer`, `developer`)
   - Roles (e.g., `ADMIN`, `USER`, `DEVELOPER`)
   - UserGroups (e.g., `IT Team`, `DBA Team`)
   - Equipment (e.g., Linux servers with SSH protocol)
   - EquipmentPolicies with `allowedProtocols` configured

---

## Table of Contents

1. [Setup Test Data](#1-setup-test-data)
2. [PolicyGroup CRUD Operations](#2-policygroup-crud-operations)
3. [Policy Members Management](#3-policy-members-management)
4. [User Assignments](#4-user-assignments)
5. [UserGroup Assignments](#5-usergroup-assignments)
6. [Role Assignments](#6-role-assignments)
7. [Equipment Access Testing](#7-equipment-access-testing)
8. [Edge Cases & Validation](#8-edge-cases--validation)

---

## 1. Setup Test Data

### 1.1 Create Test Policies (if not exists)

```bash
# Create SSH Access Policy
curl -X POST http://localhost:8080/api/equipment-policies \
  -H "Content-Type: application/json" \
  -d '{
    "policyName": "SSH Access Policy",
    "description": "Allows SSH protocol access",
    "policyClassification": "common",
    "policyApplication": "apply",
    "enabled": true,
    "priority": 100,
    "policyConfig": {
      "commonSettings": {
        "allowedProtocols": ["SSH"]
      }
    }
  }'

# Create Database Access Policy
curl -X POST http://localhost:8080/api/equipment-policies \
  -H "Content-Type: application/json" \
  -d '{
    "policyName": "Database Access Policy",
    "description": "Allows database protocols",
    "policyClassification": "common",
    "policyApplication": "apply",
    "enabled": true,
    "priority": 100,
    "policyConfig": {
      "commonSettings": {
        "allowedProtocols": ["MYSQL", "POSTGRESQL", "ORACLE"],
        "allowedDbms": ["MySQL", "PostgreSQL", "Oracle"]
      }
    }
  }'

# Create RDP Access Policy
curl -X POST http://localhost:8080/api/equipment-policies \
  -H "Content-Type: application/json" \
  -d '{
    "policyName": "RDP Access Policy",
    "description": "Allows RDP protocol access",
    "policyClassification": "common",
    "policyApplication": "apply",
    "enabled": true,
    "priority": 100,
    "policyConfig": {
      "commonSettings": {
        "allowedProtocols": ["RDP"]
      }
    }
  }'
```

---

## 2. PolicyGroup CRUD Operations

### 2.1 Create a PolicyGroup

```bash
# Create "Developer Access Bundle"
curl -X POST http://localhost:8080/api/policy-groups \
  -H "Content-Type: application/json" \
  -d '{
    "groupName": "Developer Access Bundle",
    "description": "All policies needed for developers",
    "enabled": true
  }'
```

**Expected Response:**
```json
{
  "id": 1,
  "groupName": "Developer Access Bundle",
  "description": "All policies needed for developers",
  "enabled": true,
  "createdAt": "2026-01-19T14:00:00",
  "updatedAt": "2026-01-19T14:00:00"
}
```

### 2.2 Create PolicyGroup with Policies in One Request

```bash
curl -X POST http://localhost:8080/api/policy-groups \
  -H "Content-Type: application/json" \
  -d '{
    "groupName": "DBA Access Bundle",
    "description": "All policies needed for DBAs",
    "enabled": true,
    "policyIds": [1, 2]
  }'
```

### 2.3 Get All PolicyGroups

```bash
curl -X GET http://localhost:8080/api/policy-groups
```

### 2.4 Get PolicyGroup by ID

```bash
curl -X GET http://localhost:8080/api/policy-groups/1
```

### 2.5 Update PolicyGroup

```bash
curl -X PUT http://localhost:8080/api/policy-groups/1 \
  -H "Content-Type: application/json" \
  -d '{
    "groupName": "Developer Access Bundle v2",
    "description": "Updated description",
    "enabled": true
  }'
```

### 2.6 Toggle PolicyGroup (Enable/Disable)

```bash
# Disable
curl -X PATCH "http://localhost:8080/api/policy-groups/1/toggle?enabled=false"

# Enable
curl -X PATCH "http://localhost:8080/api/policy-groups/1/toggle?enabled=true"
```

### 2.7 Delete PolicyGroup

```bash
curl -X DELETE http://localhost:8080/api/policy-groups/1
```

---

## 3. Policy Members Management

### 3.1 Add Policies to PolicyGroup

```bash
# Add policy IDs 1, 2, 3 to PolicyGroup ID 1
curl -X POST http://localhost:8080/api/policy-groups/1/policies \
  -H "Content-Type: application/json" \
  -d '[1, 2, 3]'
```

**Expected Response:**
```json
{
  "message": "Policies added to group successfully",
  "count": 3
}
```

### 3.2 Get Policies in PolicyGroup

```bash
curl -X GET http://localhost:8080/api/policy-groups/1/policies
```

### 3.3 Replace All Policies in PolicyGroup

```bash
curl -X PUT http://localhost:8080/api/policy-groups/1/policies \
  -H "Content-Type: application/json" \
  -d '[4, 5]'
```

### 3.4 Remove Policies from PolicyGroup

```bash
curl -X DELETE http://localhost:8080/api/policy-groups/1/policies \
  -H "Content-Type: application/json" \
  -d '[2]'
```

---

## 4. User Assignments

### 4.1 Assign PolicyGroup to Users

```bash
# Assign PolicyGroup ID 1 to User IDs 1, 2, 3
curl -X POST http://localhost:8080/api/policy-groups/1/users \
  -H "Content-Type: application/json" \
  -d '[1, 2, 3]'
```

### 4.2 Get Users Assigned to PolicyGroup

```bash
curl -X GET http://localhost:8080/api/policy-groups/1/users
```

### 4.3 Remove Users from PolicyGroup

```bash
curl -X DELETE http://localhost:8080/api/policy-groups/1/users \
  -H "Content-Type: application/json" \
  -d '[2]'
```

---

## 5. UserGroup Assignments

### 5.1 Assign PolicyGroup to UserGroups

```bash
# Assign PolicyGroup ID 1 to UserGroup IDs 1, 2
curl -X POST http://localhost:8080/api/policy-groups/1/user-groups \
  -H "Content-Type: application/json" \
  -d '[1, 2]'
```

### 5.2 Get UserGroups Assigned to PolicyGroup

```bash
curl -X GET http://localhost:8080/api/policy-groups/1/user-groups
```

### 5.3 Remove UserGroups from PolicyGroup

```bash
curl -X DELETE http://localhost:8080/api/policy-groups/1/user-groups \
  -H "Content-Type: application/json" \
  -d '[1]'
```

---

## 6. Role Assignments

### 6.1 Assign PolicyGroup to Roles

```bash
# Assign PolicyGroup ID 1 to Role IDs 1, 2
curl -X POST http://localhost:8080/api/policy-groups/1/roles \
  -H "Content-Type: application/json" \
  -d '[1, 2]'
```

### 6.2 Get Roles Assigned to PolicyGroup

```bash
curl -X GET http://localhost:8080/api/policy-groups/1/roles
```

### 6.3 Remove Roles from PolicyGroup

```bash
curl -X DELETE http://localhost:8080/api/policy-groups/1/roles \
  -H "Content-Type: application/json" \
  -d '[1]'
```

---

## 7. Equipment Access Testing

This is the most important section - verifying that PolicyGroup assignments correctly affect equipment access.

### 7.1 Test Scenario: User Gets Access via PolicyGroup

**Setup:**
1. Create PolicyGroup "SSH Bundle" with SSH Access Policy
2. Assign PolicyGroup to user "developer"
3. User "developer" should now have SSH access

```bash
# Step 1: Create PolicyGroup
curl -X POST http://localhost:8080/api/policy-groups \
  -H "Content-Type: application/json" \
  -d '{
    "groupName": "SSH Bundle",
    "description": "SSH access bundle",
    "enabled": true
  }'

# Step 2: Add SSH policy (assuming policy ID 5 has SSH)
curl -X POST http://localhost:8080/api/policy-groups/1/policies \
  -H "Content-Type: application/json" \
  -d '[5]'

# Step 3: Assign to user (assuming user ID 3 is "developer")
curl -X POST http://localhost:8080/api/policy-groups/1/users \
  -H "Content-Type: application/json" \
  -d '[3]'

# Step 4: Test SSH access
curl -X GET "http://localhost:8080/api/clients/ssh?username=developer&equipmentId=1"
```

**Expected Result:** Access ALLOWED with matched policy name

### 7.2 Test Scenario: User Gets Access via UserGroup → PolicyGroup

**Setup:**
1. User "viewer" is member of UserGroup "IT Team"
2. PolicyGroup "IT Bundle" is assigned to UserGroup "IT Team"
3. PolicyGroup contains SSH Access Policy
4. User "viewer" should have SSH access through the chain

```bash
# Assign PolicyGroup to UserGroup
curl -X POST http://localhost:8080/api/policy-groups/1/user-groups \
  -H "Content-Type: application/json" \
  -d '[1]'

# Test access for user in that group
curl -X GET "http://localhost:8080/api/clients/ssh?username=viewer&equipmentId=1"
```

### 7.3 Test Scenario: User Gets Access via Role → PolicyGroup

**Setup:**
1. User "admin" has role "ADMIN"
2. PolicyGroup "Admin Bundle" is assigned to role "ADMIN"
3. User "admin" should have access through role assignment

```bash
# Assign PolicyGroup to Role
curl -X POST http://localhost:8080/api/policy-groups/1/roles \
  -H "Content-Type: application/json" \
  -d '[1]'

# Test access
curl -X GET "http://localhost:8080/api/clients/ssh?username=admin&equipmentId=1"
```

### 7.4 Test Scenario: Disabled PolicyGroup Should Not Grant Access

```bash
# Disable the PolicyGroup
curl -X PATCH "http://localhost:8080/api/policy-groups/1/toggle?enabled=false"

# Test access - should be DENIED now
curl -X GET "http://localhost:8080/api/clients/ssh?username=developer&equipmentId=1"

# Re-enable
curl -X PATCH "http://localhost:8080/api/policy-groups/1/toggle?enabled=true"
```

### 7.5 Debug Endpoint for Verification

Use the debug endpoint to see full policy resolution:

```bash
curl -X GET "http://localhost:8080/api/clients/ssh/debug?username=developer&equipmentId=1"
```

**Check console logs for:**
```
=== Policy Resolution Debug ===
User: developer (ID: 3)
User Roles: [DEVELOPER]
User Groups: [IT Team]
Target Equipment ID: 1
Assigned Policy IDs: [5, 6, 7]  <-- Should include policies from PolicyGroups
```

---

## 8. Edge Cases & Validation

### 8.1 Duplicate Policy in Multiple PolicyGroups

**Test:** User is assigned to 2 PolicyGroups that both contain the same policy.

**Expected:** Policy should only be evaluated once (Set prevents duplicates).

```bash
# Create two PolicyGroups with same policy
curl -X POST http://localhost:8080/api/policy-groups/2/policies -H "Content-Type: application/json" -d '[5]'
curl -X POST http://localhost:8080/api/policy-groups/3/policies -H "Content-Type: application/json" -d '[5]'

# Assign both to same user
curl -X POST http://localhost:8080/api/policy-groups/2/users -H "Content-Type: application/json" -d '[3]'
curl -X POST http://localhost:8080/api/policy-groups/3/users -H "Content-Type: application/json" -d '[3]'

# Test - should work without issues
curl -X GET "http://localhost:8080/api/clients/ssh?username=developer&equipmentId=1"
```

### 8.2 Empty PolicyGroup

**Test:** PolicyGroup with no policies assigned.

**Expected:** No additional policies granted, but no errors.

```bash
curl -X POST http://localhost:8080/api/policy-groups \
  -H "Content-Type: application/json" \
  -d '{"groupName": "Empty Bundle", "enabled": true}'

curl -X POST http://localhost:8080/api/policy-groups/4/users \
  -H "Content-Type: application/json" \
  -d '[3]'

# Should not affect access
curl -X GET "http://localhost:8080/api/clients/ssh?username=developer&equipmentId=1"
```

### 8.3 Cascading Delete

**Test:** Deleting a PolicyGroup should remove all assignments.

```bash
# Delete PolicyGroup
curl -X DELETE http://localhost:8080/api/policy-groups/1

# Verify assignments are gone (check database)
# SELECT * FROM policy_group_user_assignments WHERE policy_group_id = 1;
```

### 8.4 Unique Constraint Validation

**Test:** Cannot add same policy twice to same PolicyGroup.

```bash
# Add policy 5
curl -X POST http://localhost:8080/api/policy-groups/1/policies -H "Content-Type: application/json" -d '[5]'

# Try to add again - should be idempotent (no error, no duplicate)
curl -X POST http://localhost:8080/api/policy-groups/1/policies -H "Content-Type: application/json" -d '[5]'

# Verify only one entry exists
curl -X GET http://localhost:8080/api/policy-groups/1/policies
```

### 8.5 PolicyGroup Name Uniqueness

**Test:** Cannot create two PolicyGroups with same name.

```bash
curl -X POST http://localhost:8080/api/policy-groups \
  -H "Content-Type: application/json" \
  -d '{"groupName": "Test Bundle", "enabled": true}'

# Second attempt should fail
curl -X POST http://localhost:8080/api/policy-groups \
  -H "Content-Type: application/json" \
  -d '{"groupName": "Test Bundle", "enabled": true}'
```

**Expected:** Error response indicating duplicate name.

---

## Summary Checklist

| Test Case | Status |
|-----------|--------|
| Create PolicyGroup | ☐ |
| Update PolicyGroup | ☐ |
| Delete PolicyGroup | ☐ |
| Toggle PolicyGroup enabled/disabled | ☐ |
| Add policies to PolicyGroup | ☐ |
| Remove policies from PolicyGroup | ☐ |
| Assign PolicyGroup to User | ☐ |
| Assign PolicyGroup to UserGroup | ☐ |
| Assign PolicyGroup to Role | ☐ |
| User access via direct PolicyGroup assignment | ☐ |
| User access via UserGroup → PolicyGroup | ☐ |
| User access via Role → PolicyGroup | ☐ |
| Disabled PolicyGroup blocks access | ☐ |
| Duplicate policies handled correctly | ☐ |
| Empty PolicyGroup no errors | ☐ |
| Cascading delete works | ☐ |
| Unique constraints enforced | ☐ |

---

## Troubleshooting

### Policies Not Being Resolved

1. Check console logs for "Policy Resolution Debug"
2. Verify PolicyGroup is `enabled = true`
3. Verify policies in PolicyGroup are `enabled = true` and `policyApplication = 'apply'`
4. Check that user/group/role assignments exist in database

### SQL Queries for Debugging

```sql
-- Check PolicyGroup assignments
SELECT * FROM policy_groups;
SELECT * FROM policy_group_members;
SELECT * FROM policy_group_user_assignments;
SELECT * FROM policy_group_user_group_assignments;
SELECT * FROM policy_group_role_assignments;

-- Check which policies a user gets via PolicyGroups
SELECT DISTINCT ep.id, ep.policy_name
FROM policy_groups pg
JOIN policy_group_members pgm ON pg.id = pgm.policy_group_id
JOIN equipment_policies ep ON pgm.policy_id = ep.id
JOIN policy_group_user_assignments pgua ON pg.id = pgua.policy_group_id
WHERE pgua.user_id = 3 AND pg.enabled = true;
```
