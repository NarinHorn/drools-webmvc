# Policy Management Testing Flow

## Overview

This guide tests the policy resolution system based on the requirements:
- **Priority Order**: `WORK_GROUP(300) > USER(200) > USER_TYPE(100) > GLOBAL(0)`
- Higher priority value = more specific = wins
- **WORK_GROUP policies have the HIGHEST priority** - they override all other policies

## Test Data Summary

### Users
| Username | User Type | Role | Work Group |
|----------|-----------|------|------------|
| admin | SUPER_ADMIN | ADMIN | - |
| yhkim | SUPER_ADMIN | ADMIN | Web Development Team |
| yang | SUPER_ADMIN | ADMIN | - |
| huy | MIDDLE_MANAGER | USER | - |
| sokhim | REGULAR_USER | USER | Web Development Team |
| lee | OCCASIONAL_USER | USER | Web Development Team |

### Policies
| Policy Name | Type | Priority | Assigned To |
|-------------|------|----------|-------------|
| Web Team Login Methods | loginMethods | 300 | WORK_GROUP: Web Development Team |
| Web Team Concurrent Sessions | concurrentSessions | 300 | WORK_GROUP: Web Development Team |
| User Lee Login Methods | loginMethods | 200 | USER: lee |
| User yhkim Login Methods | loginMethods | 200 | USER: yhkim |
| Super Admin Login Methods | loginMethods | 100 | USER_TYPE: SUPER_ADMIN |
| Occasional User Login Methods | loginMethods | 100 | USER_TYPE: OCCASIONAL_USER |
| Global Default Login Methods | loginMethods | 0 | GLOBAL (all) |
| Global Default Session Timeout | sessionTimeout | 0 | GLOBAL (all) |
| Privileged Account Session Timeout | sessionTimeout | 100 | ACCOUNT_TYPE: PRIVILEGED |
| Public Account Session Timeout | sessionTimeout | 100 | ACCOUNT_TYPE: PUBLIC |
| Oracle Device Session Timeout | sessionTimeout | 100 | EQUIPMENT: Oracle DB Server |

---

## Setup

### Step 1: Run the Test Data SQL
```bash
psql -U your_user -d your_db -f src/main/resources/sql/policy_test_data.sql
```

### Step 2: Run Access Policies (for API authorization)
```bash
psql -U your_user -d your_db -f src/main/resources/sql/initial_data.sql
```

### Step 3: Start the Application
```bash
./gradlew bootRun
```

---

## Test Cases

### Test 1: Lee's Login Policy
**Expected**: WORK_GROUP policy wins (priority 300) - Lee is in Web Development Team

```bash
curl -s "http://localhost:8081/api/clients/login-policy?username=lee" -H "X-Username: admin" | jq
```

**Expected Response**:
```json
{
  "username": "lee",
  "userType": "OCCASIONAL_USER",
  "priorityOrder": "WORK_GROUP(300) > USER(200) > USER_TYPE(100) > GLOBAL(0)",
  "allMatchingPolicies": [
    {"source": "WORK_GROUP:Web Development Team", "priority": 300, "policyName": "Web Team Login Methods"},
    {"source": "USER", "priority": 200, "policyName": "User Lee Login Methods"},
    {"source": "USER_TYPE:OCCASIONAL_USER", "priority": 100, "policyName": "Occasional User Login Methods"},
    {"source": "GLOBAL", "priority": 0, "policyName": "Global Default Login Methods"}
  ],
  "effectivePolicy": {
    "source": "WORK_GROUP:Web Development Team",
    "priority": 300,
    "policyName": "Web Team Login Methods",
    "config": {"loginMethods": {"credential": true, "mfa": {"enabled": false}}}
  },
  "resolution": "Policy 'Web Team Login Methods' from WORK_GROUP:Web Development Team (priority 300) wins"
}
```

**Verification**: Even though Lee has a USER-specific policy, the WORK_GROUP policy takes precedence.

---

### Test 2: yhkim's Login Policy
**Expected**: WORK_GROUP policy wins (priority 300) - yhkim is in Web Development Team

```bash
curl -s "http://localhost:8081/api/clients/login-policy?username=yhkim" -H "X-Username: admin" | jq
```

**Expected Response**:
- effectivePolicy.source = "WORK_GROUP:Web Development Team"
- effectivePolicy.policyName = "Web Team Login Methods"
- mfa.enabled = false (credential only from work group)

**Verification**: yhkim's USER-specific policy (priority 200) is overridden by the WORK_GROUP policy (priority 300).

---

### Test 3: yang's Login Policy
**Expected**: USER_TYPE policy wins (priority 100) - yang is NOT in any work group

```bash
curl -s "http://localhost:8081/api/clients/login-policy?username=yang" -H "X-Username: admin" | jq
```

**Expected Response**:
- effectivePolicy.source = "USER_TYPE:SUPER_ADMIN"
- effectivePolicy.policyName = "Super Admin Login Methods"
- mfa.required = "otp"

**Verification**: yang has no WORK_GROUP or USER-specific policy, so USER_TYPE applies.

---

### Test 4: huy's Login Policy
**Expected**: GLOBAL policy wins (priority 0) - huy has no specific policies

```bash
curl -s "http://localhost:8081/api/clients/login-policy?username=huy" -H "X-Username: admin" | jq
```

**Expected Response**:
- effectivePolicy.source = "GLOBAL"
- effectivePolicy.policyName = "Global Default Login Methods"
- mfa.default = "email"

**Verification**: huy has no WORK_GROUP, USER, or USER_TYPE specific policy, so GLOBAL applies.

---

### Test 5: sokhim's Login Policy
**Expected**: WORK_GROUP policy wins (priority 300) - sokhim is in Web Development Team

```bash
curl -s "http://localhost:8081/api/clients/login-policy?username=sokhim" -H "X-Username: admin" | jq
```

**Expected Response**:
- effectivePolicy.source = "WORK_GROUP:Web Development Team"
- effectivePolicy.policyName = "Web Team Login Methods"
- mfa.enabled = false

**Verification**: sokhim is in the Web Development Team, and WORK_GROUP has highest priority.

---

### Test 6: Session Timeout for Privileged Account
**Expected**: ACCOUNT_TYPE policy wins (priority 100)

```bash
curl -s "http://localhost:8081/api/clients/session-timeout?username=yhkim&equipmentId=1&accountName=web_root" -H "X-Username: admin" | jq
```

**Expected Response**:
- accountType = "PRIVILEGED"
- effectiveTimeoutSeconds = 3600
- effectivePolicy.policyName = "Privileged Account Session Timeout"

---

### Test 7: Session Timeout for Public Account
**Expected**: ACCOUNT_TYPE policy wins (priority 100)

```bash
# First, get equipment ID for Public Server
curl -s "http://localhost:8081/api/equipment" -H "X-Username: admin" | jq '.[] | select(.deviceName == "Public Server")'

# Then test session timeout (replace 4 with actual ID)
curl -s "http://localhost:8081/api/clients/session-timeout?username=huy&equipmentId=4&accountName=public_account" -H "X-Username: admin" | jq
```

**Expected Response**:
- accountType = "PUBLIC"
- effectiveTimeoutSeconds = 300

---

### Test 8: Session Timeout for Oracle Device
**Expected**: EQUIPMENT policy wins (priority 100)

```bash
# Get equipment ID for Oracle DB Server
curl -s "http://localhost:8081/api/equipment" -H "X-Username: admin" | jq '.[] | select(.deviceName == "Oracle DB Server")'

# Test session timeout (replace 3 with actual ID)
curl -s "http://localhost:8081/api/clients/session-timeout?username=huy&equipmentId=3" -H "X-Username: admin" | jq
```

**Expected Response**:
- deviceType = "ORACLE"
- effectiveTimeoutSeconds = 1200
- effectivePolicy.policyName = "Oracle Device Session Timeout"

---

### Test 9: Equipment List for yhkim
**Expected**: Shows equipment from Web Development Team

```bash
curl -s "http://localhost:8081/api/clients/equipment-list?username=yhkim" -H "X-Username: admin" | jq
```

**Expected Response**:
- workGroups: ["Web Development Team"]
- accessibleEquipment: Web Server 1, Web Server 2 (IPs: 192.168.10.1, 192.168.10.2)

---

### Test 10: Equipment List for yang
**Expected**: Empty (not in any work group)

```bash
curl -s "http://localhost:8081/api/clients/equipment-list?username=yang" -H "X-Username: admin" | jq
```

**Expected Response**:
- workGroups: []
- accessibleEquipment: []
- totalEquipment: 0

---

### Test 11: Available Accounts on Equipment
**Expected**: Shows accounts from work group

```bash
curl -s "http://localhost:8081/api/clients/equipment/1/accounts?username=yhkim" -H "X-Username: admin" | jq
```

**Expected Response**:
- workGroups: ["Web Development Team"]
- availableAccounts: web_root (PRIVILEGED), web_user (PERSONAL)

---

### Test 12: All Policies for a User
**Expected**: Shows policies from all sources in priority order

```bash
curl -s "http://localhost:8081/api/clients/all-policies?username=lee" -H "X-Username: admin" | jq
```

**Expected Response**:
```json
{
  "username": "lee",
  "userType": "OCCASIONAL_USER",
  "policiesBySource": {
    "WORK_GROUP (priority 300)": [
      {"name": "Web Team Login Methods", "workGroup": "Web Development Team", "priority": 300},
      {"name": "Web Team Concurrent Sessions", "workGroup": "Web Development Team", "priority": 300}
    ],
    "USER (priority 200)": [
      {"name": "User Lee Login Methods", "type": "loginMethods", "priority": 200}
    ],
    "USER_TYPE:OCCASIONAL_USER (priority 100)": [
      {"name": "Occasional User Login Methods", "type": "loginMethods"}
    ],
    "GLOBAL (priority 0)": [
      {"name": "Global Default Login Methods"},
      {"name": "Global Default Session Timeout"}
    ]
  }
}
```

---

## Verification Checklist

| Test | Expected Winner | Actual Winner | Pass/Fail |
|------|-----------------|---------------|-----------|
| 1. Lee login | WORK_GROUP: Web Team Login Methods | | |
| 2. yhkim login | WORK_GROUP: Web Team Login Methods | | |
| 3. yang login | USER_TYPE: Super Admin Login Methods | | |
| 4. huy login | GLOBAL: Global Default Login Methods | | |
| 5. sokhim login | WORK_GROUP: Web Team Login Methods | | |
| 6. Privileged account timeout | ACCOUNT_TYPE: 3600s | | |
| 7. Public account timeout | ACCOUNT_TYPE: 300s | | |
| 8. Oracle device timeout | EQUIPMENT: 1200s | | |
| 9. yhkim equipment list | 2 equipment (Web Servers) | | |
| 10. yang equipment list | 0 equipment | | |
| 11. yhkim accounts on equip 1 | web_root, web_user | | |
| 12. lee all policies | 4 sources with policies | | |

---

## Quick Run All Tests

```bash
#!/bin/bash
BASE_URL="http://localhost:8081/api/clients"
HEADER="X-Username: admin"

echo "=== Test 1: Lee's Login Policy (expect WORK_GROUP) ==="
curl -s "$BASE_URL/login-policy?username=lee" -H "$HEADER" | jq '.effectivePolicy.source, .effectivePolicy.policyName'

echo -e "\n=== Test 2: yhkim's Login Policy (expect WORK_GROUP) ==="
curl -s "$BASE_URL/login-policy?username=yhkim" -H "$HEADER" | jq '.effectivePolicy.source, .effectivePolicy.policyName'

echo -e "\n=== Test 3: yang's Login Policy (expect USER_TYPE) ==="
curl -s "$BASE_URL/login-policy?username=yang" -H "$HEADER" | jq '.effectivePolicy.source, .effectivePolicy.policyName'

echo -e "\n=== Test 4: huy's Login Policy (expect GLOBAL) ==="
curl -s "$BASE_URL/login-policy?username=huy" -H "$HEADER" | jq '.effectivePolicy.source, .effectivePolicy.policyName'

echo -e "\n=== Test 5: sokhim's Login Policy (expect WORK_GROUP) ==="
curl -s "$BASE_URL/login-policy?username=sokhim" -H "$HEADER" | jq '.effectivePolicy.source, .effectivePolicy.policyName'

echo -e "\n=== Test 6: Session Timeout (Privileged Account) ==="
curl -s "$BASE_URL/session-timeout?username=yhkim&equipmentId=1&accountName=web_root" -H "$HEADER" | jq '.effectiveTimeoutSeconds, .effectivePolicy.source'

echo -e "\n=== Test 9: Equipment List for yhkim ==="
curl -s "$BASE_URL/equipment-list?username=yhkim" -H "$HEADER" | jq '.totalEquipment, .workGroups[].name'

echo -e "\n=== Test 12: All Policies for lee ==="
curl -s "$BASE_URL/all-policies?username=lee" -H "$HEADER" | jq '.policiesBySource | keys'

echo -e "\n=== All tests complete ==="
```

---

## Key Points

### Why WORK_GROUP has highest priority?
- In enterprise environments, project/workspace policies often need to override individual settings
- When a user joins a work group, they should follow that group's security policies
- This ensures consistent policy enforcement within project teams

### Priority Resolution Flow
```
1. Check WORK_GROUP policies (priority 300) - if found, use it
   ↓ not found
2. Check USER-specific policies (priority 200) - if found, use it
   ↓ not found
3. Check USER_TYPE policies (priority 100) - if found, use it
   ↓ not found
4. Use GLOBAL default policy (priority 0)
```

---

## Troubleshooting

### No policies found
1. Ensure `policy_test_data.sql` was run successfully
2. Check that policy_types exist: `SELECT * FROM policy_types;`
3. Check that policies exist: `SELECT * FROM equipment_policies;`
4. Check assignments: `SELECT * FROM policy_user_assignments;`

### User not found
1. Check users exist: `SELECT username, user_type_id FROM users;`
2. Verify user_types: `SELECT * FROM user_types;`

### Work group policies not loading
1. Check work_groups: `SELECT * FROM work_groups;`
2. Check work_group_users: `SELECT * FROM work_group_users;`
3. Check work_group_policies: `SELECT * FROM work_group_policies;`

### 403 Forbidden on API calls
1. Run `initial_data.sql` to set up access_policies
2. Use correct header: `X-Username: admin` (not `X-User-Id`)
3. Check admin user exists: `SELECT * FROM users WHERE username = 'admin';`
