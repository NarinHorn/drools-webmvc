# Complete Testing Flow Guide - Equipment Policy Management System V2 (JSONB Implementation)

This document provides a comprehensive step-by-step testing guide for the Equipment Policy Management System with Drools integration and JSONB-based policy configuration. It covers all aspects including user groups, equipment policies, policy assignments, and access control evaluation.

**Key Implementation Notes:**
- Equipment policies now use **JSONB `policy_config`** field instead of normalized tables
- All policy settings (commonSettings, allowedTime, loginControl, commandSettings) are stored in a single JSONB column
- Supports **customConditions** and **customMetadata** for flexible frontend customization
- Policy configuration is cached for performance (30-minute TTL, 1000 entries)

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Database Setup & Verification](#database-setup--verification)
3. [Testing User Groups](#testing-user-groups)
4. [Testing Equipment Policies](#testing-equipment-policies)
5. [Testing Policy Assignments](#testing-policy-assignments)
6. [Testing Policy Settings](#testing-policy-settings)
7. [Testing Drools Rule Generation](#testing-drools-rule-generation)
8. [Testing Equipment Access Control](#testing-equipment-access-control)
9. [Integration Testing Scenarios](#integration-testing-scenarios)
10. [Performance Testing](#performance-testing)
11. [Troubleshooting](#troubleshooting)

---

## 🎯 Prerequisites

Before starting the testing flow, ensure:

1. **Application is running** on `http://localhost:8081`
2. **Database is initialized** with all schema tables created
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

### Verify Database Schema

```sql
-- Check if all tables exist (JSONB implementation - no normalized policy tables)
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
    'command_lists',
    'command_list_items',
    'access_policies',
    'access_policy_group_assignments'
  )
ORDER BY table_name;

-- Verify equipment_policies has policy_config JSONB column
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'equipment_policies' 
  AND column_name = 'policy_config';
-- Expected: policy_config | jsonb
```

---

## ✅ Database Setup & Verification

### Step 1: Run Database Migration Scripts

```bash
# Run the initial data script (includes schema setup and test data)
psql -U postgres -d abacdb -f src/main/resources/sql/initial_data_sql_v2.sql
```

**Note:** The `initial_data_sql_v2.sql` script includes:
- Roles, Users, User Groups
- Access Policies (with group assignments)
- Equipment Policies (with JSONB `policy_config`)
- Equipment, Command Lists
- Policy Assignments

### Step 2: Verify Initial Data

**2.1 Check Users Exist**

```bash
GET http://localhost:8081/api/users
```

**Expected Response:** Should return at least:
- admin (ADMIN role)
- manager (MANAGER role)
- john (USER role, SALES department)
- jane (USER role, HR department)
- viewer (VIEWER role)

**2.2 Check Roles Exist**

```bash
GET http://localhost:8081/api/roles
```

**Expected Response:** Should return:
- ADMIN
- MANAGER
- USER
- VIEWER

**2.3 Check Equipment Exist**

```bash
GET http://localhost:8081/api/equipment
```

**Expected Response:** Should return equipment entries (if initial data was loaded)

---

## 👥 Testing User Groups

### Test Case 1: Create User Group

**Request:**
```bash
POST http://localhost:8081/api/user-groups
Content-Type: application/json

{
  "groupName": "IT Team",
  "groupDescription": "IT Department Team Members"
}
```

**Expected Response:** `201 Created`
```json
{
  "id": 1,
  "groupName": "IT Team",
  "groupDescription": "IT Department Team Members",
  "createdAt": "2024-01-15T10:00:00",
  "updatedAt": "2024-01-15T10:00:00"
}
```

**Verification:**
```bash
GET http://localhost:8081/api/user-groups/1
```

### Test Case 2: Add Members to Group

**Request:**
```bash
POST http://localhost:8081/api/user-groups/1/members
Content-Type: application/json

[1, 2]
```

**Note:** Use user IDs (e.g., 1 = admin, 2 = manager)

**Expected Response:** `200 OK`
```json
{
  "message": "Members added successfully",
  "count": "2"
}
```

**Verification:**
```bash
GET http://localhost:8081/api/user-groups/1/members
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com"
  },
  {
    "id": 2,
    "username": "manager",
    "email": "manager@example.com"
  }
]
```

### Test Case 3: Create Multiple Groups

Create groups for different departments:

```bash
# Create Sales Team group
POST http://localhost:8081/api/user-groups
{
  "groupName": "Sales Team",
  "groupDescription": "Sales Department Team"
}

# Create HR Team group
POST http://localhost:8081/api/user-groups
{
  "groupName": "HR Team",
  "groupDescription": "Human Resources Team"
}
```

### Test Case 4: Remove Members from Group

**Request:**
```bash
DELETE http://localhost:8081/api/user-groups/1/members
Content-Type: application/json

[2]
```

**Expected Response:** `200 OK`
```json
{
  "message": "Members removed successfully",
  "count": "1"
}
```

### Test Case 5: Update Group

**Request:**
```bash
PUT http://localhost:8081/api/user-groups/1
Content-Type: application/json

{
  "groupName": "IT Department",
  "groupDescription": "Updated description for IT Department"
}
```

**Expected Response:** `200 OK` with updated group information

### Test Case 6: Delete Group

**Request:**
```bash
DELETE http://localhost:8081/api/user-groups/1
```

**Expected Response:** `200 OK`
```json
{
  "message": "Group deleted successfully",
  "id": "1"
}
```

**Note:** This will also remove all group memberships automatically. The delete operation handles foreign key constraints by removing members first.

---

## 🛡️ Testing Equipment Policies

### Test Case 1: Create Basic Policy

**Request:**
```bash
POST http://localhost:8081/api/equipment-policies
Content-Type: application/json

{
  "policyName": "Basic SSH Access",
  "description": "Basic SSH access policy for IT team",
  "policyClassification": "common",
  "policyApplication": "apply",
  "enabled": true,
  "priority": 100
}
```

**Expected Response:** `201 Created`
```json
{
  "id": 1,
  "policyName": "Basic SSH Access",
  "description": "Basic SSH access policy for IT team",
  "policyClassification": "common",
  "policyApplication": "apply",
  "enabled": true,
  "priority": 100,
  "createdAt": "2024-01-15T10:00:00",
  "updatedAt": "2024-01-15T10:00:00"
}
```

**Verification:**
```bash
GET http://localhost:8081/api/equipment-policies/1
```

### Test Case 2: Create Policy with Common Settings

**Request:**
```bash
POST http://localhost:8081/api/equipment-policies
Content-Type: application/json

{
  "policyName": "IT Team Multi-Protocol Access",
  "description": "IT team can access SSH, TELNET, and RDP",
  "policyClassification": "common",
  "policyApplication": "apply",
  "enabled": true,
  "priority": 150,
  "commonSettings": {
    "allowedProtocols": ["SSH", "TELNET", "RDP"],
    "allowedDbms": ["MySQL", "PostgreSQL"],
    "idleTimeMinutes": 30,
    "timeoutMinutes": 60,
    "maxSshSessions": 5,
    "sshBorderless": false,
    "maxRdpSessions": 3,
    "rdpBorderless": false,
    "maxTelnetSessions": 2,
    "telnetBorderless": false
  }
}
```

**Expected Response:** `201 Created` with full policy details including `policyConfig` JSONB field

**Database Verification:**
```sql
-- Check policy was created with JSONB config
SELECT 
  id, 
  policy_name, 
  policy_config::jsonb->'commonSettings'->>'allowedProtocols' as protocols,
  policy_config::jsonb->'commonSettings'->>'allowedDbms' as dbms
FROM equipment_policies 
WHERE policy_name = 'IT Team Multi-Protocol Access';

-- View full policy_config JSONB structure
SELECT policy_config::jsonb 
FROM equipment_policies 
WHERE policy_name = 'IT Team Multi-Protocol Access';

-- Expected: protocols should contain ["SSH", "TELNET", "RDP"]
```

### Test Case 3: Create Policy with Time Restrictions

**Request:**
```bash
POST http://localhost:8081/api/equipment-policies
Content-Type: application/json

{
  "policyName": "Business Hours Access",
  "description": "Access only during business hours",
  "policyClassification": "common",
  "policyApplication": "apply",
  "enabled": true,
  "priority": 100,
  "allowedTime": {
    "startDate": "2024-01-01",
    "endDate": "2024-12-31",
    "borderless": false,
    "timeZone": "UTC",
    "timeSlots": [
      {"dayOfWeek": 1, "hourStart": 9, "hourEnd": 18},
      {"dayOfWeek": 2, "hourStart": 9, "hourEnd": 18},
      {"dayOfWeek": 3, "hourStart": 9, "hourEnd": 18},
      {"dayOfWeek": 4, "hourStart": 9, "hourEnd": 18},
      {"dayOfWeek": 5, "hourStart": 9, "hourEnd": 18}
    ]
  }
}
```
**Expected Response:** `201 Created`

**Database Verification:**
```sql
-- Check time slots in JSONB policy_config
SELECT 
  policy_name,
  jsonb_array_elements(policy_config::jsonb->'allowedTime'->'timeSlots') as time_slot
FROM equipment_policies 
WHERE policy_name = 'Business Hours Access';

-- Or extract specific fields
SELECT 
  policy_name,
  jsonb_array_elements(policy_config::jsonb->'allowedTime'->'timeSlots')->>'dayOfWeek' as day_of_week,
  jsonb_array_elements(policy_config::jsonb->'allowedTime'->'timeSlots')->>'hourStart' as hour_start,
  jsonb_array_elements(policy_config::jsonb->'allowedTime'->'timeSlots')->>'hourEnd' as hour_end
FROM equipment_policies 
WHERE policy_name = 'Business Hours Access'
ORDER BY day_of_week, hour_start;

-- Expected: 5 rows (Monday-Friday, 9-18)
```

### Test Case 4: Create Policy with Command Control

**First, create a command list:**

```bash
# This would be done via CommandListController (if created)
# For now, we'll test via direct database or assume it exists

# Create policy with command settings
POST http://localhost:8081/api/equipment-policies
Content-Type: application/json

{
  "policyName": "Secure Command Policy",
  "description": "Blocks dangerous commands",
  "policyClassification": "common",
  "policyApplication": "apply",
  "enabled": true,
  "priority": 200,
  "commandSettings": [
    {
      "protocolType": "TELNET_SSH",
      "controlMethod": "blacklist",
      "controlTarget": "command",
      "commandListIds": [1]
    }
  ]
}
```

**Note:** Assumes command list with ID 1 exists. Create it first if needed.

### Test Case 5: Create Policy with Login Control

**Request:**
```bash
POST http://localhost:8081/api/equipment-policies
Content-Type: application/json

{
  "policyName": "Restricted IP Access",
  "description": "Only allow access from specific IPs",
  "policyClassification": "common",
  "policyApplication": "apply",
  "enabled": true,
  "priority": 100,
  "loginControl": {
    "ipFilteringType": "whitelist",
    "allowedIps": ["192.168.1.0/24", "10.0.0.1"],
    "accountLockEnabled": true,
    "maxFailureAttempts": 3,
    "lockoutDurationMinutes": 30,
    "twoFactorType": "none"
  }
}
```

**Expected Response:** `201 Created`

**Database Verification:**
```sql
-- Check login control in JSONB policy_config
SELECT 
  policy_name,
  policy_config::jsonb->'loginControl'->>'ipFilteringType' as ip_filtering_type,
  policy_config::jsonb->'loginControl'->'allowedIps' as allowed_ips
FROM equipment_policies 
WHERE policy_name = 'Restricted IP Access';

-- Extract allowed IPs array
SELECT 
  policy_name,
  jsonb_array_elements_text(policy_config::jsonb->'loginControl'->'allowedIps') as ip_address
FROM equipment_policies 
WHERE policy_name = 'Restricted IP Access';

-- Expected: 192.168.1.0/24, 10.0.0.1
```

### Test Case 6: Create Complete Policy (All Settings)

**Request:**
```bash
POST http://localhost:8081/api/equipment-policies
Content-Type: application/json

{
  "policyName": "Complete IT Team Policy",
  "description": "Full-featured policy with all settings",
  "policyClassification": "common",
  "policyApplication": "apply",
  "enabled": true,
  "priority": 200,
  "commonSettings": {
    "allowedProtocols": ["SSH", "RDP"],
    "allowedDbms": ["MySQL", "PostgreSQL"],
    "maxSshSessions": 5,
    "maxRdpSessions": 3
  },
  "allowedTime": {
    "startDate": "2024-01-01",
    "endDate": "2024-12-31",
    "borderless": false,
    "timeSlots": [
      {"dayOfWeek": 1, "hourStart": 9, "hourEnd": 18},
      {"dayOfWeek": 2, "hourStart": 9, "hourEnd": 18},
      {"dayOfWeek": 3, "hourStart": 9, "hourEnd": 18},
      {"dayOfWeek": 4, "hourStart": 9, "hourEnd": 18},
      {"dayOfWeek": 5, "hourStart": 9, "hourEnd": 18}
    ]
  },
  "loginControl": {
    "ipFilteringType": "no_restrictions",
    "twoFactorType": "none"
  },
  "customConditions": {
    "deviceType": {
      "operator": "equals",
      "value": "LINUX_SERVER"
    }
  },
  "customMetadata": {
    "createdBy": "admin",
    "department": "IT",
    "notes": "Complete policy example"
  },
  "userIds": [1, 2],
  "groupIds": [1]
}
```

**Expected Response:** `201 Created` with complete policy details

### Test Case 7: Update Policy

**Request:**
```bash
PUT http://localhost:8081/api/equipment-policies/1
Content-Type: application/json

{
  "policyName": "Updated SSH Access",
  "description": "Updated description",
  "policyClassification": "common",
  "policyApplication": "apply",
  "enabled": true,
  "priority": 150,
  "commonSettings": {
    "allowedProtocols": ["SSH", "TELNET"],
    "maxSshSessions": 10
  }
}
```

**Expected Response:** `200 OK` with updated policy

**Verification:**
```bash
GET http://localhost:8081/api/equipment-policies/1
```

### Test Case 8: Toggle Policy (Enable/Disable)

**Request:**
```bash
# Disable policy
PATCH http://localhost:8081/api/equipment-policies/1/toggle?enabled=false

# Enable policy
PATCH http://localhost:8081/api/equipment-policies/1/toggle?enabled=true
```

**Expected Response:** `200 OK` with updated policy (enabled field changed)

**Note:** Disabled policies won't be evaluated by Drools

### Test Case 9: Delete Policy

**Request:**
```bash
DELETE http://localhost:8081/api/equipment-policies/1
```

**Expected Response:** `200 OK`
```json
{
  "message": "Policy deleted successfully",
  "id": "1"
}
```

**Note:** This will cascade delete all related assignments. The JSONB `policy_config` is automatically deleted with the policy.

---

## 🔗 Testing Policy Assignments

**Important:** Policy assignments are now managed via separate RESTful endpoints. When creating a policy, you create it first, then assign it to users, groups, equipment, or roles using the assignment endpoints.

### Test Case 1: Create Policy (Without Assignments)

**Step 1: Create Policy**
```bash
POST http://localhost:8081/api/equipment-policies
Content-Type: application/json

{
  "policyName": "User-Specific Policy",
  "description": "Policy assigned to specific user",
  "policyClassification": "common",
  "policyApplication": "apply",
  "enabled": true,
  "priority": 100
}
```

**Expected Response:** `201 Created`
```json
{
  "id": 1,
  "policyName": "User-Specific Policy",
  ...
}
```

**Note:** Save the `id` from the response for assignment operations.

### Test Case 2: Assign Policy to User

**Step 1: Create Policy First** (see Test Case 1)

**Step 2: Assign to User(s)**
```bash
POST http://localhost:8081/api/equipment-policies/1/assignments/users
Content-Type: application/json

[1]
```

**Expected Response:** `200 OK`
```json
{
  "message": "Users assigned successfully",
  "count": 1
}
```

**Alternative: Assign Multiple Users**
```bash
POST http://localhost:8081/api/equipment-policies/1/assignments/users
Content-Type: application/json

[1, 2, 3]
```

**Get Current User Assignments:**
```bash
GET http://localhost:8081/api/equipment-policies/1/assignments/users
```

**Expected Response:** `200 OK`
```json
[
  {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    ...
  }
]
```

**Database Verification:**
```sql
SELECT u.username, p.policy_name
FROM users u
JOIN policy_user_assignments pua ON u.id = pua.user_id
JOIN equipment_policies p ON pua.policy_id = p.id
WHERE p.id = 1;
```

### Test Case 3: Assign Policy to Group

**Step 1: Create Policy** (if not already created)

**Step 2: Assign to Group(s)**
```bash
POST http://localhost:8081/api/equipment-policies/1/assignments/groups
Content-Type: application/json

[1]
```

**Expected Response:** `200 OK`
```json
{
  "message": "Groups assigned successfully",
  "count": 1
}
```

**Get Current Group Assignments:**
```bash
GET http://localhost:8081/api/equipment-policies/1/assignments/groups
```

**Verification:**
```sql
SELECT g.group_name, p.policy_name
FROM user_groups g
JOIN policy_group_assignments pga ON g.id = pga.group_id
JOIN equipment_policies p ON pga.policy_id = p.id
WHERE p.id = 1;
```

### Test Case 4: Assign Policy to Equipment

**Step 1: Create Policy** (if not already created)

**Step 2: Assign to Equipment**
```bash
POST http://localhost:8081/api/equipment-policies/1/assignments/equipment
Content-Type: application/json

[1, 2]
```

**Expected Response:** `200 OK`
```json
{
  "message": "Equipment assigned successfully",
  "count": 2
}
```

**Get Current Equipment Assignments:**
```bash
GET http://localhost:8081/api/equipment-policies/1/assignments/equipment
```

**Verification:**
```sql
SELECT e.device_name, p.policy_name
FROM equipment e
JOIN policy_equipment_assignments pea ON e.id = pea.equipment_id
JOIN equipment_policies p ON pea.policy_id = p.id
WHERE p.id = 1;
```

### Test Case 5: Assign Policy to Role

**Step 1: Create Policy** (if not already created)

**Step 2: Assign to Role(s)**
```bash
POST http://localhost:8081/api/equipment-policies/1/assignments/roles
Content-Type: application/json

[1]
```

**Expected Response:** `200 OK`
```json
{
  "message": "Roles assigned successfully",
  "count": 1
}
```

**Get Current Role Assignments:**
```bash
GET http://localhost:8081/api/equipment-policies/1/assignments/roles
```

**Verification:**
```sql
SELECT r.name as role_name, p.policy_name
FROM roles r
JOIN policy_role_assignments pra ON r.id = pra.role_id
JOIN equipment_policies p ON pra.policy_id = p.id
WHERE p.id = 1;
```

### Test Case 6: Multiple Assignment Types

**Create a policy and assign it to multiple types:**

```bash
# Step 1: Create policy
POST http://localhost:8081/api/equipment-policies
{
  "policyName": "Multi-Assignment Policy",
  "description": "Assigned to user, group, equipment, and role",
  "policyClassification": "common",
  "policyApplication": "apply",
  "enabled": true,
  "priority": 100
}

# Step 2: Assign to user
POST http://localhost:8081/api/equipment-policies/{id}/assignments/users
[1]

# Step 3: Assign to group
POST http://localhost:8081/api/equipment-policies/{id}/assignments/groups
[1]

# Step 4: Assign to equipment
POST http://localhost:8081/api/equipment-policies/{id}/assignments/equipment
[1]

# Step 5: Assign to role
POST http://localhost:8081/api/equipment-policies/{id}/assignments/roles
[1]
```

**Note:** Policy will apply if user matches ANY assignment type

### Test Case 7: Replace All Assignments

**Replace all user assignments:**
```bash
PUT http://localhost:8081/api/equipment-policies/1/assignments/users
Content-Type: application/json

[2, 3, 4]
```

**Expected Response:** `200 OK`
```json
{
  "message": "User assignments replaced successfully",
  "count": 3
}
```

**Note:** This removes all existing user assignments and replaces them with the new ones.

### Test Case 8: Remove Assignments

**Remove single user:**
```bash
DELETE http://localhost:8081/api/equipment-policies/1/assignments/users/1
```

**Expected Response:** `200 OK`
```json
{
  "message": "User removed successfully",
  "userId": "1"
}
```

**Remove multiple users:**
```bash
DELETE http://localhost:8081/api/equipment-policies/1/assignments/users
Content-Type: application/json

[2, 3]
```

**Expected Response:** `200 OK`
```json
{
  "message": "Users removed successfully",
  "count": 2
}
```

**Similar endpoints for groups, equipment, and roles:**
- `DELETE /api/equipment-policies/{id}/assignments/groups/{groupId}`
- `DELETE /api/equipment-policies/{id}/assignments/equipment/{equipmentId}`
- `DELETE /api/equipment-policies/{id}/assignments/roles/{roleId}`
- `DELETE /api/equipment-policies/{id}/assignments/groups` (bulk)
- `DELETE /api/equipment-policies/{id}/assignments/equipment` (bulk)
- `DELETE /api/equipment-policies/{id}/assignments/roles` (bulk)

---

## ⚙️ Testing Policy Settings (JSONB Verification)

### Test Case 1: Verify Protocol Settings

**Request:**
```bash
GET http://localhost:8081/api/equipment-policies/2
```

**Expected Response:** Should include `policyConfig` JSONB with `commonSettings.allowedProtocols`

**Database Verification:**
```sql
-- Get all protocols from JSONB policy_config
SELECT 
  policy_name,
  jsonb_array_elements_text(policy_config::jsonb->'commonSettings'->'allowedProtocols') as protocol
FROM equipment_policies 
WHERE id = 2;

-- Or get as array
SELECT 
  policy_name,
  policy_config::jsonb->'commonSettings'->>'allowedProtocols' as protocols
FROM equipment_policies 
WHERE id = 2;
```

### Test Case 2: Verify Time Slots

**Database Verification:**
```sql
-- Get all time slots from JSONB policy_config
SELECT 
  policy_name,
  jsonb_array_elements(policy_config::jsonb->'allowedTime'->'timeSlots')->>'dayOfWeek' as day_of_week,
  jsonb_array_elements(policy_config::jsonb->'allowedTime'->'timeSlots')->>'hourStart' as hour_start,
  jsonb_array_elements(policy_config::jsonb->'allowedTime'->'timeSlots')->>'hourEnd' as hour_end
FROM equipment_policies 
WHERE id = 3
ORDER BY day_of_week, hour_start;
```

### Test Case 3: Verify IP Filtering

**Database Verification:**
```sql
-- Get IP filtering settings from JSONB policy_config
SELECT 
  policy_name,
  policy_config::jsonb->'loginControl'->>'ipFilteringType' as ip_filtering_type,
  jsonb_array_elements_text(policy_config::jsonb->'loginControl'->'allowedIps') as ip_address
FROM equipment_policies 
WHERE id = 5;
```

### Test Case 4: Verify Custom Conditions and Metadata

**Database Verification:**
```sql
-- Check customConditions (flexible fields from frontend)
SELECT 
  policy_name,
  policy_config::jsonb->'customConditions' as custom_conditions,
  policy_config::jsonb->'customMetadata' as custom_metadata
FROM equipment_policies 
WHERE policy_config::jsonb->'customConditions' IS NOT NULL;
```

---

## 🔧 Testing Drools Rule Generation

### Test Case 1: Verify Rules Are Generated

**Step 1: Create a Policy**
```bash
POST http://localhost:8081/api/equipment-policies
{
  "policyName": "Test Rule Generation",
  "policyClassification": "common",
  "policyApplication": "apply",
  "enabled": true,
  "priority": 100,
  "commonSettings": {
    "allowedProtocols": ["SSH"]
  },
  "userIds": [1]
}
```

**Step 2: Check Application Logs**

Look for:
```
✓ Rules rebuilt successfully
  Loaded equipment policies from database
```

**Step 3: Verify Rule Content (Database)**

```sql
-- Check if policy exists and has generated DRL
SELECT 
  id, 
  policy_name, 
  enabled, 
  priority,
  generated_rule_drl IS NOT NULL as has_drl,
  policy_config IS NOT NULL as has_config
FROM equipment_policies
WHERE policy_name = 'Test Rule Generation';

-- View generated DRL (if stored)
SELECT generated_rule_drl
FROM equipment_policies
WHERE policy_name = 'Test Rule Generation';
```

### Test Case 2: Test Rule Evaluation Logic

**Create Test Policy:**
```bash
POST http://localhost:8081/api/equipment-policies
{
  "policyName": "SSH Only Policy",
  "policyClassification": "common",
  "policyApplication": "apply",
  "enabled": true,
  "priority": 100,
  "commonSettings": {
    "allowedProtocols": ["SSH"]
  },
  "userIds": [1]
}
```

**Expected Drools Rule (conceptually):**
```drl
rule "SSH Only Policy"
    salience 100
    when
        $request : EquipmentAccessRequest(
            isAssignedToPolicy(1),
            isProtocolAllowed("SSH")
        )
        $result : EquipmentAccessResult(evaluated == false)
    then
        $result.allow("SSH Only Policy");
end
```

### Test Case 3: Test Multiple Policies

**Create Multiple Policies:**
```bash
# Policy 1: High priority, SSH only
POST http://localhost:8081/api/equipment-policies
{
  "policyName": "High Priority SSH",
  "priority": 200,
  "commonSettings": {"allowedProtocols": ["SSH"]},
  "userIds": [1]
}

# Policy 2: Lower priority, Multiple protocols
POST http://localhost:8081/api/equipment-policies
{
  "policyName": "Low Priority Multi-Protocol",
  "priority": 50,
  "commonSettings": {"allowedProtocols": ["SSH", "TELNET", "RDP"]},
  "userIds": [1]
}
```

**Expected Behavior:**
- Higher priority policy (200) evaluated first
- If it matches, lower priority policy (50) may not be evaluated
- Salience determines evaluation order

---

## 🚪 Testing Equipment Access Control

### Test Case 1: Basic Access Check - Allowed

**Request:**
```bash
POST http://localhost:8081/api/equipment-access/check
Content-Type: application/json
X-Username: admin

{
  "username": "admin",
  "equipmentId": 1,
  "protocol": "SSH",
  "clientIp": "192.168.1.100",
  "requestTime": "2024-01-15T14:00:00"
}
```

**Expected Response:** `200 OK`
```json
{
  "allowed": true,
  "matchedPolicyName": "High Priority SSH",
  "denialReason": null,
  "denialCode": null
}
```

**Check Application Logs:**
```
Equipment access rules fired: 1 for user: admin
✓ Equipment access ALLOWED by policy: High Priority SSH
```

### Test Case 2: Basic Access Check - Denied (Protocol Mismatch)

**Request:**
```bash
POST http://localhost:8081/api/equipment-access/check
Content-Type: application/json
X-Username: admin

{
  "username": "admin",
  "equipmentId": 1,
  "protocol": "TELNET",
  "clientIp": "192.168.1.100"
}
```

**Expected Response:** `200 OK`
```json
{
  "allowed": false,
  "matchedPolicyName": null,
  "denialReason": "No matching policy found for this equipment access",
  "denialCode": null
}
```

**Note:** If policy only allows SSH, TELNET will be denied

### Test Case 3: Time-Based Access - Within Allowed Time

**Setup:**
1. Create policy with time restrictions (Monday-Friday, 9-18)
2. Test during allowed time

**Request:**
```bash
POST http://localhost:8081/api/equipment-access/check
Content-Type: application/json
X-Username: admin

{
  "username": "admin",
  "equipmentId": 1,
  "protocol": "SSH",
  "requestTime": "2024-01-15T14:00:00"  # Monday 14:00 (2 PM)
}
```

**Expected Response:** `200 OK`
```json
{
  "allowed": true,
  "matchedPolicyName": "Business Hours Access"
}
```

### Test Case 4: Time-Based Access - Outside Allowed Time

**Request:**
```bash
POST http://localhost:8081/api/equipment-access/check
Content-Type: application/json
X-Username: admin

{
  "username": "admin",
  "equipmentId": 1,
  "protocol": "SSH",
  "requestTime": "2024-01-15T20:00:00"  # Monday 8 PM (outside 9-18)
}
```

**Expected Response:** `200 OK`
```json
{
  "allowed": false,
  "matchedPolicyName": null,
  "denialReason": "No matching policy found for this equipment access"
}
```

### Test Case 5: IP Filtering - Allowed IP

**Setup:**
1. Create policy with IP filtering: `whitelist`
2. Add allowed IP: `192.168.1.100`

**Request:**
```bash
POST http://localhost:8081/api/equipment-access/check
Content-Type: application/json
X-Username: admin

{
  "username": "admin",
  "equipmentId": 1,
  "protocol": "SSH",
  "clientIp": "192.168.1.100"
}
```

**Expected Response:** `200 OK`
```json
{
  "allowed": true,
  "matchedPolicyName": "Restricted IP Access"
}
```

### Test Case 6: IP Filtering - Blocked IP

**Request:**
```bash
POST http://localhost:8081/api/equipment-access/check
Content-Type: application/json
X-Username: admin

{
  "username": "admin",
  "equipmentId": 1,
  "protocol": "SSH",
  "clientIp": "10.0.0.100"  # Not in allowed list
}
```

**Expected Response:** `200 OK`
```json
{
  "allowed": false,
  "matchedPolicyName": null,
  "denialReason": "No matching policy found for this equipment access"
}
```

### Test Case 7: Command Blacklist - Allowed Command

**Setup:**
1. Create command list with blacklist: `["rm -rf", "format"]`
2. Create policy with command settings linking to blacklist

**Request:**
```bash
POST http://localhost:8081/api/equipment-access/check
Content-Type: application/json
X-Username: admin

{
  "username": "admin",
  "equipmentId": 1,
  "protocol": "SSH",
  "command": "ls -la"  # Not in blacklist
}
```

**Expected Response:** `200 OK`
```json
{
  "allowed": true,
  "matchedPolicyName": "Secure Command Policy"
}
```

### Test Case 8: Command Blacklist - Blocked Command

**Request:**
```bash
POST http://localhost:8081/api/equipment-access/check
Content-Type: application/json
X-Username: admin

{
  "username": "admin",
  "equipmentId": 1,
  "protocol": "SSH",
  "command": "rm -rf /"  # In blacklist
}
```

**Expected Response:** `200 OK`
```json
{
  "allowed": false,
  "matchedPolicyName": null,
  "denialReason": "No matching policy found for this equipment access"
}
```

**Note:** Command blacklist check happens in Drools rules

### Test Case 9: Session Limit Check

**Setup:**
1. Create policy with `maxSshSessions: 5`
2. Test with current sessions count

**Request:**
```bash
POST http://localhost:8081/api/equipment-access/check
Content-Type: application/json
X-Username: admin

{
  "username": "admin",
  "equipmentId": 1,
  "protocol": "SSH",
  "currentSshSessions": 4  # Below limit
}
```

**Expected Response:** `200 OK`
```json
{
  "allowed": true
}
```

**Request (Exceeds Limit):**
```bash
POST http://localhost:8081/api/equipment-access/check
Content-Type: application/json
X-Username: admin

{
  "username": "admin",
  "equipmentId": 1,
  "protocol": "SSH",
  "currentSshSessions": 6  # Exceeds limit of 5
}
```

**Expected Response:** `200 OK`
```json
{
  "allowed": false,
  "denialReason": "Maximum session limit exceeded"
}
```

### Test Case 10: Group-Based Access

**Setup:**
1. Create user group "IT Team"
2. Add user to group
3. Assign policy to group

**Request:**
```bash
POST http://localhost:8081/api/equipment-access/check
Content-Type: application/json
X-Username: admin

{
  "username": "admin",
  "equipmentId": 1,
  "protocol": "SSH"
}
```

**Expected Response:** `200 OK`
```json
{
  "allowed": true,
  "matchedPolicyName": "Group Policy"
}
```

**Verification:**
```sql
-- Verify user is in group
SELECT u.username, g.group_name
FROM users u
JOIN user_group_members ugm ON u.id = ugm.user_id
JOIN user_groups g ON ugm.group_id = g.id
WHERE u.username = 'admin';
```

---

## 🔄 Integration Testing Scenarios

### Scenario 1: Complete User Onboarding Flow

**Step 1: Create New User**
```bash
POST http://localhost:8081/api/users
{
  "username": "newemployee",
  "password": "password123",
  "email": "new@example.com",
  "department": "IT",
  "level": 3
}
```

**Step 2: Add User to Group**
```bash
POST http://localhost:8081/api/user-groups/1/members
[<new_user_id>]
```

**Step 3: Verify User Gets Group Policies**
```bash
POST http://localhost:8081/api/equipment-access/check
{
  "username": "newemployee",
  "equipmentId": 1,
  "protocol": "SSH"
}
```

**Expected:** User should have access via group policy

### Scenario 2: Policy Update and Hot Reload

**Step 1: Create Policy**
```bash
POST http://localhost:8081/api/equipment-policies
{
  "policyName": "Test Hot Reload",
  "commonSettings": {"allowedProtocols": ["SSH"]},
  "userIds": [1]
}
```

**Step 2: Verify Access**
```bash
POST http://localhost:8081/api/equipment-access/check
{
  "username": "admin",
  "protocol": "SSH"
}
```
**Expected:** Allowed

**Step 3: Update Policy (Add TELNET)**
```bash
PUT http://localhost:8081/api/equipment-policies/{id}
{
  "commonSettings": {"allowedProtocols": ["SSH", "TELNET"]}
}
```

**Step 4: Verify Updated Access**
```bash
POST http://localhost:8081/api/equipment-access/check
{
  "username": "admin",
  "protocol": "TELNET"
}
```
**Expected:** Allowed (rules should be hot-reloaded)

**Check Logs:**
```
✓ Rules rebuilt successfully
```

### Scenario 3: Multiple Policies with Priority

**Setup:**
1. Policy A: Priority 100, SSH only, assigned to user
2. Policy B: Priority 200, SSH + TELNET, assigned to group (user is member)

**Test:**
```bash
POST http://localhost:8081/api/equipment-access/check
{
  "username": "admin",
  "protocol": "SSH"
}
```

**Expected Behavior:**
- Policy B (priority 200) evaluated first
- If it matches, Policy A may not be evaluated
- Higher priority wins

### Scenario 4: Policy Inheritance

**Step 1: Create Base Policy**
```bash
POST http://localhost:8081/api/equipment-policies
{
  "policyName": "Base Policy Template",
  "commonSettings": {
    "allowedProtocols": ["SSH", "TELNET"],
    "maxSshSessions": 5
  }
}
```

**Step 2: Create Inherited Policy**
```bash
POST http://localhost:8081/api/equipment-policies
{
  "policyName": "Derived Policy",
  "equipmentBasicPolicyId": <base_policy_id>,
  "commonSettings": {
    "allowedProtocols": ["SSH"],  # Override: only SSH
    "maxSshSessions": 10  # Override: increase limit
  }
}
```

**Expected:** Inherited policy uses base policy as template, overrides specific settings

---

## 📊 Performance Testing

### Test Case 1: Load Multiple Policies

**Create 50 Policies:**
```bash
# Use a script to create multiple policies
for i in {1..50}; do
  curl -X POST http://localhost:8081/api/equipment-policies \
    -H "Content-Type: application/json" \
    -d "{
      \"policyName\": \"Policy $i\",
      \"policyClassification\": \"common\",
      \"policyApplication\": \"apply\",
      \"enabled\": true,
      \"priority\": $i,
      \"commonSettings\": {
        \"allowedProtocols\": [\"SSH\"]
      }
    }"
done
```

**Measure:**
- Time to create policies
- Time to rebuild rules
- Memory usage
- Rule evaluation time

### Test Case 2: Concurrent Access Checks

**Test with multiple simultaneous requests:**
```bash
# Use Apache Bench or similar
ab -n 1000 -c 10 -p access_request.json -T application/json \
  http://localhost:8081/api/equipment-access/check
```

**Monitor:**
- Response times
- Throughput (requests/second)
- Error rate
- Database connection pool usage

### Test Case 3: Large Policy Assignment

**Test with policy assigned to many users:**
```bash
# Assign policy to 100 users
POST http://localhost:8081/api/equipment-policies
{
  "policyName": "Large Assignment Policy",
  "userIds": [1, 2, 3, ..., 100]  # Array of 100 user IDs
}
```

**Measure:**
- Time to create assignment
- Time to load policies for fact
- Memory usage

---

## 🐛 Troubleshooting

### Issue 1: Policy Created but Returns 500 Error

**Symptoms:**
- Policy is inserted into database
- API returns 500 Internal Server Error
- Check application logs for stack trace

**Possible Causes:**
1. Invalid JSONB structure in `policy_config`
2. Transaction rollback issue
3. Drools rule generation error
4. JSON parsing error in PolicyConfigCache

**Solution:**
1. Check application logs for detailed error
2. Verify `policy_config` JSONB is valid JSON
3. Check database for policy existence
4. Verify Drools rule compilation

**Debug Query:**
```sql
-- Check if policy has valid JSONB config
SELECT 
  id,
  policy_name,
  CASE 
    WHEN policy_config IS NULL THEN 'Missing Config'
    WHEN policy_config::text = 'null' THEN 'Null Config'
    ELSE 'OK'
  END as config_status,
  policy_config IS NOT NULL as has_config,
  jsonb_typeof(policy_config::jsonb) as config_type
FROM equipment_policies
WHERE id = <policy_id>;

-- Validate JSONB structure
SELECT 
  policy_name,
  policy_config::jsonb->'commonSettings' IS NOT NULL as has_common_settings,
  policy_config::jsonb->'allowedTime' IS NOT NULL as has_allowed_time,
  policy_config::jsonb->'loginControl' IS NOT NULL as has_login_control,
  policy_config::jsonb->'commandSettings' IS NOT NULL as has_command_settings
FROM equipment_policies
WHERE id = <policy_id>;
```

### Issue 2: Group Deletion Fails with Members

**Symptoms:**
- Cannot delete group that has members
- Returns 500 error

**Possible Causes:**
- Foreign key constraint violation
- Cascade delete not configured properly

**Solution:**
1. Remove all members first:
   ```bash
   DELETE http://localhost:8081/api/user-groups/{id}/members
   [<all_member_ids>]
   ```
2. Then delete group
3. Or update entity to handle cascade properly

**Database Check:**
```sql
-- Check if group has members
SELECT COUNT(*) as member_count
FROM user_group_members
WHERE group_id = <group_id>;
```

### Issue 3: Access Always Denied

**Symptoms:**
- All access checks return `allowed: false`
- "No matching policy found"

**Possible Causes:**
1. No policies assigned to user/group/equipment/role
2. Policy is disabled
3. Policy application is "not_applicable"
4. Drools rules not loaded

**Solution:**
1. Verify policy assignments:
   ```sql
   -- Check user assignments
   SELECT p.* FROM equipment_policies p
   JOIN policy_user_assignments pua ON p.id = pua.policy_id
   WHERE pua.user_id = <user_id> AND p.enabled = true;
   ```

2. Check policy status:
   ```sql
   SELECT id, policy_name, enabled, policy_application
   FROM equipment_policies
   WHERE id = <policy_id>;
   ```

3. Verify Drools rules loaded:
   - Check application startup logs
   - Look for "Rules rebuilt successfully"
   - Check for rule compilation errors

### Issue 4: Time-Based Access Not Working

**Symptoms:**
- Time slots configured but not enforced
- Access allowed outside time windows

**Possible Causes:**
1. Time slots not loaded into fact
2. Current time not set in request
3. Time zone mismatch

**Solution:**
1. Verify time slots in JSONB policy_config:
   ```sql
   SELECT 
     policy_name,
     jsonb_array_elements(policy_config::jsonb->'allowedTime'->'timeSlots') as time_slot
   FROM equipment_policies
   WHERE id = <policy_id>;
   ```

2. Ensure requestTime is set:
   ```json
   {
     "requestTime": "2024-01-15T14:00:00"  // Must be set
   }
   ```

3. Check time zone in JSONB:
   ```sql
   SELECT 
     policy_name,
     policy_config::jsonb->'allowedTime'->>'timeZone' as time_zone
   FROM equipment_policies 
   WHERE id = <policy_id>;
   ```

### Issue 5: Command Blacklist Not Working

**Symptoms:**
- Blacklisted commands still allowed

**Possible Causes:**
1. Command list not linked to policy
2. Command list items not loaded
3. Command matching logic issue

**Solution:**
1. Verify command settings in JSONB policy_config:
   ```sql
   SELECT 
     policy_name,
     jsonb_array_elements(policy_config::jsonb->'commandSettings') as command_setting
   FROM equipment_policies
   WHERE id = <policy_id>;
   
   -- Extract command list IDs
   SELECT 
     policy_name,
     jsonb_array_elements(policy_config::jsonb->'commandSettings')->>'commandListIds' as command_list_ids
   FROM equipment_policies
   WHERE id = <policy_id>;
   ```

2. Check command list items (still normalized):
   ```sql
   SELECT command_text FROM command_list_items
   WHERE command_list_id = <command_list_id>;
   ```

3. Verify command is passed in request:
   ```json
   {
     "command": "rm -rf"  // Must be set for command check
   }
   ```

### Issue 6: IP Filtering Not Working

**Symptoms:**
- IP filtering configured but not enforced

**Possible Causes:**
1. clientIp not set in request
2. IP filtering type is "no_restrictions"
3. IP not in allowed list

**Solution:**
1. Verify IP filtering type in JSONB:
   ```sql
   SELECT 
     policy_name,
     policy_config::jsonb->'loginControl'->>'ipFilteringType' as ip_filtering_type
   FROM equipment_policies
   WHERE id = <policy_id>;
   ```

2. Check allowed IPs in JSONB:
   ```sql
   SELECT 
     policy_name,
     jsonb_array_elements_text(policy_config::jsonb->'loginControl'->'allowedIps') as ip_address
   FROM equipment_policies
   WHERE id = <policy_id>;
   ```

3. Ensure clientIp is set:
   ```json
   {
     "clientIp": "192.168.1.100"  // Must be set
   }
   ```

### Issue 7: Drools Rules Not Rebuilding

**Symptoms:**
- Policy created but rules not updated
- Old rules still in effect

**Possible Causes:**
1. rebuildRules() not called
2. Rule compilation errors
3. KieContainer not updated

**Solution:**
1. Check application logs for:
   - "Rules rebuilt successfully"
   - Rule compilation errors

2. Manually trigger rebuild (if endpoint exists):
   ```bash
   POST http://localhost:8081/api/admin/rebuild-rules
   ```

3. Verify rule generation:
   - Check EquipmentPolicyRuleGenerator service
   - Verify policies are loaded from database
   - Check DRL syntax

### Issue 8: Performance Issues

**Symptoms:**
- Slow access checks
- High memory usage
- Database connection timeouts

**Possible Causes:**
1. Too many policies loaded
2. N+1 query problem
3. Missing indexes

**Solution:**
1. Check database indexes:
   ```sql
   SELECT indexname, indexdef
   FROM pg_indexes
   WHERE tablename LIKE 'policy%';
   ```

2. Optimize queries:
   - Use JOIN FETCH in repositories
   - Add missing indexes
   - Limit policies loaded (only enabled)

3. Monitor performance:
   - Enable SQL logging
   - Use JProfiler or similar
   - Check Drools session creation time

---

## ✅ Testing Checklist

Use this checklist to ensure comprehensive testing:

### User Groups
- [ ] Create group
- [ ] Add members to group
- [ ] Remove members from group
- [ ] Update group
- [ ] Delete group (with and without members)
- [ ] Get group members
- [ ] List all groups

### Equipment Policies
- [ ] Create basic policy
- [ ] Create policy with common settings
- [ ] Create policy with time restrictions
- [ ] Create policy with command control
- [ ] Create policy with login control
- [ ] Create complete policy (all settings)
- [ ] Update policy
- [ ] Toggle policy (enable/disable)
- [ ] Delete policy
- [ ] Get all policies
- [ ] Get enabled policies
- [ ] Get policy by ID

### Policy Assignments
- [ ] Create policy (without assignments)
- [ ] Assign policy to user (POST /assignments/users)
- [ ] Assign policy to group (POST /assignments/groups)
- [ ] Assign policy to equipment (POST /assignments/equipment)
- [ ] Assign policy to role (POST /assignments/roles)
- [ ] Get user assignments (GET /assignments/users)
- [ ] Get group assignments (GET /assignments/groups)
- [ ] Get equipment assignments (GET /assignments/equipment)
- [ ] Get role assignments (GET /assignments/roles)
- [ ] Replace all user assignments (PUT /assignments/users)
- [ ] Remove single user assignment (DELETE /assignments/users/{userId})
- [ ] Remove multiple user assignments (DELETE /assignments/users)
- [ ] Multiple assignment types (assign to all types)
- [ ] Verify assignments in database

### Policy Settings
- [ ] Verify protocol settings
- [ ] Verify DBMS settings
- [ ] Verify time slots
- [ ] Verify IP filtering
- [ ] Verify command lists
- [ ] Verify session limits

### Drools Integration
- [ ] Rules generated on policy create
- [ ] Rules regenerated on policy update
- [ ] Rules regenerated on policy delete
- [ ] Rules regenerated on policy toggle
- [ ] Rule compilation successful
- [ ] Rules loaded into KieContainer

### Equipment Access Control
- [ ] Access allowed (protocol match)
- [ ] Access denied (protocol mismatch)
- [ ] Time-based access (within hours)
- [ ] Time-based access (outside hours)
- [ ] IP filtering (allowed IP)
- [ ] IP filtering (blocked IP)
- [ ] Command blacklist (allowed)
- [ ] Command blacklist (blocked)
- [ ] Session limit (within limit)
- [ ] Session limit (exceeded)
- [ ] Group-based access
- [ ] Role-based access
- [ ] Equipment-based access
- [ ] Multiple policies (priority)

### Integration Scenarios
- [ ] User onboarding flow
- [ ] Policy update and hot reload
- [ ] Multiple policies with priority
- [ ] Policy inheritance
- [ ] Concurrent access checks

### Performance
- [ ] Load multiple policies (50+)
- [ ] Concurrent requests (100+)
- [ ] Large assignments (100+ users)
- [ ] Response time < 100ms
- [ ] Memory usage acceptable

---

## 📝 Testing Best Practices

1. **Start with Simple Cases**
   - Test basic policy creation first
   - Add complexity gradually
   - Verify each step before moving on

2. **Verify Database State**
   - Check data after each operation
   - Use SQL queries to verify relationships
   - Ensure foreign keys are correct

3. **Check Application Logs**
   - Monitor Drools rule generation
   - Watch for errors or warnings
   - Verify rule compilation success

4. **Test Both Positive and Negative Cases**
   - Test allowed scenarios
   - Test denied scenarios
   - Test edge cases

5. **Use Swagger UI**
   - Interactive testing is easier
   - Can see request/response clearly
   - Good for exploring API

6. **Clean Up Test Data**
   - Delete test policies after testing
   - Remove test groups
   - Keep database clean

7. **Document Issues**
   - Note any errors encountered
   - Document workarounds
   - Update this guide with solutions

---

## 🔗 Related Documentation

- [README.md](README.md) - Project overview
- [new-process.md](new-process.md) - System architecture and flow
- [TESTING-FLOW.md](TESTING-FLOW.md) - Original API endpoint testing
- [ENDPOINT-FLOW-EXAMPLES.md](ENDPOINT-FLOW-EXAMPLES.md) - Detailed endpoint flows

---

## 📊 Example Test Data

### Sample Policies for Testing

```json
// Policy 1: Basic SSH Access
// Step 1: Create policy
{
  "policyName": "Basic SSH Access",
  "policyClassification": "common",
  "policyApplication": "apply",
  "enabled": true,
  "priority": 100,
  "commonSettings": {
    "allowedProtocols": ["SSH"]
  }
}

// Step 2: Assign to user (separate endpoint)
// POST /api/equipment-policies/{id}/assignments/users
[1]

// Policy 2: Business Hours Only
{
  "policyName": "Business Hours Access",
  "policyClassification": "common",
  "policyApplication": "apply",
  "enabled": true,
  "priority": 100,
  "commonSettings": {
    "allowedProtocols": ["SSH", "TELNET", "RDP"]
  },
  "allowedTime": {
    "startDate": "2024-01-01",
    "endDate": "2024-12-31",
    "borderless": false,
    "timeSlots": [
      {"dayOfWeek": 1, "hourStart": 9, "hourEnd": 18},
      {"dayOfWeek": 2, "hourStart": 9, "hourEnd": 18},
      {"dayOfWeek": 3, "hourStart": 9, "hourEnd": 18},
      {"dayOfWeek": 4, "hourStart": 9, "hourEnd": 18},
      {"dayOfWeek": 5, "hourStart": 9, "hourEnd": 18}
    ]
  },
  "groupIds": [1]
}

// Policy 3: IP Restricted
{
  "policyName": "Office Network Only",
  "policyClassification": "common",
  "policyApplication": "apply",
  "enabled": true,
  "priority": 150,
  "commonSettings": {
    "allowedProtocols": ["SSH", "RDP"]
  },
  "loginControl": {
    "ipFilteringType": "whitelist",
    "allowedIps": ["192.168.1.0/24"],
    "accountLockEnabled": false,
    "maxFailureAttempts": 0,
    "lockoutDurationMinutes": 0,
    "twoFactorType": "none"
  },
  "customConditions": {
    "deviceType": {
      "operator": "equals",
      "value": "LINUX_SERVER"
    }
  },
  "customMetadata": {
    "createdBy": "admin",
    "department": "IT"
  },
  "roleIds": [1]
}
```

---

## 🎓 Quick Reference

### Common API Endpoints

```
# User Groups
GET    /api/user-groups
POST   /api/user-groups
GET    /api/user-groups/{id}
PUT    /api/user-groups/{id}
DELETE /api/user-groups/{id}
POST   /api/user-groups/{id}/members
DELETE /api/user-groups/{id}/members
GET    /api/user-groups/{id}/members

# Equipment Policies
GET    /api/equipment-policies
POST   /api/equipment-policies
GET    /api/equipment-policies/{id}
PUT    /api/equipment-policies/{id}
DELETE /api/equipment-policies/{id}
PATCH  /api/equipment-policies/{id}/toggle
GET    /api/equipment-policies/enabled

# Equipment Access
POST   /api/equipment-access/check
```

### Common Database Queries (JSONB Implementation)

```sql
-- Find policies for a user
SELECT p.* FROM equipment_policies p
JOIN policy_user_assignments pua ON p.id = pua.policy_id
WHERE pua.user_id = 1 AND p.enabled = true;

-- Find policies for a group
SELECT p.* FROM equipment_policies p
JOIN policy_group_assignments pga ON p.id = pga.policy_id
WHERE pga.group_id = 1 AND p.enabled = true;

-- Get all protocols for a policy (from JSONB)
SELECT 
  policy_name,
  jsonb_array_elements_text(policy_config::jsonb->'commonSettings'->'allowedProtocols') as protocol
FROM equipment_policies
WHERE id = 1;

-- Get time slots for a policy (from JSONB)
SELECT 
  policy_name,
  jsonb_array_elements(policy_config::jsonb->'allowedTime'->'timeSlots')->>'dayOfWeek' as day_of_week,
  jsonb_array_elements(policy_config::jsonb->'allowedTime'->'timeSlots')->>'hourStart' as hour_start,
  jsonb_array_elements(policy_config::jsonb->'allowedTime'->'timeSlots')->>'hourEnd' as hour_end
FROM equipment_policies
WHERE id = 1;

-- View full policy_config JSONB
SELECT 
  policy_name,
  policy_config::jsonb
FROM equipment_policies
WHERE id = 1;

-- Check customConditions and customMetadata
SELECT 
  policy_name,
  policy_config::jsonb->'customConditions' as custom_conditions,
  policy_config::jsonb->'customMetadata' as custom_metadata
FROM equipment_policies
WHERE policy_config::jsonb->'customConditions' IS NOT NULL;
```

---

**Last Updated:** 2026-01-14
**Version:** 2.2.0 (RESTful Assignment Management)
**Author:** Testing Team

**Implementation Notes:**
- Equipment policies use JSONB `policy_config` field (no normalized tables)
- Policy configuration is cached using Spring Cache with Caffeine (30-minute TTL)
- Supports flexible `customConditions` and `customMetadata` for frontend customization
- All policy settings consolidated into single JSONB column for better flexibility and performance
- **NEW:** Policy assignments managed via separate RESTful endpoints for better separation of concerns
- **NEW:** Assignment endpoints support GET, POST, PUT, DELETE operations for each assignment type
- **NEW:** Create policy first, then assign separately using `/api/equipment-policies/{id}/assignments/*` endpoints