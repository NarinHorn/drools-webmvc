# Phase 1 Testing Guide: User Types and Accounts

## Overview
Phase 1 introduces three new core entities:
1. **UserType** - Categorizes users (SUPER_ADMIN, MIDDLE_MANAGER, REGULAR_USER, OCCASIONAL_USER)
2. **AccountType** - Categorizes accounts (COLLECTION, PRIVILEGED, PERSONAL, SOLUTION, PUBLIC)
3. **Account** - Represents accounts linked to equipment (e.g., web_root, web_user on Linux servers)

## Prerequisites

### 1. Run Database Migration
Execute the migration SQL to create new tables:

```sql
-- Run this file first
src/main/resources/db/migration/V20260121__add_user_types_accounts.sql
```

### 2. Run Initial Data SQL
Execute the updated initial data SQL to populate default data:

```sql
-- Run this file second
src/main/resources/sql/initial_data_sql_v2.sql
```

This will:
- Create default UserTypes (SUPER_ADMIN, MIDDLE_MANAGER, REGULAR_USER, OCCASIONAL_USER)
- Create default AccountTypes (COLLECTION, PRIVILEGED, PERSONAL, SOLUTION, PUBLIC)
- Assign user types to existing users
- Create sample accounts for Linux Production Server

### 3. Start the Application
```bash
./gradlew bootRun
```

## Testing Endpoints

### Base URL
```
http://localhost:8081
```

### Headers
For authenticated requests, include:
```
X-Username: admin
```

---

## Test Case 1: Verify User Types

### 1.1 Get All User Types
**Request:**
```http
GET /api/user-types
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "typeCode": "SUPER_ADMIN",
    "typeName": "Super Admin User",
    "description": "Super administrator with highest privileges",
    "active": true
  },
  {
    "id": 2,
    "typeCode": "MIDDLE_MANAGER",
    "typeName": "Middle Manager",
    "description": "Department or team manager",
    "active": true
  },
  {
    "id": 3,
    "typeCode": "REGULAR_USER",
    "typeName": "Regular User",
    "description": "Standard user with normal access",
    "active": true
  },
  {
    "id": 4,
    "typeCode": "OCCASIONAL_USER",
    "typeName": "Occasional User",
    "description": "User with limited or occasional access",
    "active": true
  }
]
```

### 1.2 Get User Type by Code
**Request:**
```http
GET /api/user-types/code/SUPER_ADMIN
```

**Expected Response:**
```json
{
  "id": 1,
  "typeCode": "SUPER_ADMIN",
  "typeName": "Super Admin User",
  "description": "Super administrator with highest privileges",
  "active": true
}
```

### 1.3 Get Active User Types Only
**Request:**
```http
GET /api/user-types/active
```

**Expected:** Only active user types (should be all 4 by default)

---

## Test Case 2: Verify Users Have User Types

### 2.1 Get User Details (Check UserType)
**Request:**
```http
GET /api/users
```

**Expected:** Each user should have a `userType` field populated. For example:
```json
{
  "id": 1,
  "username": "admin",
  "email": "admin@example.com",
  "userType": {
    "id": 1,
    "typeCode": "SUPER_ADMIN",
    "typeName": "Super Admin User"
  },
  "active": true
}
```

**Verification:**
- `admin` user should have `userType.typeCode = "SUPER_ADMIN"`
- `manager` user should have `userType.typeCode = "MIDDLE_MANAGER"`
- Other users should have `REGULAR_USER` or `OCCASIONAL_USER`

---

## Test Case 3: Verify Account Types

### 3.1 Get All Account Types
**Request:**
```http
GET /api/account-types
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "typeCode": "COLLECTION",
    "typeName": "Collection Account",
    "description": "Account used for collecting data or resources",
    "active": true
  },
  {
    "id": 2,
    "typeCode": "PRIVILEGED",
    "typeName": "Privileged Account",
    "description": "Account with elevated privileges",
    "active": true
  },
  {
    "id": 3,
    "typeCode": "PERSONAL",
    "typeName": "Personal Account",
    "description": "Personal user account",
    "active": true
  },
  {
    "id": 4,
    "typeCode": "SOLUTION",
    "typeName": "Solution Account",
    "description": "Account for specific solution or service",
    "active": true
  },
  {
    "id": 5,
    "typeCode": "PUBLIC",
    "typeName": "Public Account",
    "description": "Public or shared account",
    "active": true
  }
]
```

### 3.2 Get Account Type by Code
**Request:**
```http
GET /api/account-types/code/PRIVILEGED
```

**Expected:** Returns PRIVILEGED account type details

---

## Test Case 4: Verify Accounts

### 4.1 Get All Accounts
**Request:**
```http
GET /api/accounts
```

**Expected:** Should return at least 3 accounts for Linux Production Server:
- `root` (PRIVILEGED)
- `web_root` (PRIVILEGED)
- `web_user` (PERSONAL)

### 4.2 Get Accounts by Equipment
**Request:**
```http
GET /api/accounts/equipment/{equipmentId}
```

**Steps:**
1. First, get equipment ID:
   ```http
   GET /api/equipment
   ```
   Find the ID for "Linux Production Server"

2. Then get accounts:
   ```http
   GET /api/accounts/equipment/1
   ```

**Expected Response:**
```json
[
  {
    "id": 1,
    "accountName": "root",
    "accountType": {
      "id": 2,
      "typeCode": "PRIVILEGED",
      "typeName": "Privileged Account"
    },
    "equipment": {
      "id": 1,
      "deviceName": "Linux Production Server"
    },
    "username": "root",
    "description": "Root account for Linux server",
    "active": true
  },
  {
    "id": 2,
    "accountName": "web_root",
    "accountType": {
      "id": 2,
      "typeCode": "PRIVILEGED"
    },
    "username": "webadmin",
    "active": true
  },
  {
    "id": 3,
    "accountName": "web_user",
    "accountType": {
      "id": 3,
      "typeCode": "PERSONAL"
    },
    "username": "webuser",
    "active": true
  }
]
```

**Note:** Password fields should NOT appear in the response (hidden by `@JsonIgnore`)

---

## Test Case 5: Create New Account

### 5.1 Create Account for Equipment
**Request:**
```http
POST /api/accounts
Content-Type: application/json

{
  "accountName": "db_admin",
  "accountTypeId": 2,
  "equipmentId": 1,
  "username": "dbadmin",
  "password": "secure123",
  "description": "Database administrator account"
}
```

**Expected Response:** 201 Created with account details

### 5.2 Verify Account Created
**Request:**
```http
GET /api/accounts/equipment/1
```

**Expected:** Should now include the new `db_admin` account

---

## Test Case 6: Update Account

### 6.1 Update Account
**Request:**
```http
PUT /api/accounts/{accountId}
Content-Type: application/json

{
  "description": "Updated description",
  "active": true
}
```

**Expected:** Updated account details

---

## Test Case 7: Policy Fact Loader Integration

### 7.1 Test SSH Access Endpoint (Verify UserType in Attributes)
**Request:**
```http
GET /api/equipment-access/ssh?username=admin&equipmentId=1&clientIp=192.168.1.100
```

**Check Application Logs:**
Look for these debug messages:
```
User Type: SUPER_ADMIN
Equipment Account Types: [PRIVILEGED, PERSONAL]
```

**Verification:**
- The `EquipmentAccessRequest` should have `attributes.get("userTypeCode") = "SUPER_ADMIN"`
- The `EquipmentAccessRequest` should have `attributes.get("equipmentAccountTypeCodes")` containing account type codes

### 7.2 Test with Different User Types
**Request:**
```http
GET /api/equipment-access/ssh?username=manager&equipmentId=1&clientIp=192.168.1.100
```

**Expected in Logs:**
```
User Type: MIDDLE_MANAGER
```

---

## Test Case 8: Database Verification

### 8.1 Verify User Types Table
```sql
SELECT * FROM user_types;
```

**Expected:** 4 rows (SUPER_ADMIN, MIDDLE_MANAGER, REGULAR_USER, OCCASIONAL_USER)

### 8.2 Verify Users Have User Types
```sql
SELECT u.username, ut.type_code, ut.type_name
FROM users u
LEFT JOIN user_types ut ON u.user_type_id = ut.id;
```

**Expected:** All users should have a user_type_id assigned

### 8.3 Verify Account Types Table
```sql
SELECT * FROM account_types;
```

**Expected:** 5 rows (COLLECTION, PRIVILEGED, PERSONAL, SOLUTION, PUBLIC)

### 8.4 Verify Accounts Table
```sql
SELECT a.account_name, at.type_code, e.device_name
FROM accounts a
JOIN account_types at ON a.account_type_id = at.id
JOIN equipment e ON a.equipment_id = e.id;
```

**Expected:** At least 3 accounts for Linux Production Server

---

## Troubleshooting

### Issue: User Type Not Showing in User Response
**Solution:**
1. Verify migration SQL ran successfully
2. Check that `users.user_type_id` column exists
3. Verify initial data SQL updated users with user types
4. Restart the application

### Issue: Accounts Not Found
**Solution:**
1. Verify `accounts` table exists
2. Check that initial data SQL created sample accounts
3. Verify equipment ID is correct
4. Check that accounts are active (`is_active = true`)

### Issue: PolicyFactLoader Not Loading UserType
**Solution:**
1. Check application logs for "User Type: ..." message
2. Verify User entity has `userType` field populated
3. Ensure `UserTypeRepository` is injected correctly
4. Check that user has a `user_type_id` in database

### Issue: Account Type Codes Not in Attributes
**Solution:**
1. Verify `AccountRepository` is injected in `PolicyFactLoader`
2. Check that equipment has accounts
3. Verify accounts are active
4. Check application logs for "Equipment Account Types: ..." message

---

## Next Steps (Phase 2)

After Phase 1 testing is complete, Phase 2 will add:
- WorkGroup entity
- WorkGroup membership (users, equipment, accounts)
- WorkGroup policy catalog
- Policy assignments by UserType and AccountType

---

## Summary Checklist

- [ ] Database migration executed successfully
- [ ] Initial data SQL executed successfully
- [ ] Application starts without errors
- [ ] UserTypes endpoint returns 4 types
- [ ] Users have userType field populated
- [ ] AccountTypes endpoint returns 5 types
- [ ] Accounts endpoint returns sample accounts
- [ ] Can create new account via API
- [ ] Can update account via API
- [ ] PolicyFactLoader logs show UserType
- [ ] PolicyFactLoader logs show AccountTypeCodes
- [ ] Database tables verified

---

## API Endpoints Summary

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/user-types` | GET | Get all user types |
| `/api/user-types/active` | GET | Get active user types |
| `/api/user-types/{id}` | GET | Get user type by ID |
| `/api/user-types/code/{typeCode}` | GET | Get user type by code |
| `/api/account-types` | GET | Get all account types |
| `/api/account-types/active` | GET | Get active account types |
| `/api/account-types/{id}` | GET | Get account type by ID |
| `/api/account-types/code/{typeCode}` | GET | Get account type by code |
| `/api/accounts` | GET | Get all accounts |
| `/api/accounts/{id}` | GET | Get account by ID |
| `/api/accounts/equipment/{equipmentId}` | GET | Get accounts by equipment |
| `/api/accounts/type/{accountTypeId}` | GET | Get accounts by type |
| `/api/accounts` | POST | Create new account |
| `/api/accounts/{id}` | PUT | Update account |
| `/api/accounts/{id}` | DELETE | Delete account (soft delete) |
