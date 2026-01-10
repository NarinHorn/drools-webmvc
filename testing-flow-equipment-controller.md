# Equipment Controller Testing Flow Guide

This document provides a comprehensive step-by-step testing guide for the Equipment/Device Management API. It covers all CRUD operations including create, read, update, and soft delete functionality.

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Initial Setup Verification](#initial-setup-verification)
3. [Testing Equipment Creation](#testing-equipment-creation)
4. [Testing Equipment Retrieval](#testing-equipment-retrieval)
5. [Testing Equipment Update](#testing-equipment-update)
6. [Testing Equipment Soft Delete](#testing-equipment-soft-delete)
7. [Testing Equipment by Type](#testing-equipment-by-type)
8. [Testing Connection Fields](#testing-connection-fields)
9. [Advanced Testing Scenarios](#advanced-testing-scenarios)
10. [Troubleshooting Common Issues](#troubleshooting-common-issues)

---

## 🎯 Prerequisites

Before starting the testing flow, ensure:

1. **Application is running** on `http://localhost:8081`
2. **Database is initialized** with equipment table created
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

### Verify Equipment Table Exists

If using Hibernate auto-create, the table will be created automatically. Otherwise, run the SQL script:

```sql
-- Run: src/main/resources/sql/create_equipment_table.sql
```

---

## ✅ Initial Setup Verification

### Step 1: Verify Equipment Endpoint is Available

**Request:**
```bash
GET http://localhost:8081/api/equipment
```

**Expected Response:** `200 OK`
```json
[]
```

If the endpoint returns an empty array, the endpoint is working correctly and ready for testing.

---

## 🖥️ Testing Equipment Creation

### Test Case 1: Create a Linux Server Equipment with Connection Info

**Request:**
```bash
POST http://localhost:8081/api/equipment
Content-Type: application/json

{
  "deviceName": "Linux Server 01",
  "hostName": "linux-server-01.example.com",
  "ipAddress": "192.168.1.100",
  "protocol": "ssh",
  "port": 22,
  "username": "admin",
  "password": "secretpassword",
  "deviceType": "LINUX_SERVER"
}
```

**Expected Response:** `201 Created`
```json
{
  "id": 1,
  "deviceName": "Linux Server 01",
  "hostName": "linux-server-01.example.com",
  "ipAddress": "192.168.1.100",
  "protocol": "ssh",
  "port": 22,
  "username": "admin",
  "deviceType": "LINUX_SERVER",
  "isDeleted": false,
  "createdAt": "2026-01-09T12:00:00",
  "updatedAt": "2026-01-09T12:00:00"
}
```

**Note:** The `password` field is hidden in the response for security reasons, but it is stored in the database.

**Verification:**
```bash
GET http://localhost:8081/api/equipment/1
```

### Test Case 2: Create a Database Equipment with Connection Info

**Request:**
```bash
POST http://localhost:8081/api/equipment
Content-Type: application/json

{
  "deviceName": "PostgreSQL Database 01",
  "hostName": "db-server-01.example.com",
  "ipAddress": "192.168.1.101",
  "protocol": "postgresql",
  "port": 5432,
  "username": "postgres",
  "password": "dbpassword123",
  "deviceType": "DATABASE"
}
```

**Expected Response:** `201 Created` with database equipment details (password hidden in response)

### Test Case 3: Create Equipment with Minimal Fields

**Request:**
```bash
POST http://localhost:8081/api/equipment
Content-Type: application/json

{
  "deviceName": "Minimal Device",
  "deviceType": "LINUX_SERVER"
}
```

**Expected Response:** `201 Created`
- `hostName`, `ipAddress`, `protocol`, `port`, `username`, and `password` can be null
- `deviceName` is required

### Test Case 4: Create Equipment - Duplicate Device Name (Should Fail)

**Request:**
```bash
POST http://localhost:8081/api/equipment
Content-Type: application/json

{
  "deviceName": "Linux Server 01",
  "hostName": "another-server.example.com",
  "ipAddress": "192.168.1.200",
  "deviceType": "LINUX_SERVER"
}
```

**Expected Response:** `400 Bad Request`
- Device name must be unique among non-deleted equipment

### Test Case 5: Create Equipment with IPv6 Address

**Request:**
```bash
POST http://localhost:8081/api/equipment
Content-Type: application/json

{
  "deviceName": "IPv6 Server",
  "hostName": "ipv6-server.example.com",
  "ipAddress": "2001:0db8:85a3:0000:0000:8a2e:0370:7334",
  "protocol": "ssh",
  "port": 22,
  "username": "root",
  "password": "password123",
  "deviceType": "LINUX_SERVER"
}
```

**Expected Response:** `201 Created`
- IPv6 addresses are supported (max 45 characters)

### Test Case 6: Create Equipment with RDP Protocol (Windows Server)

**Request:**
```bash
POST http://localhost:8081/api/equipment
Content-Type: application/json

{
  "deviceName": "Windows Server 01",
  "hostName": "windows-server-01.example.com",
  "ipAddress": "192.168.1.102",
  "protocol": "rdp",
  "port": 3389,
  "username": "administrator",
  "password": "windows123",
  "deviceType": "WINDOWS_SERVER"
}
```

**Expected Response:** `201 Created`
- RDP protocol with port 3389 (standard Windows Remote Desktop port)

### Test Case 7: Create Equipment - Password Security Test

**Request:**
```bash
POST http://localhost:8081/api/equipment
Content-Type: application/json

{
  "deviceName": "Secure Server",
  "hostName": "secure.example.com",
  "ipAddress": "192.168.1.103",
  "protocol": "ssh",
  "port": 22,
  "username": "user",
  "password": "very-secret-password-123",
  "deviceType": "LINUX_SERVER"
}
```

**Expected Response:** `201 Created`
```json
{
  "id": 3,
  "deviceName": "Secure Server",
  "hostName": "secure.example.com",
  "ipAddress": "192.168.1.103",
  "protocol": "ssh",
  "port": 22,
  "username": "user",
  "deviceType": "LINUX_SERVER",
  "isDeleted": false,
  "createdAt": "2026-01-09T12:00:00",
  "updatedAt": "2026-01-09T12:00:00"
}
```

**Note:** Password is NOT included in the response (hidden for security). Verify by checking the database directly if needed.

---

## 📖 Testing Equipment Retrieval

### Test Case 1: Get All Equipment

**Request:**
```bash
GET http://localhost:8081/api/equipment
```

**Expected Response:** `200 OK`
```json
[
  {
    "id": 1,
    "deviceName": "Linux Server 01",
    "hostName": "linux-server-01.example.com",
    "ipAddress": "192.168.1.100",
    "protocol": "ssh",
    "port": 22,
    "username": "admin",
    "deviceType": "LINUX_SERVER",
    "isDeleted": false,
    "createdAt": "2026-01-09T12:00:00",
    "updatedAt": "2026-01-09T12:00:00"
  },
  {
    "id": 2,
    "deviceName": "PostgreSQL Database 01",
    "hostName": "db-server-01.example.com",
    "ipAddress": "192.168.1.101",
    "protocol": "postgresql",
    "port": 5432,
    "username": "postgres",
    "deviceType": "DATABASE",
    "isDeleted": false,
    "createdAt": "2026-01-09T12:00:01",
    "updatedAt": "2026-01-09T12:00:01"
  }
]
```

**Note:** 
- Only non-deleted equipment is returned
- Password fields are hidden in responses for security

### Test Case 2: Get Equipment by ID

**Request:**
```bash
GET http://localhost:8081/api/equipment/1
```

**Expected Response:** `200 OK`
```json
{
  "id": 1,
  "deviceName": "Linux Server 01",
  "hostName": "linux-server-01.example.com",
  "ipAddress": "192.168.1.100",
  "protocol": "ssh",
  "port": 22,
  "username": "admin",
  "deviceType": "LINUX_SERVER",
  "isDeleted": false,
  "createdAt": "2026-01-09T12:00:00",
  "updatedAt": "2026-01-09T12:00:00"
}
```

**Note:** Password is not included in the response (hidden for security).

### Test Case 3: Get Equipment by ID - Not Found

**Request:**
```bash
GET http://localhost:8081/api/equipment/999
```

**Expected Response:** `404 Not Found`

### Test Case 4: Get Equipment by Device Name

**Request:**
```bash
GET http://localhost:8081/api/equipment/name/Linux Server 01
```

**Expected Response:** `200 OK` with equipment details

**Note:** URL encoding may be needed for device names with spaces:
```bash
GET http://localhost:8081/api/equipment/name/Linux%20Server%2001
```

### Test Case 5: Get Equipment by Device Name - Not Found

**Request:**
```bash
GET http://localhost:8081/api/equipment/name/NonExistentDevice
```

**Expected Response:** `404 Not Found`

---

## ✏️ Testing Equipment Update

### Test Case 1: Update Equipment Information

**Request:**
```bash
PUT http://localhost:8081/api/equipment/1
Content-Type: application/json

{
  "deviceName": "Linux Server 01 Updated",
  "hostName": "linux-server-01-updated.example.com",
  "ipAddress": "192.168.1.150",
  "protocol": "ssh",
  "port": 2222,
  "username": "newuser",
  "password": "newpassword123",
  "deviceType": "LINUX_SERVER"
}
```

**Expected Response:** `200 OK`
```json
{
  "id": 1,
  "deviceName": "Linux Server 01 Updated",
  "hostName": "linux-server-01-updated.example.com",
  "ipAddress": "192.168.1.150",
  "protocol": "ssh",
  "port": 2222,
  "username": "newuser",
  "deviceType": "LINUX_SERVER",
  "isDeleted": false,
  "createdAt": "2026-01-09T12:00:00",
  "updatedAt": "2026-01-09T12:05:00"
}
```

**Note:** 
- `updatedAt` timestamp should be updated, but `createdAt` remains unchanged
- Password is updated in database but hidden in response

### Test Case 2: Update Equipment - Partial Update

**Request:**
```bash
PUT http://localhost:8081/api/equipment/1
Content-Type: application/json

{
  "deviceName": "Linux Server 01",
  "hostName": "linux-server-01.example.com",
  "ipAddress": "192.168.1.200",
  "protocol": "ssh",
  "port": 22,
  "username": "admin",
  "deviceType": "LINUX_SERVER"
}
```

**Expected Response:** `200 OK`
- Only `ipAddress` is updated
- Other fields remain the same

### Test Case 5: Update Equipment - Change Connection Credentials

**Request:**
```bash
PUT http://localhost:8081/api/equipment/1
Content-Type: application/json

{
  "deviceName": "Linux Server 01",
  "hostName": "linux-server-01.example.com",
  "ipAddress": "192.168.1.100",
  "protocol": "ssh",
  "port": 22,
  "username": "updateduser",
  "password": "updatedpassword",
  "deviceType": "LINUX_SERVER"
}
```

**Expected Response:** `200 OK`
- Username and password are updated
- Password is stored but not returned in response

### Test Case 3: Update Equipment - Not Found

**Request:**
```bash
PUT http://localhost:8081/api/equipment/999
Content-Type: application/json

{
  "deviceName": "Updated Device",
  "deviceType": "LINUX_SERVER"
}
```

**Expected Response:** `404 Not Found`

### Test Case 4: Update Equipment - Change Device Type and Protocol

**Request:**
```bash
PUT http://localhost:8081/api/equipment/1
Content-Type: application/json

{
  "deviceName": "Linux Server 01",
  "hostName": "linux-server-01.example.com",
  "ipAddress": "192.168.1.100",
  "protocol": "rdp",
  "port": 3389,
  "username": "admin",
  "password": "password123",
  "deviceType": "WINDOWS_SERVER"
}
```

**Expected Response:** `200 OK`
- Device type and protocol successfully changed
- Protocol changed from "ssh" to "rdp"
- Port changed from 22 to 3389

---

## 🗑️ Testing Equipment Soft Delete

### Test Case 1: Soft Delete Equipment

**Request:**
```bash
DELETE http://localhost:8081/api/equipment/1
```

**Expected Response:** `200 OK`
```json
{
  "message": "Equipment soft deleted successfully",
  "id": "1"
}
```

**Verification:**
```bash
# Try to get the deleted equipment
GET http://localhost:8081/api/equipment/1
```

**Expected Response:** `404 Not Found`
- Deleted equipment is not returned in queries

**Verify it's not in the list:**
```bash
GET http://localhost:8081/api/equipment
```

**Expected Response:** Equipment with id=1 should not appear in the list.

### Test Case 2: Soft Delete Equipment - Already Deleted

**Request:**
```bash
DELETE http://localhost:8081/api/equipment/1
```

**Expected Response:** `404 Not Found`
- Cannot delete an already deleted equipment

### Test Case 3: Soft Delete Equipment - Not Found

**Request:**
```bash
DELETE http://localhost:8081/api/equipment/999
```

**Expected Response:** `404 Not Found`

### Test Case 4: Create Equipment with Same Name After Deletion

**Scenario:** After soft deleting "Linux Server 01", create a new equipment with the same name.

**Step 1: Delete existing equipment**
```bash
DELETE http://localhost:8081/api/equipment/1
```

**Step 2: Create new equipment with same name**
```bash
POST http://localhost:8081/api/equipment
Content-Type: application/json

{
  "deviceName": "Linux Server 01",
  "hostName": "new-linux-server.example.com",
  "ipAddress": "192.168.1.300",
  "protocol": "ssh",
  "port": 22,
  "username": "admin",
  "password": "newpassword",
  "deviceType": "LINUX_SERVER"
}
```

**Expected Response:** `201 Created`
- Since the old equipment is soft deleted, the name is available again

---

## 🔍 Testing Equipment by Type

### Test Case 1: Get All Linux Servers

**Request:**
```bash
GET http://localhost:8081/api/equipment/type/LINUX_SERVER
```

**Expected Response:** `200 OK`
```json
[
  {
    "id": 1,
    "deviceName": "Linux Server 01",
    "hostName": "linux-server-01.example.com",
    "ipAddress": "192.168.1.100",
    "protocol": "ssh",
    "port": 22,
    "username": "admin",
    "deviceType": "LINUX_SERVER",
    "isDeleted": false,
    "createdAt": "2026-01-09T12:00:00",
    "updatedAt": "2026-01-09T12:00:00"
  }
]
```

### Test Case 2: Get All Databases

**Request:**
```bash
GET http://localhost:8081/api/equipment/type/DATABASE
```

**Expected Response:** `200 OK` with all database equipment

### Test Case 3: Get Equipment by Type - No Results

**Request:**
```bash
GET http://localhost:8081/api/equipment/type/WINDOWS_SERVER
```

**Expected Response:** `200 OK`
```json
[]
```

### Test Case 4: Get Equipment by Type - Case Sensitive

**Request:**
```bash
GET http://localhost:8081/api/equipment/type/linux_server
```

**Expected Response:** `200 OK` with empty array or different results
- Type matching is case-sensitive: "LINUX_SERVER" ≠ "linux_server"

---

## 🔌 Testing Connection Fields

This section focuses specifically on testing the connection-related fields: `protocol`, `port`, `username`, and `password`.

### Test Case 1: Create Equipment with SSH Connection

**Request:**
```bash
POST http://localhost:8081/api/equipment
Content-Type: application/json

{
  "deviceName": "SSH Server",
  "hostName": "ssh-server.example.com",
  "ipAddress": "192.168.1.100",
  "protocol": "ssh",
  "port": 22,
  "username": "admin",
  "password": "admin123",
  "deviceType": "LINUX_SERVER"
}
```

**Expected Response:** `201 Created`
- Password is stored but not returned in response
- Verify password is saved by checking database directly if needed

### Test Case 2: Create Equipment with RDP Connection (Windows)

**Request:**
```bash
POST http://localhost:8081/api/equipment
Content-Type: application/json

{
  "deviceName": "Windows RDP Server",
  "hostName": "windows.example.com",
  "ipAddress": "192.168.1.200",
  "protocol": "rdp",
  "port": 3389,
  "username": "administrator",
  "password": "windows123",
  "deviceType": "WINDOWS_SERVER"
}
```

**Expected Response:** `201 Created`
- RDP protocol with standard port 3389

### Test Case 3: Create Equipment with Database Connection

**Request:**
```bash
POST http://localhost:8081/api/equipment
Content-Type: application/json

{
  "deviceName": "PostgreSQL DB",
  "hostName": "postgres.example.com",
  "ipAddress": "192.168.1.300",
  "protocol": "postgresql",
  "port": 5432,
  "username": "postgres",
  "password": "postgres123",
  "deviceType": "DATABASE"
}
```

**Expected Response:** `201 Created`
- Database connection with PostgreSQL protocol

### Test Case 4: Create Equipment with Custom Port

**Request:**
```bash
POST http://localhost:8081/api/equipment
Content-Type: application/json

{
  "deviceName": "Custom SSH Server",
  "hostName": "custom.example.com",
  "ipAddress": "192.168.1.400",
  "protocol": "ssh",
  "port": 2222,
  "username": "user",
  "password": "pass123",
  "deviceType": "LINUX_SERVER"
}
```

**Expected Response:** `201 Created`
- Custom SSH port (2222 instead of default 22)

### Test Case 5: Verify Password is Hidden in Response

**Step 1: Create equipment with password**
```bash
POST http://localhost:8081/api/equipment
{
  "deviceName": "Password Test",
  "protocol": "ssh",
  "port": 22,
  "username": "test",
  "password": "secretpassword123",
  "deviceType": "LINUX_SERVER"
}
```

**Step 2: Retrieve the equipment**
```bash
GET http://localhost:8081/api/equipment/{id}
```

**Expected Response:** `200 OK`
```json
{
  "id": 1,
  "deviceName": "Password Test",
  "protocol": "ssh",
  "port": 22,
  "username": "test",
  "deviceType": "LINUX_SERVER",
  "isDeleted": false,
  "createdAt": "2026-01-09T12:00:00",
  "updatedAt": "2026-01-09T12:00:00"
}
```

**Note:** Password field is NOT present in the response (hidden for security).

### Test Case 6: Update Connection Credentials

**Request:**
```bash
PUT http://localhost:8081/api/equipment/1
Content-Type: application/json

{
  "deviceName": "Updated Server",
  "hostName": "server.example.com",
  "ipAddress": "192.168.1.100",
  "protocol": "ssh",
  "port": 22,
  "username": "newuser",
  "password": "newpassword456",
  "deviceType": "LINUX_SERVER"
}
```

**Expected Response:** `200 OK`
- Username and password are updated
- Password is not returned in response

### Test Case 7: Update Only Protocol and Port

**Request:**
```bash
PUT http://localhost:8081/api/equipment/1
Content-Type: application/json

{
  "deviceName": "Linux Server 01",
  "hostName": "linux-server-01.example.com",
  "ipAddress": "192.168.1.100",
  "protocol": "rdp",
  "port": 3389,
  "username": "admin",
  "password": "password123",
  "deviceType": "LINUX_SERVER"
}
```

**Expected Response:** `200 OK`
- Protocol changed from "ssh" to "rdp"
- Port changed from 22 to 3389
- Other fields remain the same

### Test Case 8: Create Equipment Without Connection Info

**Request:**
```bash
POST http://localhost:8081/api/equipment
Content-Type: application/json

{
  "deviceName": "No Connection Info",
  "deviceType": "LINUX_SERVER"
}
```

**Expected Response:** `201 Created`
- All connection fields (protocol, port, username, password) can be null
- Equipment is created successfully

### Test Case 9: Different Protocol Types

Test creating equipment with various protocols:

**SSH:**
```json
{
  "deviceName": "SSH Server",
  "protocol": "ssh",
  "port": 22,
  "deviceType": "LINUX_SERVER"
}
```

**RDP:**
```json
{
  "deviceName": "RDP Server",
  "protocol": "rdp",
  "port": 3389,
  "deviceType": "WINDOWS_SERVER"
}
```

**PostgreSQL:**
```json
{
  "deviceName": "PostgreSQL DB",
  "protocol": "postgresql",
  "port": 5432,
  "deviceType": "DATABASE"
}
```

**MySQL:**
```json
{
  "deviceName": "MySQL DB",
  "protocol": "mysql",
  "port": 3306,
  "deviceType": "DATABASE"
}
```

**HTTP/HTTPS:**
```json
{
  "deviceName": "Web Server",
  "protocol": "https",
  "port": 443,
  "deviceType": "LINUX_SERVER"
}
```

---

## 🧪 Advanced Testing Scenarios

### Scenario 1: Complete Equipment Lifecycle

**Objective:** Test the complete lifecycle: Create → Read → Update → Delete → Recreate

**Steps:**

1. **Create Equipment:**
```bash
POST http://localhost:8081/api/equipment
{
  "deviceName": "Lifecycle Test Device",
  "hostName": "test.example.com",
  "ipAddress": "192.168.1.50",
  "protocol": "ssh",
  "port": 22,
  "username": "testuser",
  "password": "testpass",
  "deviceType": "LINUX_SERVER"
}
```
**Save the `id` from response (e.g., id=5)**

2. **Read Equipment:**
```bash
GET http://localhost:8081/api/equipment/5
```

3. **Update Equipment:**
```bash
PUT http://localhost:8081/api/equipment/5
{
  "deviceName": "Lifecycle Test Device Updated",
  "hostName": "test-updated.example.com",
  "ipAddress": "192.168.1.51",
  "protocol": "postgresql",
  "port": 5432,
  "username": "dbuser",
  "password": "dbpass",
  "deviceType": "DATABASE"
}
```

4. **Verify Update:**
```bash
GET http://localhost:8081/api/equipment/5
```

5. **Soft Delete:**
```bash
DELETE http://localhost:8081/api/equipment/5
```

6. **Verify Deletion:**
```bash
GET http://localhost:8081/api/equipment/5
# Should return 404
```

7. **Recreate with Same Name:**
```bash
POST http://localhost:8081/api/equipment
{
  "deviceName": "Lifecycle Test Device",
  "hostName": "test-new.example.com",
  "ipAddress": "192.168.1.52",
  "protocol": "ssh",
  "port": 22,
  "username": "newuser",
  "password": "newpass",
  "deviceType": "LINUX_SERVER"
}
```
**Expected:** Should succeed (old one is deleted)

### Scenario 2: Multiple Equipment Types

**Objective:** Test managing different types of equipment

**Steps:**

1. Create various equipment types:
```bash
# Linux Server
POST /api/equipment
{"deviceName": "Web Server 01", "deviceType": "LINUX_SERVER", ...}

# Database
POST /api/equipment
{"deviceName": "MySQL DB 01", "deviceType": "DATABASE", ...}

# Windows Server
POST /api/equipment
{"deviceName": "Windows Server 01", "deviceType": "WINDOWS_SERVER", ...}
```

2. Retrieve by type:
```bash
GET /api/equipment/type/LINUX_SERVER
GET /api/equipment/type/DATABASE
GET /api/equipment/type/WINDOWS_SERVER
```

3. Verify all equipment:
```bash
GET /api/equipment
```

### Scenario 3: Bulk Operations Simulation

**Objective:** Test creating and managing multiple equipment

**Steps:**

1. Create 5 different equipment:
```bash
POST /api/equipment {"deviceName": "Device 1", ...}
POST /api/equipment {"deviceName": "Device 2", ...}
POST /api/equipment {"deviceName": "Device 3", ...}
POST /api/equipment {"deviceName": "Device 4", ...}
POST /api/equipment {"deviceName": "Device 5", ...}
```

2. Get all equipment:
```bash
GET /api/equipment
```
**Expected:** Should return all 5 equipment

3. Delete 2 equipment:
```bash
DELETE /api/equipment/1
DELETE /api/equipment/3
```

4. Get all equipment again:
```bash
GET /api/equipment
```
**Expected:** Should return only 3 equipment (2, 4, 5)

### Scenario 4: Edge Cases

**Test Cases:**

1. **Empty Device Name:**
```bash
POST /api/equipment
{
  "deviceName": "",
  "deviceType": "LINUX_SERVER"
}
```
**Expected:** `400 Bad Request` or validation error

2. **Very Long Device Name:**
```bash
POST /api/equipment
{
  "deviceName": "A".repeat(201),  // Exceeds 200 char limit
  "deviceType": "LINUX_SERVER"
}
```
**Expected:** `400 Bad Request` or database error

3. **Invalid IP Address Format:**
```bash
POST /api/equipment
{
  "deviceName": "Test Device",
  "ipAddress": "999.999.999.999",
  "deviceType": "LINUX_SERVER"
}
```
**Expected:** `201 Created` (validation happens at application level if implemented)

4. **Null Values:**
```bash
POST /api/equipment
{
  "deviceName": "Test Device",
  "hostName": null,
  "ipAddress": null,
  "protocol": null,
  "port": null,
  "username": null,
  "password": null,
  "deviceType": null
}
```
**Expected:** `201 Created` (nullable fields are allowed)

5. **Update Only Password:**
```bash
PUT /api/equipment/1
{
  "deviceName": "Linux Server 01",
  "hostName": "linux-server-01.example.com",
  "ipAddress": "192.168.1.100",
  "protocol": "ssh",
  "port": 22,
  "username": "admin",
  "password": "newpassword123",
  "deviceType": "LINUX_SERVER"
}
```
**Expected:** `200 OK` - Password is updated but not returned in response

---

## 🔍 Troubleshooting Common Issues

### Issue 1: Equipment Not Appearing in List

**Symptoms:** Equipment created but not showing in `GET /api/equipment`

**Possible Causes:**
1. Equipment is soft deleted (`isDeleted = true`)
2. Database query issue
3. Equipment not actually saved

**Solution:**
1. Check database directly: `SELECT * FROM equipment WHERE id = ?`
2. Verify `isDeleted` field value
3. Check application logs for errors

### Issue 2: Cannot Create Equipment with Unique Name

**Symptoms:** `400 Bad Request` when creating equipment with unique name

**Possible Causes:**
1. Equipment with same name exists (even if deleted)
2. Database constraint issue

**Solution:**
1. Check if equipment exists: `GET /api/equipment/name/{deviceName}`
2. Verify soft delete is working correctly
3. Check database for existing records

### Issue 3: Update Not Working

**Symptoms:** Equipment update returns 200 but changes not saved

**Possible Causes:**
1. Equipment is soft deleted
2. Transaction not committed
3. Wrong ID used

**Solution:**
1. Verify equipment exists and is not deleted: `GET /api/equipment/{id}`
2. Check `updatedAt` timestamp changed
3. Verify all fields are being updated

### Issue 4: Soft Delete Not Working

**Symptoms:** Equipment still appears after deletion

**Possible Causes:**
1. Soft delete query not executing
2. Equipment already deleted
3. Cache issue

**Solution:**
1. Check database: `SELECT is_deleted FROM equipment WHERE id = ?`
2. Verify soft delete endpoint returns 200 OK
3. Clear any caches and retry

### Issue 5: Equipment by Type Returns Wrong Results

**Symptoms:** Type filter returns incorrect equipment

**Possible Causes:**
1. Case sensitivity issue
2. Equipment is deleted
3. Type value mismatch

**Solution:**
1. Verify exact type value (case-sensitive)
2. Check equipment is not deleted
3. Use exact type string from database

---

## 📊 Testing Checklist

Use this checklist to ensure comprehensive testing:

### Equipment Creation
- [ ] Create equipment with all fields (including connection info)
- [ ] Create equipment with minimal fields
- [ ] Create equipment with duplicate name (should fail)
- [ ] Create equipment with IPv6 address
- [ ] Create equipment with null optional fields
- [ ] Create equipment with different protocols (ssh, rdp, postgresql, mysql)
- [ ] Create equipment with different ports (22, 3389, 5432, 3306)
- [ ] Create equipment with custom ports
- [ ] Verify password is hidden in response
- [ ] Create equipment without connection info

### Equipment Retrieval
- [ ] Get all equipment
- [ ] Get equipment by ID (existing)
- [ ] Get equipment by ID (non-existent)
- [ ] Get equipment by device name
- [ ] Get equipment by type
- [ ] Verify deleted equipment not returned

### Equipment Update
- [ ] Update all fields (including connection info)
- [ ] Update partial fields
- [ ] Update connection credentials (username, password)
- [ ] Update protocol and port
- [ ] Update non-existent equipment (should fail)
- [ ] Update deleted equipment (should fail)
- [ ] Verify `updatedAt` timestamp changes
- [ ] Verify `createdAt` timestamp unchanged
- [ ] Verify password is updated but hidden in response

### Equipment Soft Delete
- [ ] Soft delete existing equipment
- [ ] Soft delete non-existent equipment (should fail)
- [ ] Soft delete already deleted equipment (should fail)
- [ ] Verify deleted equipment not in list
- [ ] Verify deleted equipment not accessible by ID
- [ ] Create new equipment with same name after deletion

### Equipment by Type
- [ ] Get equipment by LINUX_SERVER type
- [ ] Get equipment by DATABASE type
- [ ] Get equipment by WINDOWS_SERVER type
- [ ] Get equipment by non-existent type
- [ ] Verify case sensitivity

### Edge Cases
- [ ] Empty device name
- [ ] Very long device name
- [ ] Invalid IP format
- [ ] Null values for optional fields
- [ ] Special characters in device name
- [ ] Invalid port numbers (negative, zero, too large > 65535)
- [ ] Very long passwords
- [ ] Special characters in username/password
- [ ] Empty protocol string
- [ ] Very long protocol name
- [ ] Update password only (without other fields)

---

## 🎓 Best Practices

1. **Always verify soft delete** - Check that deleted equipment doesn't appear in queries
2. **Test uniqueness constraints** - Verify device name uniqueness works correctly
3. **Check timestamps** - Verify `createdAt` and `updatedAt` are managed correctly
4. **Test with different device types** - Ensure type filtering works for all types
5. **Use Swagger UI** - Interactive testing is easier with Swagger
6. **Clean up test data** - Delete test equipment after testing
7. **Test error cases** - Don't just test happy paths

---

## 📝 Notes

- **Equipment and Device are the same** - The terms are interchangeable in this system
- **Soft delete** - Equipment is marked as deleted but remains in database
- **Device name uniqueness** - Only enforced for non-deleted equipment
- **Timestamps** - Automatically managed by JPA `@PrePersist` and `@PreUpdate`
- **Device types** - Common types: `LINUX_SERVER`, `DATABASE`, `WINDOWS_SERVER`
- **IP addresses** - Supports both IPv4 and IPv6 (max 45 characters)
- **Connection fields** - `protocol`, `port`, `username`, and `password` are optional
- **Password security** - Password is stored in database but hidden in all API responses using `@JsonIgnore`
- **Protocol examples** - Common protocols: `ssh` (port 22), `rdp` (port 3389), `postgresql` (port 5432), `mysql` (port 3306)
- **Port numbers** - Standard ports: SSH (22), RDP (3389), HTTP (80), HTTPS (443), PostgreSQL (5432), MySQL (3306)

---

## 🔗 Related Documentation

- [TESTING-FLOW.md](TESTING-FLOW.md) - General testing flow for the system
- [README.md](README.md) - Project overview and setup
- Equipment Entity: `src/main/java/com/hunesion/drool_v2/entity/Equipment.java`
- Equipment Controller: `src/main/java/com/hunesion/drool_v2/controller/EquipmentController.java`

---

## 📋 Example Test Data

Here are some example equipment you can use for testing:

```json
// Linux Server with SSH
{
  "deviceName": "Production Web Server",
  "hostName": "web-prod-01.example.com",
  "ipAddress": "192.168.1.10",
  "protocol": "ssh",
  "port": 22,
  "username": "admin",
  "password": "securepassword123",
  "deviceType": "LINUX_SERVER"
}

// Database Server (PostgreSQL)
{
  "deviceName": "PostgreSQL Primary",
  "hostName": "db-primary.example.com",
  "ipAddress": "192.168.1.20",
  "protocol": "postgresql",
  "port": 5432,
  "username": "postgres",
  "password": "dbpassword",
  "deviceType": "DATABASE"
}

// Windows Server with RDP
{
  "deviceName": "Windows File Server",
  "hostName": "fileserver.example.com",
  "ipAddress": "192.168.1.30",
  "protocol": "rdp",
  "port": 3389,
  "username": "administrator",
  "password": "windows123",
  "deviceType": "WINDOWS_SERVER"
}

// MySQL Database
{
  "deviceName": "MySQL Database Server",
  "hostName": "mysql-db.example.com",
  "ipAddress": "192.168.1.40",
  "protocol": "mysql",
  "port": 3306,
  "username": "root",
  "password": "mysqlpass",
  "deviceType": "DATABASE"
}

// Minimal Equipment (no connection info)
{
  "deviceName": "Test Device",
  "deviceType": "LINUX_SERVER"
}

// Equipment with Custom Port
{
  "deviceName": "Custom SSH Server",
  "hostName": "custom.example.com",
  "ipAddress": "192.168.1.50",
  "protocol": "ssh",
  "port": 2222,
  "username": "user",
  "password": "pass123",
  "deviceType": "LINUX_SERVER"
}
```

**Note:** Remember that `password` fields will be hidden in all API responses for security.

---

**Last Updated:** Generated automatically  
**Version:** 1.0.0
