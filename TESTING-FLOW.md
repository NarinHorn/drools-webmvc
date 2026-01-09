# Complete Testing Flow Guide

This document provides a comprehensive step-by-step testing guide for the ABAC Policy Management System. It covers all aspects of the system including user management, role management, policy creation, and access control testing.

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Initial Setup Verification](#initial-setup-verification)
3. [Testing User Management](#testing-user-management)
4. [Testing Role Management](#testing-role-management)
5. [Testing Policy Management](#testing-policy-management)
6. [Testing Access Control](#testing-access-control)
7. [Testing SSH Access Policy Scenario](#testing-ssh-access-policy-scenario)
8. [Testing Protected Endpoints](#testing-protected-endpoints)
9. [Advanced Testing Scenarios](#advanced-testing-scenarios)
10. [Troubleshooting Common Issues](#troubleshooting-common-issues)

---

## 🎯 Prerequisites

Before starting the testing flow, ensure:

1. **Application is running** on `http://localhost:8081`
2. **Database is initialized** with initial data (roles, users, policies)
3. **Swagger UI is accessible** at `http://localhost:8081/swagger-ui.html`
4. **Postman or cURL** is available for API testing

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

## ✅ Initial Setup Verification

### Step 1: Verify Initial Data

**1.1 Check Roles**

```bash
GET http://localhost:8081/api/roles
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "name": "ADMIN",
    "description": "System Administrator with full access"
  },
  {
    "id": 2,
    "name": "MANAGER",
    "description": "Department Manager"
  },
  {
    "id": 3,
    "name": "USER",
    "description": "Regular User"
  },
  {
    "id": 4,
    "name": "VIEWER",
    "description": "Read-only access"
  }
]
```

**1.2 Check Users**

```bash
GET http://localhost:8081/api/users
```

**Expected Response:** Should contain at least 5 users:
- `admin` (ADMIN role)
- `manager` (MANAGER role)
- `john` (USER role, SALES department)
- `jane` (USER role, HR department)
- `viewer` (VIEWER role)

**1.3 Check Existing Policies**

```bash
GET http://localhost:8081/api/policies
```

**Expected Response:** Should contain at least 6 pre-configured policies.

---

## 👥 Testing User Management

### Test Case 1: Create a New User

**Request:**
```bash
POST http://localhost:8081/api/users
Content-Type: application/json

{
  "username": "testuser",
  "password": "test123",
  "email": "testuser@example.com",
  "department": "IT",
  "level": 4,
  "active": true
}
```

**Expected Response:** `201 Created`
```json
{
  "id": 6,
  "username": "testuser",
  "email": "testuser@example.com",
  "department": "IT",
  "level": 4,
  "active": true,
  "roles": []
}
```

**Verification:**
```bash
GET http://localhost:8081/api/users/username/testuser
```

### Test Case 2: Get User by ID

**Request:**
```bash
GET http://localhost:8081/api/users/1
```

**Expected Response:** `200 OK` with user details

### Test Case 3: Update User Information

**Request:**
```bash
PUT http://localhost:8081/api/users/6
Content-Type: application/json

{
  "email": "updated@example.com",
  "department": "SALES",
  "level": 5,
  "active": true
}
```

**Expected Response:** `200 OK` with updated user

### Test Case 4: Add Role to User

**Request:**
```bash
POST http://localhost:8081/api/users/6/roles/USER
```

**Expected Response:** `200 OK` with user including the new role

**Verification:**
```bash
GET http://localhost:8081/api/users/6
# Should show USER role in the roles array
```

### Test Case 5: Remove Role from User

**Request:**
```bash
DELETE http://localhost:8081/api/users/6/roles/USER
```

**Expected Response:** `200 OK` with user without the removed role

### Test Case 6: Delete User

**Request:**
```bash
DELETE http://localhost:8081/api/users/6
```

**Expected Response:** `200 OK`
```json
{
  "message": "User deleted successfully"
}
```

---

## 🔐 Testing Role Management

### Test Case 1: Create a New Role

**Request:**
```bash
POST http://localhost:8081/api/roles
Content-Type: application/json

{
  "name": "SSH_USER",
  "description": "User with SSH access permissions"
}
```

**Expected Response:** `201 Created`
```json
{
  "id": 5,
  "name": "SSH_USER",
  "description": "User with SSH access permissions"
}
```

### Test Case 2: Get All Roles

**Request:**
```bash
GET http://localhost:8081/api/roles
```

**Expected Response:** `200 OK` with list of all roles

### Test Case 3: Get Role by Name

**Request:**
```bash
GET http://localhost:8081/api/roles/name/SSH_USER
```

**Expected Response:** `200 OK` with role details

### Test Case 4: Update Role Description

**Request:**
```bash
PUT http://localhost:8081/api/roles/5
Content-Type: application/json

{
  "description": "Updated description for SSH users"
}
```

**Expected Response:** `200 OK` with updated role

### Test Case 5: Delete Role

**Request:**
```bash
DELETE http://localhost:8081/api/roles/5
```

**Expected Response:** `200 OK`
```json
{
  "message": "Role deleted successfully"
}
```

---

## 📜 Testing Policy Management

### Test Case 1: Get All Policies

**Request:**
```bash
GET http://localhost:8081/api/policies
```

**Expected Response:** `200 OK` with list of all policies

### Test Case 2: Get Enabled Policies Only

**Request:**
```bash
GET http://localhost:8081/api/policies/enabled
```

**Expected Response:** `200 OK` with only enabled policies

### Test Case 3: Get Policy by ID

**Request:**
```bash
GET http://localhost:8081/api/policies/1
```

**Expected Response:** `200 OK` with policy details including generated DRL

### Test Case 4: Preview DRL Without Saving

**Request:**
```bash
POST http://localhost:8081/api/policies/preview-drl
Content-Type: application/json

{
  "policyName": "Test Policy Preview",
  "description": "Testing DRL preview",
  "endpoint": "/api/test/**",
  "httpMethod": "GET",
  "allowedRoles": ["USER"],
  "effect": "ALLOW",
  "priority": 10,
  "enabled": true
}
```

**Expected Response:** `200 OK`
```json
{
  "drl": "package rules.dynamic;\n\nimport com.hunesion.drool_v2.model.AccessRequest;\n..."
}
```

### Test Case 5: Create a New Policy

**Request:**
```bash
POST http://localhost:8081/api/policies
Content-Type: application/json

{
  "policyName": "Test Policy",
  "description": "A test policy for demonstration",
  "endpoint": "/api/test/**",
  "httpMethod": "GET",
  "allowedRoles": ["USER", "MANAGER"],
  "conditions": {
    "department": {
      "operator": "equals",
      "value": "SALES"
    }
  },
  "effect": "ALLOW",
  "priority": 50,
  "enabled": true
}
```

**Expected Response:** `201 Created`
```json
{
  "id": 7,
  "policyName": "Test Policy",
  "description": "A test policy for demonstration",
  "endpoint": "/api/test/**",
  "httpMethod": "GET",
  "allowedRoles": "[\"USER\",\"MANAGER\"]",
  "conditions": "{\"department\":{\"operator\":\"equals\",\"value\":\"SALES\"}}",
  "effect": "ALLOW",
  "priority": 50,
  "enabled": true,
  "generatedDrl": "package rules.dynamic;..."
}
```

**Verification:**
- Check that the policy appears in the list: `GET /api/policies`
- Verify the generated DRL is correct

### Test Case 6: Update an Existing Policy

**Request:**
```bash
PUT http://localhost:8081/api/policies/7
Content-Type: application/json

{
  "policyName": "Test Policy Updated",
  "description": "Updated description",
  "endpoint": "/api/test/**",
  "httpMethod": "GET",
  "allowedRoles": ["ADMIN"],
  "effect": "ALLOW",
  "priority": 60,
  "enabled": true
}
```

**Expected Response:** `200 OK` with updated policy

### Test Case 7: Toggle Policy (Enable/Disable)

**Request:**
```bash
PATCH http://localhost:8081/api/policies/7/toggle?enabled=false
```

**Expected Response:** `200 OK` with `enabled: false`

**Verification:**
```bash
GET http://localhost:8081/api/policies/enabled
# Policy 7 should NOT appear in the list
```

### Test Case 8: Delete a Policy

**Request:**
```bash
DELETE http://localhost:8081/api/policies/7
```

**Expected Response:** `200 OK`
```json
{
  "message": "Policy deleted successfully"
}
```

**Verification:**
```bash
GET http://localhost:8081/api/policies
# Policy 7 should NOT appear in the list
```

---

## 🔒 Testing Access Control

### Test Case 1: Check Access by Username (Simple)

**Request:**
```bash
GET http://localhost:8081/api/access/check?username=john&endpoint=/api/reports&method=GET
```

**Expected Response:** `200 OK`
```json
{
  "username": "john",
  "endpoint": "/api/reports",
  "method": "GET",
  "allowed": true,
  "matchedPolicy": "Manager Reports Access"
}
```

**Test with Different Users:**

```bash
# Test with manager (should be allowed)
GET http://localhost:8081/api/access/check?username=manager&endpoint=/api/reports&method=GET

# Test with viewer (should be denied)
GET http://localhost:8081/api/access/check?username=viewer&endpoint=/api/reports&method=GET
```

### Test Case 2: Check Access with Custom Request

**Request:**
```bash
POST http://localhost:8081/api/access/check
Content-Type: application/json

{
  "username": "john",
  "userRoles": ["USER"],
  "endpoint": "/api/sales",
  "httpMethod": "GET",
  "department": "SALES",
  "userLevel": 3,
  "attributes": {}
}
```

**Expected Response:** `200 OK`
```json
{
  "allowed": true,
  "matchedPolicyName": "Sales Department Data Access",
  "denialReason": null,
  "evaluated": true
}
```

### Test Case 3: Test Access Denial

**Request:**
```bash
POST http://localhost:8081/api/access/check
Content-Type: application/json

{
  "username": "viewer",
  "userRoles": ["VIEWER"],
  "endpoint": "/api/admin/dashboard",
  "httpMethod": "GET",
  "department": "GUEST",
  "userLevel": 1,
  "attributes": {}
}
```

**Expected Response:** `200 OK`
```json
{
  "allowed": false,
  "matchedPolicyName": null,
  "denialReason": "No access policy found for this endpoint",
  "evaluated": true
}
```

---

## 🖥️ Testing SSH Access Policy Scenario

This section demonstrates how to test the SSH access policy scenario discussed earlier.

### Step 1: Create SSH_USER Role (if not exists)

**Request:**
```bash
POST http://localhost:8081/api/roles
Content-Type: application/json

{
  "name": "SSH_USER",
  "description": "User with SSH access permissions"
}
```

### Step 2: Create User with SSH Access

**Request:**
```bash
POST http://localhost:8081/api/users
Content-Type: application/json

{
  "username": "narin",
  "password": "asdqwe",
  "email": "narin@example.com",
  "department": "IT",
  "level": 5,
  "active": true
}
```

### Step 3: Assign SSH_USER Role to User

**Request:**
```bash
POST http://localhost:8081/api/users/username/narin/roles/SSH_USER
```

**Note:** You may need to get the user ID first:
```bash
GET http://localhost:8081/api/users/username/narin
# Use the ID from the response
```

### Step 4: Create SSH Access Policy

**Request:**
```bash
POST http://localhost:8081/api/policies
Content-Type: application/json

{
  "policyName": "SSH_Access_192.168.0.211",
  "description": "Allow SSH access to server 192.168.0.211 for authorized users",
  "endpoint": "/api/guacamole/connect/**",
  "httpMethod": "POST",
  "allowedRoles": ["ADMIN", "SSH_USER"],
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
```

**Expected Response:** `201 Created` with the policy

### Step 5: Test SSH Access - Allowed Scenario

**Request:**
```bash
POST http://localhost:8081/api/access/check
Content-Type: application/json

{
  "username": "narin",
  "userRoles": ["SSH_USER"],
  "endpoint": "/api/guacamole/connect",
  "httpMethod": "POST",
  "attributes": {
    "protocol": "ssh",
    "hostname": "192.168.0.211",
    "port": "22",
    "username": "narin"
  }
}
```

**Expected Response:** `200 OK`
```json
{
  "allowed": true,
  "matchedPolicyName": "SSH_Access_192.168.0.211",
  "denialReason": null,
  "evaluated": true
}
```

### Step 6: Test SSH Access - Denied Scenario (Wrong Hostname)

**Request:**
```bash
POST http://localhost:8081/api/access/check
Content-Type: application/json

{
  "username": "narin",
  "userRoles": ["SSH_USER"],
  "endpoint": "/api/guacamole/connect",
  "httpMethod": "POST",
  "attributes": {
    "protocol": "ssh",
    "hostname": "192.168.0.100",
    "port": "22",
    "username": "narin"
  }
}
```

**Expected Response:** `200 OK`
```json
{
  "allowed": false,
  "matchedPolicyName": null,
  "denialReason": "No access policy found for this endpoint",
  "evaluated": true
}
```

### Step 7: Test SSH Access - Denied Scenario (Wrong Role)

**Request:**
```bash
POST http://localhost:8081/api/access/check
Content-Type: application/json

{
  "username": "john",
  "userRoles": ["USER"],
  "endpoint": "/api/guacamole/connect",
  "httpMethod": "POST",
  "attributes": {
    "protocol": "ssh",
    "hostname": "192.168.0.211",
    "port": "22",
    "username": "narin"
  }
}
```

**Expected Response:** `200 OK`
```json
{
  "allowed": false,
  "matchedPolicyName": null,
  "denialReason": "No access policy found for this endpoint",
  "evaluated": true
}
```

---

## 🛡️ Testing Protected Endpoints

All protected endpoints require the `X-Username` header. The interceptor will automatically check access before allowing the request.

### Test Case 1: Access Reports Endpoint (Manager Required)

**Request:**
```bash
GET http://localhost:8081/api/reports
X-Username: manager
```

**Expected Response:** `200 OK` with reports data

**Test with Non-Manager:**
```bash
GET http://localhost:8081/api/reports
X-Username: john
```

**Expected Response:** `403 Forbidden`
```json
{
  "error": "Access denied",
  "message": "No access policy found for this endpoint",
  "policy": null
}
```

### Test Case 2: Access Profile Endpoint

**Request:**
```bash
GET http://localhost:8081/api/profile
X-Username: jane
```

**Expected Response:** `200 OK`
```json
{
  "endpoint": "/api/profile",
  "username": "jane",
  "message": "This is your profile data"
}
```

### Test Case 3: Access Sales Data (Department Check)

**Request:**
```bash
GET http://localhost:8081/api/sales
X-Username: john
```

**Expected Response:** `200 OK` (john is in SALES department)

**Test with Non-Sales User:**
```bash
GET http://localhost:8081/api/sales
X-Username: jane
```

**Expected Response:** `403 Forbidden` (jane is in HR department)

### Test Case 4: Access Management Data (Level Check)

**Request:**
```bash
GET http://localhost:8081/api/management
X-Username: manager
```

**Expected Response:** `200 OK` (manager has level 5)

**Test with Low-Level User:**
```bash
GET http://localhost:8081/api/management
X-Username: viewer
```

**Expected Response:** `403 Forbidden` (viewer has level 1)

### Test Case 5: Access Admin Dashboard (Admin Only)

**Request:**
```bash
GET http://localhost:8081/api/admin/dashboard
X-Username: admin
```

**Expected Response:** `200 OK`

**Test with Non-Admin:**
```bash
GET http://localhost:8081/api/admin/dashboard
X-Username: manager
```

**Expected Response:** `403 Forbidden`

### Test Case 6: Access Public Info (No Authentication Required)

**Request:**
```bash
GET http://localhost:8081/api/public/info
```

**Expected Response:** `200 OK` (public endpoint, no header needed)

---

## 🧪 Advanced Testing Scenarios

### Scenario 1: Policy Priority Testing

**Objective:** Test that higher priority policies are evaluated first.

**Steps:**

1. Create Policy A with priority 10:
```bash
POST http://localhost:8081/api/policies
{
  "policyName": "Policy A - Low Priority",
  "endpoint": "/api/test/**",
  "httpMethod": "*",
  "allowedRoles": ["USER"],
  "effect": "ALLOW",
  "priority": 10
}
```

2. Create Policy B with priority 50:
```bash
POST http://localhost:8081/api/policies
{
  "policyName": "Policy B - High Priority",
  "endpoint": "/api/test/**",
  "httpMethod": "*",
  "allowedRoles": ["USER"],
  "effect": "DENY",
  "priority": 50
}
```

3. Test access:
```bash
POST http://localhost:8081/api/access/check
{
  "username": "john",
  "userRoles": ["USER"],
  "endpoint": "/api/test",
  "httpMethod": "GET"
}
```

**Expected Result:** Access should be DENIED (Policy B with higher priority wins)

### Scenario 2: Multiple Conditions Testing

**Objective:** Test policies with multiple conditions.

**Steps:**

1. Create a policy with multiple conditions:
```bash
POST http://localhost:8081/api/policies
{
  "policyName": "Multi-Condition Policy",
  "endpoint": "/api/advanced/**",
  "httpMethod": "GET",
  "allowedRoles": ["USER"],
  "conditions": {
    "department": {
      "operator": "equals",
      "value": "SALES"
    },
    "userLevel": {
      "operator": "greaterThanOrEqual",
      "value": "3"
    }
  },
  "effect": "ALLOW",
  "priority": 30
}
```

2. Test with matching conditions:
```bash
POST http://localhost:8081/api/access/check
{
  "username": "john",
  "userRoles": ["USER"],
  "endpoint": "/api/advanced/test",
  "httpMethod": "GET",
  "department": "SALES",
  "userLevel": 3
}
```

**Expected Result:** Access ALLOWED

3. Test with non-matching department:
```bash
POST http://localhost:8081/api/access/check
{
  "username": "jane",
  "userRoles": ["USER"],
  "endpoint": "/api/advanced/test",
  "httpMethod": "GET",
  "department": "HR",
  "userLevel": 3
}
```

**Expected Result:** Access DENIED

### Scenario 3: Policy Disable/Enable Testing

**Objective:** Test that disabled policies are not evaluated.

**Steps:**

1. Create and enable a policy:
```bash
POST http://localhost:8081/api/policies
{
  "policyName": "Test Disable Policy",
  "endpoint": "/api/disable-test/**",
  "httpMethod": "*",
  "allowedRoles": ["USER"],
  "effect": "ALLOW",
  "enabled": true
}
```

2. Test access (should be allowed):
```bash
POST http://localhost:8081/api/access/check
{
  "username": "john",
  "userRoles": ["USER"],
  "endpoint": "/api/disable-test",
  "httpMethod": "GET"
}
```

3. Disable the policy:
```bash
PATCH http://localhost:8081/api/policies/{id}/toggle?enabled=false
```

4. Test access again (should be denied):
```bash
POST http://localhost:8081/api/access/check
{
  "username": "john",
  "userRoles": ["USER"],
  "endpoint": "/api/disable-test",
  "httpMethod": "GET"
}
```

**Expected Result:** Access DENIED (policy is disabled)

### Scenario 4: Endpoint Pattern Matching

**Objective:** Test different endpoint patterns.

**Test Cases:**

1. **Exact Match:**
   - Policy: `/api/users`
   - Request: `/api/users` ✅ Match
   - Request: `/api/users/123` ❌ No match

2. **Single Wildcard:**
   - Policy: `/api/users/*`
   - Request: `/api/users/123` ✅ Match
   - Request: `/api/users/123/profile` ❌ No match

3. **Double Wildcard:**
   - Policy: `/api/users/**`
   - Request: `/api/users` ✅ Match
   - Request: `/api/users/123` ✅ Match
   - Request: `/api/users/123/profile` ✅ Match

---

## 🔍 Troubleshooting Common Issues

### Issue 1: Access Always Denied

**Symptoms:** All access checks return `allowed: false`

**Possible Causes:**
1. No matching policy exists
2. Policy is disabled
3. User doesn't have required role
4. Conditions don't match

**Solution:**
1. Check existing policies: `GET /api/policies`
2. Verify policy is enabled: `GET /api/policies/enabled`
3. Check user roles: `GET /api/users/username/{username}`
4. Review policy conditions and user attributes

### Issue 2: Policy Not Taking Effect

**Symptoms:** Policy created but not evaluated

**Possible Causes:**
1. Policy disabled
2. Priority too low (other policy matches first)
3. Endpoint pattern doesn't match
4. HTTP method doesn't match

**Solution:**
1. Verify policy is enabled
2. Check policy priority
3. Test endpoint pattern matching
4. Verify HTTP method matches

### Issue 3: DRL Generation Errors

**Symptoms:** Policy creation fails or DRL is malformed

**Possible Causes:**
1. Invalid condition operator
2. Invalid endpoint pattern
3. Missing required fields

**Solution:**
1. Use preview DRL endpoint to test: `POST /api/policies/preview-drl`
2. Check condition operators are valid
3. Verify all required fields are present

### Issue 4: User Not Found

**Symptoms:** `401 Unauthorized` or "User not found" error

**Possible Causes:**
1. Username doesn't exist
2. Missing X-Username header
3. User is inactive

**Solution:**
1. Verify user exists: `GET /api/users/username/{username}`
2. Check X-Username header is present
3. Verify user is active: `GET /api/users/{id}`

### Issue 5: Role Assignment Not Working

**Symptoms:** User doesn't have expected role

**Possible Causes:**
1. Role doesn't exist
2. Role not properly assigned
3. User entity not refreshed

**Solution:**
1. Verify role exists: `GET /api/roles/name/{roleName}`
2. Check user roles: `GET /api/users/{id}`
3. Re-assign role if needed: `POST /api/users/{id}/roles/{roleName}`

---

## 📊 Testing Checklist

Use this checklist to ensure comprehensive testing:

### User Management
- [ ] Create user
- [ ] Get user by ID
- [ ] Get user by username
- [ ] Update user
- [ ] Add role to user
- [ ] Remove role from user
- [ ] Delete user

### Role Management
- [ ] Create role
- [ ] Get all roles
- [ ] Get role by ID
- [ ] Get role by name
- [ ] Update role
- [ ] Delete role

### Policy Management
- [ ] Get all policies
- [ ] Get enabled policies
- [ ] Get policy by ID
- [ ] Preview DRL
- [ ] Create policy
- [ ] Update policy
- [ ] Toggle policy (enable/disable)
- [ ] Delete policy

### Access Control
- [ ] Check access by username (simple)
- [ ] Check access with custom request
- [ ] Test access allowed scenario
- [ ] Test access denied scenario
- [ ] Test with different roles
- [ ] Test with different departments
- [ ] Test with different user levels
- [ ] Test with custom attributes

### Protected Endpoints
- [ ] Test reports endpoint (manager required)
- [ ] Test profile endpoint
- [ ] Test sales endpoint (department check)
- [ ] Test management endpoint (level check)
- [ ] Test admin dashboard (admin only)
- [ ] Test public info (no auth required)

### Advanced Scenarios
- [ ] Test policy priority
- [ ] Test multiple conditions
- [ ] Test policy enable/disable
- [ ] Test endpoint pattern matching
- [ ] Test SSH access policy scenario

---

## 🎓 Best Practices

1. **Always verify initial data** before starting tests
2. **Use Swagger UI** for interactive testing
3. **Test both positive and negative cases**
4. **Verify policy DRL** using preview endpoint
5. **Check console logs** for Drools rule firing messages
6. **Clean up test data** after testing
7. **Document custom policies** for future reference

---

## 📝 Notes

- All timestamps in responses are in UTC
- Policy priority: Higher numbers are evaluated first
- Endpoint patterns support `*` (single segment) and `**` (any path)
- Conditions are case-sensitive for string comparisons
- Disabled policies are not evaluated but remain in the database
- Policy deletion automatically rebuilds the rule engine

---

## 🔗 Related Documentation

- [README.md](README.md) - Project overview and setup
- [ENDPOINT-FLOW-EXAMPLES.md](ENDPOINT-FLOW-EXAMPLES.md) - Detailed endpoint flow
- [PROJECT_SETUP_README.md](PROJECT_SETUP_README.md) - Complete setup guide

---

**Last Updated:** Generated automatically
**Version:** 1.0.0
