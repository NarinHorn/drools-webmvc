# ABAC Policy Management System with Drools - v2

A comprehensive **Attribute-Based Access Control (ABAC)** system built with Spring Boot, Drools, and PostgreSQL. This system allows dynamic policy management where access control rules are stored in the database and evaluated in real-time using the Drools rule engine.

---

## 📋 Table of Contents

1. [Project Overview](#project-overview)
2. [Technology Stack](#technology-stack)
3. [Architecture & Flow](#architecture--flow)
4. [Prerequisites](#prerequisites)
5. [Setup Instructions](#setup-instructions)
6. [Database Setup](#database-setup)
7. [Configuration](#configuration)
8. [Running the Application](#running-the-application)
9. [API Documentation](#api-documentation)
10. [Testing Flow](#testing-flow)
11. [Development Workflow](#development-workflow)
12. [Troubleshooting](#troubleshooting)

---

## 🎯 Project Overview

This project implements a flexible ABAC (Attribute-Based Access Control) system that:

- **Dynamically manages access policies** stored in PostgreSQL database
- **Evaluates access in real-time** using Drools rule engine
- **Supports complex conditions** based on user attributes (roles, department, level, custom attributes)
- **Hot-reloads rules** when policies are created, updated, or deleted
- **Provides RESTful API** for policy management and access control

### Key Features

- ✅ Dynamic policy creation via REST API
- ✅ Policy-to-DRL (Drools Rule Language) automatic conversion
- ✅ Role-based and attribute-based access control
- ✅ Request interception and automatic access evaluation
- ✅ Swagger UI for API testing
- ✅ PostgreSQL persistence with JPA
- ✅ Gradle build system

---

## 🛠 Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Build Tool** | Gradle | 8.14.2 |
| **Framework** | Spring Boot | 4.0.1 |
| **Java** | JDK | 21 |
| **Database** | PostgreSQL | Latest |
| **Rule Engine** | Drools | 8.44.0.Final |
| **API Documentation** | SpringDoc OpenAPI | 2.7.0 |
| **ORM** | Spring Data JPA / Hibernate | (via Spring Boot) |

---

## 🏗 Architecture & Flow

### System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Client Application                              │
│                    (Postman, Swagger UI, Frontend)                      │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │ HTTP Request
                               │ Header: X-Username
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    AccessControlInterceptor                             │
│  • Intercepts all /api/** requests                                     │
│  • Extracts username from X-Username header                             │
│  • Builds AccessRequest with user context                               │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    AccessControlService                                 │
│  • Loads user from database                                             │
│  • Builds AccessRequest (roles, department, level, attributes)          │
│  • Evaluates access via Drools                                          │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    DynamicRuleService                                    │
│  • Manages KieContainer (Drools container)                             │
│  • Loads static rules from classpath                                    │
│  • Loads dynamic policies from PostgreSQL                               │
│  • Hot-reloads when policies change                                     │
│  • Creates KieSession for rule evaluation                                │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         PostgreSQL Database                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────────────────────────┐   │
│  │  users   │  │  roles   │  │      access_policies                 │   │
│  │          │  │          │  │  • policy_name                       │   │
│  │  • id    │  │  • id    │  │  • endpoint, http_method            │   │
│  │  • username│  • name  │  │  • allowed_roles (JSON)             │   │
│  │  • email │  │  • desc  │  │  • conditions (JSON)                │   │
│  │  • dept  │  │          │  │  • generated_drl (TEXT)             │   │
│  │  • level │  └──────────┘  │  • priority, enabled                │   │
│  │  • roles │                 └──────────────────────────────────────┘   │
│  └──────────┘                                                              │
│       │                                                                    │
│       └─── user_roles (Many-to-Many)                                      │
└─────────────────────────────────────────────────────────────────────────┘
```

### Request Flow Diagram

```
1. HTTP Request arrives
   ↓
2. AccessControlInterceptor intercepts /api/** requests
   ↓
3. Extracts X-Username header
   ↓
4. AccessControlService.checkAccess(username, endpoint, method)
   ↓
5. Loads User from database (with roles, department, level)
   ↓
6. Builds AccessRequest fact:
   - username
   - userRoles (Set<String>)
   - endpoint
   - httpMethod
   - department
   - userLevel
   - attributes (Map)
   ↓
7. DynamicRuleService.newKieSession()
   ↓
8. Insert AccessRequest and AccessResult into Drools session
   ↓
9. fireAllRules() - Drools evaluates all policies
   ↓
10. Matching policy sets AccessResult.allow() or deny()
   ↓
11. If no policy matches → Default Deny All rule fires
   ↓
12. Return AccessResult to interceptor
   ↓
13. Allow (200) or Deny (403) response
```

### Policy Evaluation Flow

```
Policy in Database
   ↓
PolicyService.generateDrl() converts to DRL
   ↓
Stored in access_policies.generated_drl
   ↓
DynamicRuleService.loadDynamicRulesFromDatabase()
   ↓
Combines all enabled policies into one DRL file
   ↓
Builds KieContainer (compiles rules)
   ↓
On request: Creates KieSession
   ↓
Inserts AccessRequest fact
   ↓
Drools pattern matching:
   - endpointMatches(pattern)
   - httpMethod == "GET"
   - hasRole("ADMIN")
   - department == "SALES"
   - userLevel >= 5
   ↓
If all conditions match → Rule fires
   ↓
AccessResult.allow() or deny()
```

---

## 📦 Prerequisites

Before setting up the project, ensure you have:

- **Java 21** or higher
- **PostgreSQL** (latest version recommended)
- **Gradle 8.14+** (or use Gradle Wrapper)
- **IDE** (IntelliJ IDEA, Eclipse, VS Code) - optional but recommended

### Verify Prerequisites

```bash
# Check Java version
java -version
# Should show: openjdk version "21" or higher

# Check Gradle (if installed globally)
gradle -v
# Or use wrapper: ./gradlew -v

# Check PostgreSQL
psql --version
```

---

## 🚀 Setup Instructions

### Step 1: Clone/Download the Project

```bash
cd drool_v3_gradle
```

### Step 2: Create PostgreSQL Database

```sql
-- Connect to PostgreSQL as superuser
psql -U postgres

-- Create database
CREATE DATABASE abacdb;

-- Verify creation
\l
```

### Step 3: Configure Database Connection

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/abacdb
    username: postgres          # Change if needed
    password: your_password      # Change to your PostgreSQL password
```

### Step 4: Build the Project

```bash
# Using Gradle Wrapper (recommended)
./gradlew build

# Or if you have Gradle installed globally
gradle build
```

### Step 5: Run the Application

```bash
# Using Gradle Wrapper
./gradlew bootRun

# Or build JAR and run
./gradlew build
java -jar build/libs/drool_v2-0.0.1-SNAPSHOT.jar
```

The application will start on: **http://localhost:8081**

### Step 6: Initialize Database Schema

The first time you run the application, JPA will automatically create tables based on entities:

- `users`
- `roles`
- `user_roles` (join table)
- `user_attributes` (join table)
- `access_policies`

**Note:** `ddl-auto: update` means tables are created/updated automatically.

---

## 💾 Database Setup

### Option 1: Manual SQL Insertion (Recommended)

After the first run creates the schema, insert initial data using the SQL script:

1. **Run the diagnostic script first** (optional, to check current state):
   ```bash
   psql -U postgres -d abacdb -f diagnose_issue.sql
   ```

2. **Insert initial data**:
   ```bash
   psql -U postgres -d abacdb -f initial_data.sql
   ```

   Or manually in psql:
   ```sql
   \i initial_data.sql
   ```

### Option 2: Enable DataInitializer (Not Recommended for Production)

If you want to use the Java initializer (for development only):

1. Edit `src/main/java/com/hunesion/drool_v2/runner/DataInitializer.java`
2. Uncomment the `@Component` and `@Order(1)` annotations
3. Restart the application

**⚠️ Warning:** This will try to create data on every startup. Use manual SQL for production.

### Initial Data Includes

- **4 Roles**: ADMIN, MANAGER, USER, VIEWER
- **5 Users**: admin, manager, john, jane, viewer
- **6 Access Policies**: Pre-configured policies for testing

### Sample Users

| Username | Password | Roles | Department | Level |
|----------|----------|-------|------------|-------|
| admin | admin123 | ADMIN | IT | 10 |
| manager | manager123 | MANAGER | SALES | 5 |
| john | john123 | USER | SALES | 3 |
| jane | jane123 | USER | HR | 3 |
| viewer | viewer123 | VIEWER | GUEST | 1 |

---

## ⚙️ Configuration

### Application Configuration (`application.yml`)

```yaml
spring:
  application:
    name: drool_v2
  
  # PostgreSQL Database Configuration
  datasource:
    url: jdbc:postgresql://localhost:5432/abacdb
    username: postgres
    password: your_password
    driver-class-name: org.postgresql.Driver
  
  # JPA/Hibernate Configuration
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update  # Options: none, validate, update, create, create-drop
    show-sql: false    # Set to true for SQL debugging
    properties:
      hibernate:
        format_sql: true

server:
  port: 8081
```

### Configuration Options

| Property | Description | Default |
|----------|-------------|---------|
| `spring.jpa.hibernate.ddl-auto` | Schema management | `update` |
| `spring.jpa.show-sql` | Log SQL queries | `false` |
| `server.port` | Application port | `8081` |

### Build Configuration (`build.gradle`)

Key dependencies:
- **Spring Boot**: 4.0.1
- **Drools**: 8.44.0.Final
- **PostgreSQL Driver**: Latest
- **SpringDoc OpenAPI**: 2.7.0

---

## 🏃 Running the Application

### Development Mode

```bash
# Using Gradle Wrapper
./gradlew bootRun

# With debug logging
./gradlew bootRun --debug
```

### Production Mode

```bash
# Build JAR
./gradlew build

# Run JAR
java -jar build/libs/drool_v2-0.0.1-SNAPSHOT.jar

# With custom profile
java -jar build/libs/drool_v2-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Verify Application is Running

1. **Check health**: Open http://localhost:8081/actuator/health (if actuator is enabled)
2. **Check Swagger UI**: Open http://localhost:8081/swagger-ui.html
3. **Check API**: `curl http://localhost:8081/api/public`

---

## 📚 API Documentation

### Swagger UI

Access interactive API documentation at:
**http://localhost:8081/swagger-ui.html**

### API Endpoints Overview

#### 1. Policy Management (`/api/policies`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|----------------|
| GET | `/api/policies` | Get all policies | Yes (X-Username) |
| GET | `/api/policies/enabled` | Get enabled policies | Yes |
| GET | `/api/policies/{id}` | Get policy by ID | Yes |
| POST | `/api/policies` | Create new policy | Yes (ADMIN) |
| PUT | `/api/policies/{id}` | Update policy | Yes (ADMIN) |
| PATCH | `/api/policies/{id}/toggle` | Enable/disable policy | Yes (ADMIN) |
| DELETE | `/api/policies/{id}` | Delete policy | Yes (ADMIN) |
| POST | `/api/policies/preview-drl` | Preview DRL without saving | Yes (ADMIN) |

#### 2. User Management (`/api/users`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|----------------|
| GET | `/api/users` | Get all users | Yes |
| GET | `/api/users/{id}` | Get user by ID | Yes |
| GET | `/api/users/username/{username}` | Get user by username | Yes |
| POST | `/api/users` | Create user | Yes |
| PUT | `/api/users/{id}` | Update user | Yes |
| POST | `/api/users/{userId}/roles/{roleName}` | Add role to user | Yes |
| DELETE | `/api/users/{userId}/roles/{roleName}` | Remove role from user | Yes |
| DELETE | `/api/users/{id}` | Delete user | Yes |

#### 3. Role Management (`/api/roles`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|----------------|
| GET | `/api/roles` | Get all roles | Yes |
| GET | `/api/roles/{id}` | Get role by ID | Yes |
| POST | `/api/roles` | Create role | Yes |
| PUT | `/api/roles/{id}` | Update role | Yes |
| DELETE | `/api/roles/{id}` | Delete role | Yes |

#### 4. Access Control (`/api/access`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|----------------|
| GET | `/api/access/check` | Check if user has access | Yes |

**Query Parameters:**
- `username`: Username to check
- `endpoint`: Endpoint path (e.g., `/api/reports`)
- `method`: HTTP method (GET, POST, etc.)

#### 5. Sample Protected Endpoints (`/api/*`)

These endpoints are protected by the interceptor:

- `/api/reports` - Reports endpoint
- `/api/sales` - Sales data endpoint
- `/api/management` - Management endpoint
- `/api/profile` - User profile endpoint
- `/api/public` - Public data endpoint

### Authentication

Currently, the system uses a simple header-based authentication:

**Header:** `X-Username: <username>`

Example:
```bash
curl -X GET http://localhost:8081/api/users \
  -H "X-Username: admin"
```

**Note:** In production, replace this with proper authentication (JWT, OAuth2, etc.).

---

## 🧪 Testing Flow

### Step-by-Step Testing Guide

### 1. Verify Application is Running

```bash
# Check if application is up
curl http://localhost:8081/api/public

# Expected: {"message": "This is public data"}
```

### 2. Access Swagger UI

1. Open browser: http://localhost:8081/swagger-ui.html
2. Explore available endpoints
3. Use "Try it out" to test APIs

### 3. Test User Authentication

```bash
# Get all users (as admin)
curl -X GET http://localhost:8081/api/users \
  -H "X-Username: admin"

# Expected: JSON array with 5 users
```

### 4. Test Access Control - Basic

#### 4.1 Admin Access (Should Succeed)

```bash
# Admin accessing reports
curl -X GET http://localhost:8081/api/reports \
  -H "X-Username: admin"

# Expected: 200 OK
```

#### 4.2 Regular User Access (Should Fail)

```bash
# John (USER role) accessing reports
curl -X GET http://localhost:8081/api/reports \
  -H "X-Username: john"

# Expected: 403 Forbidden
# Response: {"error": "No matching policy found", "status": 403}
```

### 5. Test Access Check API

```bash
# Check if admin can access reports
curl -X GET "http://localhost:8081/api/access/check?username=admin&endpoint=/api/reports&method=GET" \
  -H "X-Username: admin"

# Expected: {"allowed": true, "policyName": "Admin Full Access"}

# Check if john can access reports
curl -X GET "http://localhost:8081/api/access/check?username=john&endpoint=/api/reports&method=GET" \
  -H "X-Username: admin"

# Expected: {"allowed": false, "denialReason": "..."}
```

### 6. Test Department-Based Access

```bash
# John (SALES dept) accessing sales data (should succeed)
curl -X GET http://localhost:8081/api/sales \
  -H "X-Username: john"

# Expected: 200 OK

# Jane (HR dept) accessing sales data (should fail)
curl -X GET http://localhost:8081/api/sales \
  -H "X-Username: jane"

# Expected: 403 Forbidden
```

### 7. Test Level-Based Access

```bash
# Manager (level 5) accessing management (should succeed)
curl -X GET http://localhost:8081/api/management \
  -H "X-Username: manager"

# Expected: 200 OK

# John (level 3) accessing management (should fail)
curl -X GET http://localhost:8081/api/management \
  -H "X-Username: john"

# Expected: 403 Forbidden
```

### 8. Test Policy Management

#### 8.1 Get All Policies

```bash
curl -X GET http://localhost:8081/api/policies \
  -H "X-Username: admin"
```

#### 8.2 Create New Policy

```bash
curl -X POST http://localhost:8081/api/policies \
  -H "Content-Type: application/json" \
  -H "X-Username: admin" \
  -d '{
    "policyName": "HR Department Access",
    "description": "HR users can access HR endpoints",
    "endpoint": "/api/hr/**",
    "httpMethod": "GET",
    "allowedRoles": ["USER", "MANAGER"],
    "conditions": {
      "department": {"operator": "equals", "value": "HR"}
    },
    "effect": "ALLOW",
    "priority": 45
  }'
```

#### 8.3 Preview DRL (Without Saving)

```bash
curl -X POST http://localhost:8081/api/policies/preview-drl \
  -H "Content-Type: application/json" \
  -H "X-Username: admin" \
  -d '{
    "policyName": "Test Policy",
    "endpoint": "/api/test/**",
    "httpMethod": "GET",
    "allowedRoles": ["USER"],
    "effect": "ALLOW"
  }'
```

### 9. Complete Test Matrix

| Endpoint | User | Expected Result | Reason |
|----------|------|-----------------|--------|
| `/api/users` | admin | ✅ 200 OK | Admin has full access |
| `/api/users` | john | ✅ 200 OK | User Profile Access policy |
| `/api/reports` | admin | ✅ 200 OK | Admin has full access |
| `/api/reports` | manager | ✅ 200 OK | Manager Reports Access |
| `/api/reports` | john | ❌ 403 Forbidden | Not MANAGER role |
| `/api/sales` | john | ✅ 200 OK | SALES department |
| `/api/sales` | jane | ❌ 403 Forbidden | HR department (not SALES) |
| `/api/management` | manager | ✅ 200 OK | Level 5+ |
| `/api/management` | john | ❌ 403 Forbidden | Level 3 (< 5) |
| `/api/profile` | viewer | ✅ 200 OK | All authenticated users |
| `/api/public` | viewer | ✅ 200 OK | Public data access |

---

## 🔄 Development Workflow

### Project Structure

```
drool_v3_gradle/
├── build.gradle                 # Gradle build configuration
├── settings.gradle             # Gradle settings
├── gradlew                     # Gradle wrapper (Unix)
├── gradlew.bat                 # Gradle wrapper (Windows)
├── gradle/                     # Gradle wrapper files
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── src/
│   ├── main/
│   │   ├── java/com/hunesion/drool_v2/
│   │   │   ├── config/         # Configuration classes
│   │   │   │   ├── DroolsConfig.java
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   └── WebConfig.java
│   │   │   ├── controller/     # REST controllers
│   │   │   │   ├── AccessCheckController.java
│   │   │   │   ├── PolicyController.java
│   │   │   │   ├── UserController.java
│   │   │   │   └── ...
│   │   │   ├── dto/            # Data Transfer Objects
│   │   │   │   ├── ConditionDTO.java
│   │   │   │   └── PolicyDTO.java
│   │   │   ├── entity/         # JPA entities
│   │   │   │   ├── AccessPolicy.java
│   │   │   │   ├── Role.java
│   │   │   │   └── User.java
│   │   │   ├── interceptor/    # Request interceptors
│   │   │   │   └── AccessControlInterceptor.java
│   │   │   ├── model/          # Drools facts
│   │   │   │   ├── AccessRequest.java
│   │   │   │   ├── AccessResult.java
│   │   │   │   └── ...
│   │   │   ├── repository/     # JPA repositories
│   │   │   │   ├── AccessPolicyRepository.java
│   │   │   │   ├── RoleRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   ├── runner/         # Command line runners
│   │   │   │   ├── DataInitializer.java (disabled)
│   │   │   │   └── DroolsDemoRunner.java
│   │   │   └── service/        # Business logic
│   │   │       ├── AccessControlService.java
│   │   │       ├── DynamicRuleService.java
│   │   │       └── PolicyService.java
│   │   └── resources/
│   │       ├── application.yml # Application configuration
│   │       ├── META-INF/
│   │       │   └── kmodule.xml # Drools module config
│   │       └── rules/          # Static Drools rules
│   │           ├── applicant-rules.drl
│   │           └── product-rules.drl
│   └── test/                   # Test files
├── initial_data.sql           # SQL script for initial data
├── diagnose_issue.sql         # Diagnostic SQL queries
└── README-v2.md               # This file
```

### Adding a New Policy

1. **Create policy via API**:
   ```bash
   POST /api/policies
   ```

2. **PolicyService** generates DRL automatically

3. **DynamicRuleService** rebuilds rules automatically

4. **Test the policy**:
   ```bash
   GET /api/access/check?username=...&endpoint=...&method=...
   ```

### Adding a New Endpoint

1. **Create controller method**:
   ```java
   @GetMapping("/api/my-endpoint")
   public ResponseEntity<?> myEndpoint() {
       return ResponseEntity.ok("Data");
   }
   ```

2. **Create policy** to allow access:
   ```bash
   POST /api/policies
   {
     "endpoint": "/api/my-endpoint/**",
     ...
   }
   ```

3. **Test access** with different users

### Modifying Business Logic

- **Policy evaluation**: Modify `PolicyService.generateDrl()`
- **Access control**: Modify `AccessControlService`
- **Rule loading**: Modify `DynamicRuleService`
- **Request handling**: Modify `AccessControlInterceptor`

---

## 🔧 Troubleshooting

### Common Issues and Solutions

#### 1. "No matching policy found" Error

**Symptom:**
```json
{
  "error": "No matching policy found",
  "status": 403,
  "policy": "Default Deny All"
}
```

**Possible Causes:**
- User doesn't have required role
- Policy doesn't exist in database
- Policy is disabled (`enabled = false`)
- User-role mapping is missing

**Solution:**
1. Check user-role mappings:
   ```sql
   SELECT u.username, r.name
   FROM users u
   JOIN user_roles ur ON u.id = ur.user_id
   JOIN roles r ON ur.role_id = r.id
   WHERE u.username = 'admin';
   ```

2. Check enabled policies:
   ```sql
   SELECT policy_name, endpoint, enabled
   FROM access_policies
   WHERE enabled = true;
   ```

3. Run diagnostic script:
   ```bash
   psql -U postgres -d abacdb -f diagnose_issue.sql
   ```

#### 2. Database Connection Error

**Symptom:**
```
org.postgresql.util.PSQLException: Connection refused
```

**Solution:**
1. Verify PostgreSQL is running:
   ```bash
   # Linux/Mac
   sudo systemctl status postgresql
   
   # Windows
   # Check Services
   ```

2. Verify database exists:
   ```sql
   \l
   ```

3. Check connection settings in `application.yml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/abacdb
       username: postgres
       password: your_password
   ```

#### 3. Rules Not Loading

**Symptom:**
- Policies exist in database but access is denied
- No rules fired in logs

**Solution:**
1. Check if policies have `generated_drl`:
   ```sql
   SELECT policy_name, 
          CASE WHEN generated_drl IS NULL OR generated_drl = '' 
               THEN 'MISSING DRL' 
               ELSE 'HAS DRL' 
          END as drl_status
   FROM access_policies
   WHERE enabled = true;
   ```

2. Restart application to reload rules:
   ```bash
   ./gradlew bootRun
   ```

3. Check application logs for rule loading:
   ```
   ✓ Rules rebuilt successfully
   Loaded dynamic policy: Admin Full Access
   ```

#### 4. User Has Role But Access Denied

**Symptom:**
- User has correct role in database
- But still gets 403 Forbidden

**Possible Causes:**
- Policy endpoint pattern doesn't match
- HTTP method mismatch
- Condition not met (department, level, etc.)

**Solution:**
1. Check policy endpoint pattern:
   ```sql
   SELECT policy_name, endpoint, http_method
   FROM access_policies
   WHERE enabled = true
   ORDER BY priority DESC;
   ```

2. Use access check API to debug:
   ```bash
   GET /api/access/check?username=john&endpoint=/api/reports&method=GET
   ```

3. Check user attributes match policy conditions:
   ```sql
   SELECT u.username, u.department, u.level, r.name as role
   FROM users u
   LEFT JOIN user_roles ur ON u.id = ur.user_id
   LEFT JOIN roles r ON ur.role_id = r.id
   WHERE u.username = 'john';
   ```

#### 5. Gradle Build Issues

**Symptom:**
```
FAILURE: Build failed with an exception
```

**Solution:**
1. Clean and rebuild:
   ```bash
   ./gradlew clean build
   ```

2. Check Java version:
   ```bash
   java -version  # Should be 21+
   ```

3. Update Gradle wrapper if needed:
   ```bash
   ./gradlew wrapper --gradle-version 8.14.2
   ```

#### 6. Port Already in Use

**Symptom:**
```
Port 8081 is already in use
```

**Solution:**
1. Change port in `application.yml`:
   ```yaml
   server:
     port: 8082
   ```

2. Or kill the process using port 8081:
   ```bash
   # Linux/Mac
   lsof -ti:8081 | xargs kill -9
   
   # Windows
   netstat -ano | findstr :8081
   taskkill /PID <PID> /F
   ```

---

## 📝 Additional Resources

### Key Files Reference

| File | Purpose |
|------|---------|
| `application.yml` | Application configuration (database, JPA, server) |
| `build.gradle` | Gradle build configuration and dependencies |
| `initial_data.sql` | SQL script to insert initial data |
| `diagnose_issue.sql` | Diagnostic queries for troubleshooting |
| `DroolsConfig.java` | Drools configuration bean |
| `DynamicRuleService.java` | Manages rule loading and KieContainer |
| `PolicyService.java` | Policy CRUD and DRL generation |
| `AccessControlInterceptor.java` | Request interceptor for access control |

### Policy Structure Reference

**PolicyDTO JSON Structure:**
```json
{
  "policyName": "Policy Name (unique, required)",
  "description": "Human readable description",
  "endpoint": "/api/path/** or /api/path/* (required)",
  "httpMethod": "GET|POST|PUT|DELETE|* (required, * = all methods)",
  "allowedRoles": ["ROLE1", "ROLE2"] (array, optional),
  "conditions": {
    "department": {"operator": "equals", "value": "SALES"},
    "userLevel": {"operator": "greaterThanOrEqual", "value": "5"}
  } (object, optional),
  "effect": "ALLOW or DENY (default: ALLOW)",
  "priority": 0-100 (integer, higher = evaluated first, default: 0),
  "enabled": true/false (boolean, default: true)
}
```

### Supported Condition Operators

| Operator | Type | Description | Example |
|----------|------|-------------|---------|
| `equals` | String/Numeric | Exact match | `department == "SALES"` |
| `notEquals` | String/Numeric | Not equal | `department != "HR"` |
| `greaterThan` | Numeric | Greater than | `userLevel > 5` |
| `lessThan` | Numeric | Less than | `userLevel < 10` |
| `greaterThanOrEqual` | Numeric | >= | `userLevel >= 5` |
| `lessThanOrEqual` | Numeric | <= | `userLevel <= 10` |
| `contains` | String | String contains | `department contains "SALES"` |
| `matches` | String | Regex match | `endpoint matches "/api/.*"` |

### Endpoint Pattern Matching

| Pattern | Matches | Example |
|---------|---------|---------|
| `/api/users` | Exact match only | `/api/users` |
| `/api/users/*` | Single segment | `/api/users/123`, `/api/users/john` |
| `/api/users/**` | Any path | `/api/users`, `/api/users/123`, `/api/users/123/profile` |

**Note:** Patterns are converted to regex internally:
- `/**` → `(/.*)?` (matches base path and sub-paths)
- `/*` → `[^/]*` (matches single segment)

---

## 🎓 Understanding the System

### How Policies Work

1. **Policy Creation**: Admin creates policy via REST API
2. **DRL Generation**: `PolicyService` converts policy to Drools Rule Language
3. **Storage**: Policy and DRL stored in `access_policies` table
4. **Rule Loading**: `DynamicRuleService` loads all enabled policies on startup
5. **Rule Compilation**: Drools compiles DRL into executable rules
6. **Request Evaluation**: On each request, Drools evaluates rules against `AccessRequest` fact
7. **Access Decision**: Matching rule sets `AccessResult.allow()` or `deny()`

### How Access Control Works

1. **Request Arrives**: HTTP request to `/api/**` endpoint
2. **Interceptor**: `AccessControlInterceptor` intercepts request
3. **User Lookup**: Loads user from database by username (from `X-Username` header)
4. **Request Building**: Creates `AccessRequest` fact with:
   - User roles
   - Department
   - Level
   - Endpoint
   - HTTP method
5. **Rule Evaluation**: Drools matches `AccessRequest` against all policies
6. **Result**: `AccessResult` contains allow/deny decision
7. **Response**: Interceptor returns 200 (allow) or 403 (deny)

### Rule Priority

Rules are evaluated by `salience` (priority):
- Higher salience = evaluated first
- If multiple rules match, highest priority wins
- Default salience = policy priority value

**Example:**
- Policy 1: Priority 100 → Salience 100 (evaluated first)
- Policy 2: Priority 50 → Salience 50
- Policy 3: Priority 30 → Salience 30 (evaluated last)

---

## 🔐 Security Considerations

### Current Implementation

- **Authentication**: Simple header-based (`X-Username`)
- **Authorization**: Drools-based policy evaluation
- **Database**: PostgreSQL with JPA

### Production Recommendations

1. **Replace Header Authentication**:
   - Implement JWT token validation
   - Use Spring Security
   - Validate tokens in interceptor

2. **Secure Policy Endpoints**:
   - Add role-based check in `PolicyController`
   - Only ADMIN should create/modify policies

3. **Database Security**:
   - Use connection pooling
   - Encrypt sensitive data
   - Use environment variables for credentials

4. **API Security**:
   - Add rate limiting
   - Implement CORS properly
   - Use HTTPS in production

5. **Logging**:
   - Log all access decisions
   - Monitor failed access attempts
   - Audit policy changes

---

## 📊 Monitoring and Logging

### Application Logs

The application logs important events:

```
✓ Rules rebuilt successfully
Loaded dynamic policy: Admin Full Access
Access control rules fired: 1 for /api/users
✓ Access granted for admin to GET /api/users
✗ Access denied for john to GET /api/reports - Reason: No matching policy found
```

### Database Monitoring

Monitor these tables:
- `access_policies`: Policy changes
- `users`: User creation/modification
- `user_roles`: Role assignments

### Performance Considerations

- **Rule Compilation**: Happens once on startup and when policies change
- **Session Creation**: New `KieSession` created per request (lightweight)
- **Database Queries**: User lookup per request (consider caching)
- **Rule Evaluation**: Very fast (Drools is optimized for pattern matching)

---

## 🚀 Deployment

### Building for Production

```bash
# Build JAR
./gradlew clean build

# JAR location
build/libs/drool_v2-0.0.1-SNAPSHOT.jar
```

### Running in Production

```bash
# Set environment variables
export SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/abacdb
export SPRING_DATASOURCE_USERNAME=prod_user
export SPRING_DATASOURCE_PASSWORD=prod_password

# Run JAR
java -jar drool_v2-0.0.1-SNAPSHOT.jar
```

### Docker Deployment (Optional)

Create `Dockerfile`:
```dockerfile
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY build/libs/drool_v2-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run:
```bash
docker build -t abac-system .
docker run -p 8081:8081 abac-system
```

---

## 🤝 Contributing

### Code Style

- Follow Java naming conventions
- Use meaningful variable names
- Add comments for complex logic
- Keep methods focused and small

### Testing

- Test all policy scenarios
- Test edge cases (null values, empty strings)
- Test with different user roles
- Test department and level conditions

---

## 📄 License

This project is for educational purposes.

---

## 👥 Support

For issues or questions:
1. Check the [Troubleshooting](#-troubleshooting) section
2. Review application logs
3. Run diagnostic SQL queries
4. Check Swagger UI for API documentation

---

## 📚 References

- [Drools Documentation](https://www.drools.org/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Gradle Documentation](https://docs.gradle.org/)

---

**Last Updated:** 2025
**Version:** 2.0
**Build System:** Gradle 8.14.2
**Database:** PostgreSQL