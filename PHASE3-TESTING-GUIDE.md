# Phase 3 Testing Guide: UserType & AccountType Policy Assignments

## Overview
Phase 3 introduces policy assignments based on **UserType** and **AccountType**. This enables:
- **UserType Policies**: Automatically apply policies to all users of a specific type (e.g., SUPER_ADMIN, NORMAL_USER)
- **AccountType Policies**: Automatically apply policies when accessing accounts of a specific type (e.g., PRIVILEGED, SERVICE)

## New Features

### 1. UserType-Based Policy Assignment
- Assign a policy to a UserType (e.g., SUPER_ADMIN)
- All users with that UserType automatically inherit the policy
- Example: "Super Admin Enhanced Login" policy → assigned to SUPER_ADMIN type → admin user inherits it

### 2. AccountType-Based Policy Assignment
- Assign a policy to an AccountType (e.g., PRIVILEGED)
- When accessing equipment with accounts of that type, the policy applies
- Example: "Privileged Account Strict Session" policy → assigned to PRIVILEGED type → applies when accessing root account

## Setup

### Step 1: Run Database Migration
```bash
# Apply the Phase 3 migration
psql -U your_user -d your_db -f src/main/resources/db/migration/V20260123__add_policy_type_assignments.sql
```

Or if using Flyway, it will run automatically on application startup.

### Step 2: Run Initial Data (Optional - for demo data)
```bash
psql -U your_user -d your_db -f src/main/resources/sql/initial_data_sql_v2.sql
```

### Step 3: Start the Application
```bash
./gradlew bootRun
```

## Testing API Endpoints

### Test UserType Assignments

#### Get User Types
```bash
curl -X GET http://localhost:8081/api/user-types -H "X-User-Id: admin"
```

#### Assign Policy to UserType
```bash
# Get policy ID for "Default Session Timeout"
curl -X GET http://localhost:8081/api/equipment-policies -H "X-User-Id: admin"

# Assign to SUPER_ADMIN user type (ID: 1)
curl -X POST http://localhost:8081/api/equipment-policies/2/assignments/user-types \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin" \
  -d '[1]'
```

#### Get UserType Assignments for a Policy
```bash
curl -X GET http://localhost:8081/api/equipment-policies/2/assignments/user-types \
  -H "X-User-Id: admin"
```

#### Remove UserType Assignment
```bash
curl -X DELETE http://localhost:8081/api/equipment-policies/2/assignments/user-types/1 \
  -H "X-User-Id: admin"
```

### Test AccountType Assignments

#### Get Account Types
```bash
curl -X GET http://localhost:8081/api/account-types -H "X-User-Id: admin"
```

#### Assign Policy to AccountType
```bash
# Assign "Privileged Account Strict Session" to PRIVILEGED account type
curl -X POST http://localhost:8081/api/equipment-policies/6/assignments/account-types \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin" \
  -d '[1]'
```

#### Get AccountType Assignments for a Policy
```bash
curl -X GET http://localhost:8081/api/equipment-policies/6/assignments/account-types \
  -H "X-User-Id: admin"
```

### Test Policy Resolution with UserType

#### SSH Access as Admin (SUPER_ADMIN)
```bash
curl -X GET "http://localhost:8081/api/equipment-access/ssh?username=admin&equipmentId=1&clientIp=192.168.1.100" \
  -H "X-User-Id: admin"
```

**Expected Console Output:**
```
User Type: SUPER_ADMIN
Policies assigned to user's UserType (SUPER_ADMIN): [Super Admin Enhanced Login, ...]
```

#### SSH Access as John (NORMAL_USER)
```bash
curl -X GET "http://localhost:8081/api/equipment-access/ssh?username=john&equipmentId=1&clientIp=192.168.1.100" \
  -H "X-User-Id: admin"
```

**Expected Console Output:**
```
User Type: NORMAL_USER
Policies assigned to user's UserType (NORMAL_USER): [Normal User Standard Login, ...]
```

### Test Policy Resolution with AccountType

#### Access Equipment with Privileged Account
```bash
# Equipment ID 1 has 'root' account which is PRIVILEGED type
curl -X GET "http://localhost:8081/api/equipment-access/ssh?username=admin&equipmentId=1&clientIp=192.168.1.100" \
  -H "X-User-Id: admin"
```

**Expected Console Output:**
```
Equipment Account Types: [PRIVILEGED, ...]
Policies assigned to AccountType (PRIVILEGED): [Privileged Account Strict Session, ...]
```

## Verify in Database

### Check UserType Assignments
```sql
SELECT ep.policy_name, ut.type_code, ut.type_name
FROM policy_user_type_assignments puta
JOIN equipment_policies ep ON puta.policy_id = ep.id
JOIN user_types ut ON puta.user_type_id = ut.id;
```

### Check AccountType Assignments
```sql
SELECT ep.policy_name, at.type_code, at.type_name
FROM policy_account_type_assignments pata
JOIN equipment_policies ep ON pata.policy_id = ep.id
JOIN account_types at ON pata.account_type_id = at.id;
```

### Check User's Effective Policies
```sql
-- User's direct policies + UserType policies
SELECT DISTINCT ep.id, ep.policy_name, 'user' as assignment_source
FROM equipment_policies ep
JOIN policy_user_assignments pua ON ep.id = pua.policy_id
JOIN users u ON pua.user_id = u.id
WHERE u.username = 'admin'

UNION ALL

SELECT DISTINCT ep.id, ep.policy_name, 'user_type' as assignment_source
FROM equipment_policies ep
JOIN policy_user_type_assignments puta ON ep.id = puta.policy_id
JOIN user_types ut ON puta.user_type_id = ut.id
JOIN users u ON u.user_type_id = ut.id
WHERE u.username = 'admin';
```

## Policy Priority Hierarchy

When multiple policies apply, they are evaluated in this order:

1. **User-specific policies** (highest priority)
2. **UserType policies** (applies to all users of that type)
3. **Role policies** (applies to all users with that role)
4. **Group policies** (applies to all users in that group)
5. **WorkGroup policies** (applies within work group context)
6. **AccountType policies** (applies based on target account type)
7. **Equipment policies** (lowest priority)

Within each level, the `priority` field determines order (higher = more important).

## Sample Policies Created

| Policy Name | Type | Assigned To | Description |
|-------------|------|-------------|-------------|
| Super Admin Enhanced Login | loginControl | SUPER_ADMIN (UserType) | MFA with OTP, 3 attempts, 30min lockout |
| Normal User Standard Login | loginControl | NORMAL_USER (UserType) | MFA with Email, 5 attempts, 15min lockout |
| Privileged Account Strict Session | sessionTimeout | PRIVILEGED (AccountType) | 30min timeout, 10min idle |

## Troubleshooting

### Policies not loading for UserType
1. Check user has `user_type_id` set:
   ```sql
   SELECT username, user_type_id FROM users WHERE username = 'admin';
   ```
2. Check policy assignment exists:
   ```sql
   SELECT * FROM policy_user_type_assignments WHERE user_type_id = 1;
   ```

### Policies not loading for AccountType
1. Check equipment has accounts with account_type_id:
   ```sql
   SELECT a.account_name, at.type_code 
   FROM accounts a 
   JOIN account_types at ON a.account_type_id = at.id 
   WHERE a.equipment_id = 1;
   ```
2. Check policy assignment exists:
   ```sql
   SELECT * FROM policy_account_type_assignments WHERE account_type_id = 1;
   ```

## Next Steps (Phase 4)
- Login method policy enforcement at authentication time
- MFA integration (OTP, Email, SMS)
- Account lockout implementation
- Session management enforcement
