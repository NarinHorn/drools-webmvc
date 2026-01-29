# Phase 2 Testing Guide: Work Groups

## Overview

Phase 2 introduces the **WorkGroup** concept - a project/workspace that groups:
- **Users** - Who can access resources in this work group
- **Equipment** - What devices are available in this work group
- **Accounts** - What accounts on equipment are available
- **Policies** - What policies apply to all members (policy catalog)

### Key Concept
When checking access, the system now considers:
1. User must be a member of the work group
2. Equipment must be a member of the same work group
3. Policies from the work group's catalog are applied

This allows you to create isolated workspaces where specific users can only access specific equipment with specific policies.

---

## Prerequisites

### 1. Run Database Migration
Execute the Phase 2 migration SQL:
```sql
-- Run this file
src/main/resources/db/migration/V20260122__add_work_groups.sql
```

### 2. Run Initial Data SQL
Execute the updated initial data SQL:
```sql
src/main/resources/sql/initial_data_sql_v2.sql
```

This will create:
- 3 Work Groups: Web Development Team, Database Administration, IT Operations
- User memberships for each work group
- Equipment memberships for each work group
- Account memberships
- Policy catalog for each work group

### 3. Start the Application
```bash
./gradlew bootRun
```

---

## Test Case 1: Work Group CRUD

### 1.1 Get All Work Groups
**Request:**
```http
GET http://localhost:8081/api/work-groups
X-Username: admin
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "workGroupName": "Web Development Team",
    "description": "Web development project workspace with Linux servers",
    "enabled": true,
    "userCount": 2,
    "equipmentCount": 1,
    "accountCount": 3,
    "policyCount": 3,
    "userIds": [1, 5],
    "equipmentIds": [1],
    "accountIds": [1, 2, 3],
    "policyIds": [1, 2, 3]
  },
  ...
]
```

### 1.2 Get Work Group by ID
**Request:**
```http
GET http://localhost:8081/api/work-groups/1
X-Username: admin
```

### 1.3 Create New Work Group
**Request:**
```http
POST http://localhost:8081/api/work-groups
Content-Type: application/json
X-Username: admin

{
  "workGroupName": "Test Project",
  "description": "Test project workspace",
  "enabled": true
}
```

**Expected:** 201 Created with work group details

### 1.4 Update Work Group
**Request:**
```http
PUT http://localhost:8081/api/work-groups/1
Content-Type: application/json
X-Username: admin

{
  "workGroupName": "Web Development Team",
  "description": "Updated description",
  "enabled": true
}
```

### 1.5 Delete Work Group
**Request:**
```http
DELETE http://localhost:8081/api/work-groups/{id}
X-Username: admin
```

---

## Test Case 2: Work Group Membership Management

### 2.1 Get Users in Work Group
**Request:**
```http
GET http://localhost:8081/api/work-groups/1/users
X-Username: admin
```

**Expected:** List of users in the work group

### 2.2 Add Users to Work Group
**Request:**
```http
POST http://localhost:8081/api/work-groups/1/users
Content-Type: application/json
X-Username: admin

[3, 4]
```

**Expected:** `{"message": "Users added to work group successfully", "count": "2"}`

### 2.3 Remove Users from Work Group
**Request:**
```http
DELETE http://localhost:8081/api/work-groups/1/users
Content-Type: application/json
X-Username: admin

[3, 4]
```

### 2.4 Get Equipment in Work Group
**Request:**
```http
GET http://localhost:8081/api/work-groups/1/equipment
X-Username: admin
```

### 2.5 Add Equipment to Work Group
**Request:**
```http
POST http://localhost:8081/api/work-groups/1/equipment
Content-Type: application/json
X-Username: admin

[2, 3]
```

### 2.6 Remove Equipment from Work Group
**Request:**
```http
DELETE http://localhost:8081/api/work-groups/1/equipment
Content-Type: application/json
X-Username: admin

[2, 3]
```

---

## Test Case 3: Work Group Policy Catalog

### 3.1 Get Policies in Work Group
**Request:**
```http
GET http://localhost:8081/api/work-groups/1/policies
X-Username: admin
```

**Expected:** List of policies in the work group's catalog

### 3.2 Add Policies to Work Group
**Request:**
```http
POST http://localhost:8081/api/work-groups/1/policies
Content-Type: application/json
X-Username: admin

[1, 2, 3]
```

### 3.3 Remove Policies from Work Group
**Request:**
```http
DELETE http://localhost:8081/api/work-groups/1/policies
Content-Type: application/json
X-Username: admin

[3]
```

---

## Test Case 4: Access Check with Work Groups

This is the key test - verifying that work group policies are applied during access checks.

### 4.1 SSH Access - User in Work Group with Equipment
**Request:**
```http
GET http://localhost:8081/api/equipment-access/ssh?username=bob&equipmentId=1&clientIp=192.168.1.100
X-Username: admin
```

**Check Application Logs for:**
```
Work Groups containing both user and equipment: Web Development Team(ID:1), IT Operations(ID:3)
Policies from WorkGroup 'Web Development Team': Default Equipment Login Method(ID:1), Default Session Timeout(ID:2), Default Concurrent Session Limit(ID:3)
Policies from WorkGroup 'IT Operations': Default Equipment Login Method(ID:1), Default Session Timeout(ID:2), Default Concurrent Session Limit(ID:3)
```

**Expected:** Access allowed (bob is in Web Development Team, which includes Linux Production Server with policies)

### 4.2 SSH Access - User NOT in Work Group with Equipment
**Request:**
```http
GET http://localhost:8081/api/equipment-access/ssh?username=john&equipmentId=1&clientIp=192.168.1.100
X-Username: admin
```

**Check Application Logs for:**
```
No work groups contain both user 'john' and equipment ID 1
```

**Expected:** Access may be denied (john is not in any work group with Linux Production Server)

### 4.3 Database Access - User in Database Administration Work Group
**Request:**
```http
GET http://localhost:8081/api/equipment-access/ssh?username=alice&equipmentId=3&clientIp=192.168.1.100
X-Username: admin
```

**Check Application Logs for:**
```
Work Groups containing both user and equipment: Database Administration(ID:2)
Policies from WorkGroup 'Database Administration': ...
```

**Expected:** Access allowed (alice is in Database Administration, which includes PostgreSQL Database)

---

## Test Case 5: Database Verification

### 5.1 Verify Work Groups Table
```sql
SELECT * FROM work_groups;
```

**Expected:** 3 work groups (Web Development Team, Database Administration, IT Operations)

### 5.2 Verify Work Group Users
```sql
SELECT wg.work_group_name, u.username
FROM work_group_users wgu
JOIN work_groups wg ON wgu.work_group_id = wg.id
JOIN users u ON wgu.user_id = u.id
ORDER BY wg.work_group_name, u.username;
```

**Expected:**
| work_group_name | username |
|-----------------|----------|
| Database Administration | admin |
| Database Administration | alice |
| IT Operations | admin |
| IT Operations | bob |
| IT Operations | manager |
| Web Development Team | admin |
| Web Development Team | bob |

### 5.3 Verify Work Group Equipment
```sql
SELECT wg.work_group_name, e.device_name
FROM work_group_equipment wge
JOIN work_groups wg ON wge.work_group_id = wg.id
JOIN equipment e ON wge.equipment_id = e.id
ORDER BY wg.work_group_name, e.device_name;
```

### 5.4 Verify Work Group Policies
```sql
SELECT wg.work_group_name, ep.policy_name
FROM work_group_policies wgp
JOIN work_groups wg ON wgp.work_group_id = wg.id
JOIN equipment_policies ep ON wgp.policy_id = ep.id
ORDER BY wg.work_group_name, ep.policy_name;
```

---

## Troubleshooting

### Issue: Work Group Policies Not Loading
**Solution:**
1. Verify user is in the work group: `SELECT * FROM work_group_users WHERE user_id = X`
2. Verify equipment is in the work group: `SELECT * FROM work_group_equipment WHERE equipment_id = Y`
3. Verify work group has policies: `SELECT * FROM work_group_policies WHERE work_group_id = Z`
4. Check that work group is enabled: `SELECT enabled FROM work_groups WHERE id = Z`

### Issue: Access Denied Despite Work Group Membership
**Solution:**
1. Check logs for "Work Groups containing both user and equipment"
2. Ensure BOTH user AND equipment are in the SAME work group
3. Verify policies in work group catalog are enabled
4. Check that policy_application = 'apply' for the policies

### Issue: Work Group Not Found
**Solution:**
1. Verify migration SQL executed: `SELECT * FROM work_groups`
2. Verify initial data SQL executed
3. Restart application to reload Spring context

---

## API Endpoints Summary

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/work-groups` | GET | Get all work groups |
| `/api/work-groups/active` | GET | Get active work groups |
| `/api/work-groups/{id}` | GET | Get work group by ID |
| `/api/work-groups` | POST | Create work group |
| `/api/work-groups/{id}` | PUT | Update work group |
| `/api/work-groups/{id}` | DELETE | Delete work group |
| `/api/work-groups/{id}/users` | GET | Get users in work group |
| `/api/work-groups/{id}/users` | POST | Add users to work group |
| `/api/work-groups/{id}/users` | DELETE | Remove users from work group |
| `/api/work-groups/{id}/equipment` | GET | Get equipment in work group |
| `/api/work-groups/{id}/equipment` | POST | Add equipment to work group |
| `/api/work-groups/{id}/equipment` | DELETE | Remove equipment from work group |
| `/api/work-groups/{id}/accounts` | GET | Get accounts in work group |
| `/api/work-groups/{id}/accounts` | POST | Add accounts to work group |
| `/api/work-groups/{id}/accounts` | DELETE | Remove accounts from work group |
| `/api/work-groups/{id}/policies` | GET | Get policies in work group |
| `/api/work-groups/{id}/policies` | POST | Add policies to work group |
| `/api/work-groups/{id}/policies` | DELETE | Remove policies from work group |

---

## Summary Checklist

- [ ] Database migration executed successfully
- [ ] Initial data SQL executed successfully
- [ ] Application starts without errors
- [ ] Work groups endpoint returns 3 groups
- [ ] Work group CRUD operations work
- [ ] User membership management works
- [ ] Equipment membership management works
- [ ] Account membership management works
- [ ] Policy catalog management works
- [ ] SSH access check includes work group policies in logs
- [ ] Access is granted when user AND equipment are in same work group with policies
- [ ] Access behavior differs when user is NOT in work group with equipment

---

## Next Steps (Phase 3)

After Phase 2 testing is complete, Phase 3 could add:
- Policy assignments by UserType
- Policy assignments by AccountType
- Enhanced login method policies with MFA configuration
- Device category-based policies
