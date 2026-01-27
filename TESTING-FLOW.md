## Equipment Policy Testing Flow – SSH Access on Assigned Linux Server

This guide tests that the endpoint  
`GET /api/equipment-access/ssh`  
correctly allows or denies SSH access **based on EquipmentPolicy assignments**.

It uses the default data from `initial_data_sql_v2.sql`.

---

### 1. Prerequisites

- Run all DB migrations (Flyway).
- Execute `src/main/resources/sql/initial_data_sql_v2.sql` on your PostgreSQL DB.
- Start the Spring Boot app.

Verify core data:

```sql
SELECT id, username, department, level FROM users ORDER BY id;
SELECT id, device_name, device_type, protocol, ip_address FROM equipment ORDER BY id;
SELECT id, policy_name, enabled, priority FROM equipment_policies ORDER BY priority DESC;
```

Key records used in this flow:

- **User**: `bob` (IT team member, USER role)
- **Equipment**: `Linux Production Server` (`device_type = 'LINUX_SERVER'`, `protocol = 'SSH'`)
- **Policies**:
  - `IT Team SSH Access - Common Settings` (type `commonSettings`, allows `SSH` / `TELNET`)
  - `Business Hours Access - Time Restrictions` (type `allowedTime`)
  - `Office IP Whitelist - Login Control` (type `loginControl`)

Assignments from `initial_data_sql_v2.sql`:

```sql
-- User ↔ Policy
SELECT u.username, ep.policy_name
FROM policy_user_assignments pua
JOIN users u ON pua.user_id = u.id
JOIN equipment_policies ep ON pua.policy_id = ep.id;

-- UserGroup ↔ Policy
SELECT ug.group_name, ep.policy_name
FROM policy_user_group_assignments puga
JOIN user_groups ug ON puga.group_id = ug.id
JOIN equipment_policies ep ON puga.policy_id = ep.id;

-- Equipment ↔ Policy
SELECT e.device_name, ep.policy_name
FROM policy_equipment_assignments pea
JOIN equipment e ON pea.equipment_id = e.id
JOIN equipment_policies ep ON pea.policy_id = ep.id;
```

You should see at least:

- `IT Team SSH Access - Common Settings` assigned to:
  - user `bob`
  - group `IT Team`
- `Linux Production Server` assigned to:
  - `Block Dangerous Commands - Command Control` (command blacklist)
- Policy groups linking these together (see the script for details).

---

### 2. Endpoint Under Test

Controller: `EquipmentAccessController`

```java
@Operation(
        summary = "User ssh on their equipment (Linux Server) that has been assigned by admin",
        description = "Evaluates if a user has SSH access to a specific equipment based on equipment policies and assignments"
)
@GetMapping("/ssh")
public ResponseEntity<EquipmentAccessResponseDTO> sshToTargetEquipment(
        @RequestParam("username") String username,
        @RequestParam("equipmentId") Long equipmentId,
        @RequestParam(value = "clientIp", required = false) String clientIp
) {
    EquipmentAccessRequestDTO request = new EquipmentAccessRequestDTO();
    request.setUsername(username);
    request.setEquipmentId(equipmentId);
    request.setProtocol("SSH");
    request.setClientIp(clientIp);

    EquipmentAccessResponseDTO response = accessControlService.checkAccess(request);
    return ResponseEntity.ok(response);
}
```

Request parameters:

- **username**: application user (e.g. `bob`)
- **equipmentId**: ID from `equipment` table
- **clientIp** (optional): client IP, used by `loginControl` policies

Response body (`EquipmentAccessResponseDTO`):

- `allowed` (boolean)
- `matchedPolicyName` (string)
- `denialReason` (string)
- `denialCode` (string)

---

### 3. Test Case 1 – IT User Can SSH to Assigned Linux Server

**Goal**: `bob` **is allowed** to SSH to `Linux Production Server` because:

- He is in `IT Team`
- `IT Team SSH Access - Common Settings` allows `SSH`
- Policy is assigned to IT user/group and to the Linux server (directly or via policy groups)

#### 3.1 Find `bob` and Linux server IDs

```sql
SELECT id, username FROM users WHERE username = 'bob';
SELECT id, device_name FROM equipment WHERE device_name = 'Linux Production Server';
```

Assume:

- `bob` → `userId = 5`
- `Linux Production Server` → `equipmentId = 1`

#### 3.2 Call SSH endpoint

```bash
curl -X GET "http://localhost:8081/api/equipment-access/ssh?username=bob&equipmentId=1&clientIp=192.168.1.50"
```

**Expected response (example):**

```json
{
  "allowed": true,
  "matchedPolicyName": "IT Team SSH Access - Common Settings",
  "denialReason": null,
  "denialCode": null
}
```

If you call without `clientIp`, the IP whitelist policy (`Office IP Whitelist - Login Control`) may not apply; the SSH policy should still be enough to allow access if no stricter loginControl policy is matched.

---

### 4. Test Case 2 – Non-IT User Denied SSH on Linux Server

**Goal**: `alice` **is denied** SSH on `Linux Production Server` because she does not have the IT policy assigned (or only has DB read-only policies).

#### 4.1 Verify `alice` assignments

```sql
SELECT id, username, department FROM users WHERE username = 'alice';

SELECT ep.policy_name
FROM policy_user_assignments pua
JOIN users u ON pua.user_id = u.id
JOIN equipment_policies ep ON pua.policy_id = ep.id
WHERE u.username = 'alice';
```

#### 4.2 Call SSH endpoint

```bash
curl -X GET "http://localhost:8081/api/equipment-access/ssh?username=alice&equipmentId=1&clientIp=192.168.1.50"
```

**Expected response (example):**

```json
{
  "allowed": false,
  "matchedPolicyName": null,
  "denialReason": "No matching policy found for this equipment access",
  "denialCode": "No Policy Match"
}
```

If a different deny reason appears, use it to refine policy assignments (for example, IP whitelist denying access).

---

### 5. Test Case 3 – IP Whitelist Denies SSH from External IP

**Goal**: `bob` is **denied** when accessing from an IP outside office ranges, due to `Office IP Whitelist - Login Control`.

Recall IP whitelist policy:

```json
{
  "loginControl": {
    "allowedIps": [
      "192.168.1.0/24",
      "10.0.0.0/8"
    ],
    "twoFactorType": "none",
    "ipFilteringType": "allow_specified_ips",
    "accountLockEnabled": false,
    "maxFailureAttempts": 0,
    "lockoutDurationMinutes": 0
  }
}
```

#### 5.1 Call SSH endpoint from non-office IP

```bash
curl -X GET "http://localhost:8081/api/equipment-access/ssh?username=bob&equipmentId=1&clientIp=203.0.113.10"
```

**Expected response (example):**

```json
{
  "allowed": false,
  "matchedPolicyName": "Office IP Whitelist - Login Control",
  "denialReason": "IP not allowed by login control policy",
  "denialCode": "IP_NOT_ALLOWED"
}
```

The exact `denialReason`/`denialCode` depends on your Drools rule implementation, but `allowed` should be `false` and the matched policy should indicate the login control.

---

### 6. Test Case 4 – Time-Based Deny Outside Business Hours

**Goal**: A user with valid assignments is **denied** when request time is outside `Business Hours Access - Time Restrictions`.

#### 6.1 Simulate off-hours request

Pick a Sunday at 03:00 (day 7, hour 3). Use the body-based `/check` endpoint to control `requestTime`:

```bash
curl -X POST "http://localhost:8081/api/equipment-access/check" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "bob",
    "equipmentId": 1,
    "protocol": "SSH",
    "clientIp": "192.168.1.50",
    "requestTime": "2026-01-25T03:00:00"
  }'
```

**Expected response:**

- `allowed = false`
- `matchedPolicyName` may remain null (if time check is global), or may indicate the time policy.

---

### 7. Troubleshooting

If results are not as expected:

- Verify policies and assignments:

```sql
SELECT id, policy_name, enabled, priority FROM equipment_policies ORDER BY priority DESC;

SELECT * FROM policy_user_assignments;
SELECT * FROM policy_user_group_assignments;
SELECT * FROM policy_equipment_assignments;
SELECT * FROM policy_role_assignments;
```

- Check application logs for:
  - `Loaded equipment policies from database`
  - `Equipment access rules fired: N for user: ...`
  - Generated DRL for each policy (from `EquipmentPolicyRuleGenerator`).

If you need a new scenario (e.g. different user/equipment combination), you can:

- Create a new `EquipmentPolicy` via `POST /api/equipment-policies`
- Assign it using `EquipmentPolicyAssignmentController` endpoints:
  - `POST /api/equipment-policies/{policyId}/assignments/users`
  - `POST /api/equipment-policies/{policyId}/assignments/equipment`

