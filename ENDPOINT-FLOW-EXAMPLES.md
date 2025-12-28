# Complete Endpoint Flow - From Client Request to Success

This document shows the **complete end-to-end flow** when a client makes a request to a protected endpoint, including all role evaluation processes and code at every step.

---

## 📋 Table of Contents

1. [Complete Request Flow Overview](#complete-request-flow-overview)
2. [Step-by-Step Flow with Code](#step-by-step-flow-with-code)
3. [Role Evaluation Process](#role-evaluation-process)
4. [Drools Rule Evaluation](#drools-rule-evaluation)

---

## 🎯 Complete Request Flow Overview

### Example Scenario

**Client Request:**
```bash
GET http://localhost:8081/api/hr/employees
X-Username: jane
```

**User Context:**
- Username: `jane`
- Roles: `["USER"]` (from database)
- Department: `"HR"`
- Level: `3`

**Expected Result:** ✅ **200 OK** (Access granted)

---

## 🔄 Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. CLIENT REQUEST                                                 │
│ GET /api/hr/employees                                            │
│ Header: X-Username: jane                                         │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. SPRING MVC DISPATCHER                                         │
│ Routes request to interceptor chain                              │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. INTERCEPTOR REGISTRATION (WebConfig)                          │
│ Registers AccessControlInterceptor for /api/**                   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. INTERCEPTOR PRE-HANDLE                                        │
│ AccessControlInterceptor.preHandle()                             │
│   - Extracts endpoint, method, username                          │
│   - Checks if public endpoint                                    │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. ACCESS CONTROL SERVICE                                        │
│ AccessControlService.checkAccess()                               │
│   - Loads user from database                                     │
│   - Validates user exists and active                             │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6. DATABASE QUERY - USER WITH ROLES                              │
│ UserRepository.findByUsername()                                  │
│   - SELECT user + JOIN user_roles + JOIN roles                  │
│   - Returns User entity with roles loaded                        │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 7. ROLE EXTRACTION                                               │
│ User.getRoleNames()                                              │
│   - Converts Set<Role> to Set<String>                            │
│   - Returns: ["USER"]                                            │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 8. BUILD ACCESS REQUEST FACT                                     │
│ AccessRequest object created                                     │
│   - username, userRoles, endpoint, method                        │
│   - department, userLevel, attributes                            │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 9. CREATE DROOLS SESSION                                        │
│ DynamicRuleService.newKieSession()                               │
│   - Gets session from KieContainer                               │
│   - Contains compiled rules from database                        │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 10. INSERT FACTS INTO DROOLS                                    │
│ kieSession.insert(AccessRequest)                                │
│ kieSession.insert(AccessResult)                                  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 11. FIRE ALL RULES                                               │
│ kieSession.fireAllRules()                                       │
│   - Drools pattern matches AccessRequest                         │
│   - Evaluates all enabled policies                              │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 12. RULE PATTERN MATCHING                                        │
│ Matches against "HR Department Access" rule:                    │
│   - endpointMatches("/api/hr(/.*)?") → true                     │
│   - httpMethod == "GET" → true                                   │
│   - hasRole("USER") → true (ROLE CHECK)                         │
│   - department == "HR" → true                                    │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 13. RULE EXECUTION                                               │
│ result.allow("HR Department Access")                             │
│   - Sets allowed=true, evaluated=true                            │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 14. RETURN RESULT                                                │
│ AccessResult returned to interceptor                             │
│   - allowed: true                                                │
│   - matchedPolicyName: "HR Department Access"                    │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 15. INTERCEPTOR DECISION                                         │
│ if (result.isAllowed()) return true                             │
│   - Request continues to controller                             │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 16. CONTROLLER EXECUTION                                         │
│ Controller method executes                                       │
│ Returns data to client                                           │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 17. SUCCESS RESPONSE                                             │
│ HTTP 200 OK                                                      │
│ Response body with data                                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📝 Step-by-Step Flow with Code

### Step 1: Client Request

**What happens:**
- Client sends HTTP GET request to `/api/hr/employees`
- Includes `X-Username: jane` header

**Request:**
```http
GET /api/hr/employees HTTP/1.1
Host: localhost:8081
X-Username: jane
```

---

### Step 2: Spring MVC Dispatcher

**What happens:**
- Spring Boot's `DispatcherServlet` receives the request
- Routes through interceptor chain before reaching controller
- No code needed - handled by Spring Framework

---

### Step 3: Interceptor Registration

**File:** `src/main/java/com/hunesion/drool_v2/config/WebConfig.java`

**Code:**
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AccessControlInterceptor accessControlInterceptor;

    @Autowired
    public WebConfig(AccessControlInterceptor accessControlInterceptor) {
        this.accessControlInterceptor = accessControlInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register interceptor for all /api/** endpoints
        registry.addInterceptor(accessControlInterceptor)
                .addPathPatterns("/api/**")  // Intercept all /api/** requests
                .excludePathPatterns(
                        "/api/auth/**",      // Exclude auth endpoints
                        "/api/public/**",     // Exclude public endpoints
                        "/error"             // Exclude error endpoints
                );
    }
}
```

**What happens:**
- On application startup, Spring calls `addInterceptors()`
- Registers `AccessControlInterceptor` for all `/api/**` paths
- Excludes public endpoints from access control
- **Result:** All `/api/**` requests (except excluded) go through interceptor

---

### Step 4: Interceptor Pre-Handle

**File:** `src/main/java/com/hunesion/drool_v2/interceptor/AccessControlInterceptor.java`

**Code:**
```java
@Component
public class AccessControlInterceptor implements HandlerInterceptor {

    private final AccessControlService accessControlService;

    // Endpoints that bypass access control
    private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/public",
            "/error"
    );

    @Autowired
    public AccessControlInterceptor(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) throws IOException {
        
        // Step 4.1: Extract request information
        String endpoint = request.getRequestURI();  // "/api/hr/employees"
        String method = request.getMethod();        // "GET"

        // Step 4.2: Skip OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;  // Allow CORS preflight
        }

        // Step 4.3: Check if public endpoint (bypass access control)
        if (isPublicEndpoint(endpoint)) {
            return true;  // Allow public endpoints
        }

        // Step 4.4: Extract username from header
        String username = request.getHeader("X-Username");  // "jane"
        
        if (username == null || username.isEmpty()) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "Missing X-Username header", null);
            return false;  // Stop request
        }

        // Step 4.5: Evaluate access using AccessControlService
        AccessResult result = accessControlService.checkAccess(username, endpoint, method);

        // Step 4.6: Allow or deny based on result
        if (result.isAllowed()) {
            System.out.println("✓ Access granted for " + username + " to " + method + " " + endpoint);
            return true;  // Continue to controller
        } else {
            System.out.println("✗ Access denied for " + username + " to " + method + " " + endpoint 
                    + " - Reason: " + result.getDenialReason());
            sendError(response, HttpServletResponse.SC_FORBIDDEN, 
                    result.getDenialReason(), result.getMatchedPolicyName());
            return false;  // Stop request, don't call controller
        }
    }

    private boolean isPublicEndpoint(String endpoint) {
        for (String publicEndpoint : PUBLIC_ENDPOINTS) {
            if (endpoint.startsWith(publicEndpoint)) {
                return true;
            }
        }
        return false;
    }

    private void sendError(HttpServletResponse response, int status, String message, String policy) 
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        String json = String.format(
                "{\"error\": \"%s\", \"status\": %d, \"policy\": \"%s\"}", 
                message, status, policy != null ? policy : "N/A"
        );
        response.getWriter().write(json);
    }
}
```

**What happens:**
1. Extracts endpoint: `/api/hr/employees`
2. Extracts method: `GET`
3. Checks if public endpoint (not public, so continues)
4. Extracts username: `jane` from `X-Username` header
5. Calls `accessControlService.checkAccess("jane", "/api/hr/employees", "GET")`
6. Based on result, allows (returns `true`) or denies (returns `false`)

---

### Step 5: Access Control Service - Check Access

**File:** `src/main/java/com/hunesion/drool_v2/service/AccessControlService.java`

**Code:**
```java
@Service
public class AccessControlService {

    private final DynamicRuleService dynamicRuleService;
    private final UserRepository userRepository;

    @Autowired
    public AccessControlService(DynamicRuleService dynamicRuleService,
                                UserRepository userRepository) {
        this.dynamicRuleService = dynamicRuleService;
        this.userRepository = userRepository;
    }

    /**
     * Check if a user has access to a specific endpoint
     */
    public AccessResult checkAccess(String username, String endpoint, String httpMethod) {
        // Step 5.1: Load user from database (includes roles)
        User user = userRepository.findByUsername(username)
                .orElse(null);

        // Step 5.2: Validate user exists
        if (user == null) {
            AccessResult result = new AccessResult();
            result.deny("User Not Found", "User does not exist: " + username);
            return result;
        }

        // Step 5.3: Validate user is active
        if (!user.isActive()) {
            AccessResult result = new AccessResult();
            result.deny("User Inactive", "User account is disabled");
            return result;
        }

        // Step 5.4: Build AccessRequest fact from user data
        AccessRequest request = new AccessRequest();
        request.setUsername(username);
        request.setUserRoles(user.getRoleNames());  // ⭐ ROLE EXTRACTION HERE
        request.setEndpoint(endpoint);
        request.setHttpMethod(httpMethod);
        request.setDepartment(user.getDepartment());
        request.setUserLevel(user.getLevel());
        
        // Step 5.5: Copy user attributes
        if (user.getAttributes() != null) {
            user.getAttributes().forEach((k, v) -> request.setAttribute(k, v));
        }

        // Step 5.6: Evaluate access using Drools
        return evaluateAccess(request);
    }
}
```

**What happens:**
1. Calls `userRepository.findByUsername("jane")` to load user
2. Validates user exists and is active
3. **Extracts roles** using `user.getRoleNames()` (see Step 6)
4. Builds `AccessRequest` fact with all user context
5. Calls `evaluateAccess(request)` to evaluate with Drools

---

### Step 6: Database Query - Load User with Roles

**File:** `src/main/java/com/hunesion/drool_v2/repository/UserRepository.java`

**Code:**
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
```

**What happens:**
- Spring Data JPA automatically generates SQL query
- Because `User` entity has `@ManyToMany(fetch = FetchType.EAGER)` on roles, JPA loads roles in the same query

**SQL Executed:**
```sql
-- Main query
SELECT 
    u.id, u.username, u.password, u.email, 
    u.department, u.level, u.active
FROM users u
WHERE u.username = 'jane';

-- Role join (because of @ManyToMany with EAGER fetch)
SELECT 
    ur.user_id, ur.role_id,
    r.id, r.name, r.description
FROM user_roles ur
INNER JOIN roles r ON ur.role_id = r.id
WHERE ur.user_id = (
    SELECT id FROM users WHERE username = 'jane'
);
```

**User Entity Loaded:**
```java
User {
    id: 4
    username: "jane"
    email: "jane@example.com"
    department: "HR"
    level: 3
    active: true
    roles: Set<Role> {
        Role {
            id: 3
            name: "USER"
            description: "Regular User"
        }
    }
    attributes: {}
}
```

---

### Step 7: Role Extraction Process

**File:** `src/main/java/com/hunesion/drool_v2/entity/User.java`

**Code:**
```java
@Entity
@Table(name = "users")
public class User {
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * Extract role names as Set<String>
     * This is called in AccessControlService.checkAccess()
     */
    public Set<String> getRoleNames() {
        // Step 7.1: Stream through Set<Role>
        // Step 7.2: Map each Role to its name (String)
        // Step 7.3: Collect into Set<String>
        return roles.stream()
                .map(Role::getName)  // Role.getName() → "USER"
                .collect(Collectors.toSet());
    }

    /**
     * Check if user has a specific role
     */
    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(r -> r.getName().equals(roleName));
    }
}
```

**What happens:**
1. `user.getRoleNames()` is called
2. Streams through `Set<Role>`: `[Role{name: "USER"}]`
3. Maps each role to its name: `Role → "USER"`
4. Collects into `Set<String>`: `["USER"]`

**Result:**
```java
Set<String> userRoles = ["USER"]
```

**This Set is then set in AccessRequest:**
```java
request.setUserRoles(["USER"]);
```

---

### Step 8: Build AccessRequest Fact

**File:** `src/main/java/com/hunesion/drool_v2/model/AccessRequest.java`

**Code:**
```java
public class AccessRequest {
    private String username;
    private Set<String> userRoles = new HashSet<>();  // ⭐ ROLES STORED HERE
    private String endpoint;
    private String httpMethod;
    private String department;
    private Integer userLevel;
    private Map<String, Object> attributes = new HashMap<>();

    // Helper methods for Drools rules
    
    /**
     * Check if user has a specific role
     * Used in Drools rules: hasRole("USER")
     */
    public boolean hasRole(String role) {
        return userRoles != null && userRoles.contains(role);
    }

    /**
     * Check if user has any of the given roles
     */
    public boolean hasAnyRole(String... roles) {
        if (userRoles == null) return false;
        for (String role : roles) {
            if (userRoles.contains(role)) return true;
        }
        return false;
    }

    /**
     * Check if endpoint matches pattern (regex)
     * Used in Drools rules: endpointMatches("/api/hr(/.*)?")
     */
    public boolean endpointMatches(String pattern) {
        if (endpoint == null || pattern == null) return false;
        return endpoint.matches(pattern);
    }
}
```

**AccessRequest Object Created:**
```java
AccessRequest {
    username: "jane"
    userRoles: ["USER"]  // ⭐ From Step 7
    endpoint: "/api/hr/employees"
    httpMethod: "GET"
    department: "HR"
    userLevel: 3
    attributes: {}
}
```

**What happens:**
- All user context is now in `AccessRequest` object
- This object will be inserted into Drools as a "fact"
- Drools rules will pattern-match against this fact

---

### Step 9: Create Drools Session

**File:** `src/main/java/com/hunesion/drool_v2/service/AccessControlService.java`

**Code:**
```java
public AccessResult evaluateAccess(AccessRequest request) {
    // Step 9.1: Get new KieSession from container
    // The container contains all compiled rules from database
    KieSession kieSession = dynamicRuleService.newKieSession();
    
    // Step 9.2: Create empty result object
    AccessResult result = new AccessResult();

    try {
        // Step 9.3: Insert facts into Drools working memory
        kieSession.insert(request);   // AccessRequest fact
        kieSession.insert(result);    // AccessResult fact (will be modified by rules)
        
        // Step 9.4: Fire all rules - Drools evaluates
        int rulesFired = kieSession.fireAllRules();
        System.out.println("Access control rules fired: " + rulesFired + " for " + request.getEndpoint());
        
        // Step 9.5: If no rules matched, deny by default
        if (!result.isEvaluated()) {
            result.deny("No Policy Match", "No access policy found for this endpoint");
        }
        
    } finally {
        // Step 9.6: Always dispose session
        kieSession.dispose();
    }

    return result;
}
```

**What happens:**
1. Gets a new `KieSession` from `KieContainer`
2. Creates empty `AccessResult` object
3. Inserts both facts into Drools working memory:
   - `AccessRequest` (input - what we're checking)
   - `AccessResult` (output - will be modified by rules)
4. Calls `fireAllRules()` - Drools evaluates all rules
5. Disposes session after evaluation

---

### Step 10: Drools Rule Evaluation - Pattern Matching

**What Drools Does:**

Drools has compiled rules from the database. One of them is:

**Rule from Database:**
```drl
rule "HR Department Access"
    salience 45
    when
        $request : AccessRequest(
            endpointMatches("/api/hr(/.*)?"),
            httpMethod == "GET",
            (hasRole("USER") || hasRole("MANAGER")),
            department == "HR"
        )
        $result : AccessResult(evaluated == false)
    then
        $result.allow("HR Department Access");
        System.out.println("✓ Access ALLOWED by policy: HR Department Access");
end
```

**Pattern Matching Process:**

Drools tries to match the `AccessRequest` fact against this rule's conditions:

#### Condition 1: `endpointMatches("/api/hr(/.*)?")`
```java
// Drools calls: request.endpointMatches("/api/hr(/.*)?")
// Which calls: request.endpoint.matches("/api/hr(/.*)?")
"/api/hr/employees".matches("/api/hr(/.*)?")  // true ✓
```

#### Condition 2: `httpMethod == "GET"`
```java
// Drools checks: request.httpMethod == "GET"
request.getHttpMethod().equals("GET")  // "GET" == "GET" → true ✓
```

#### Condition 3: `(hasRole("USER") || hasRole("MANAGER"))` ⭐ **ROLE CHECK**
```java
// Drools calls: request.hasRole("USER") || request.hasRole("MANAGER")
// Which calls: request.userRoles.contains("USER") || request.userRoles.contains("MANAGER")

// Step 10.1: Check hasRole("USER")
request.hasRole("USER")
  → request.userRoles.contains("USER")
  → ["USER"].contains("USER")
  → true ✓

// Step 10.2: Check hasRole("MANAGER")
request.hasRole("MANAGER")
  → request.userRoles.contains("MANAGER")
  → ["USER"].contains("MANAGER")
  → false

// Step 10.3: OR condition
true || false → true ✓
```

**Role Evaluation Code (in AccessRequest):**
```java
public boolean hasRole(String role) {
    // userRoles = ["USER"] (from Step 7)
    return userRoles != null && userRoles.contains(role);
    // Returns: ["USER"].contains("USER") → true
}
```

#### Condition 4: `department == "HR"`
```java
// Drools checks: request.department == "HR"
request.getDepartment().equals("HR")  // "HR" == "HR" → true ✓
```

#### Condition 5: `$result : AccessResult(evaluated == false)`
```java
// Drools checks: result.isEvaluated() == false
result.isEvaluated()  // Initially false → true ✓
```

**All conditions match!** → Rule fires

---

### Step 11: Rule Execution

**When rule fires, the `then` clause executes:**

```drl
then
    $result.allow("HR Department Access");
    System.out.println("✓ Access ALLOWED by policy: HR Department Access");
end
```

**Code Execution:**
```java
// Drools calls: result.allow("HR Department Access")
// Which executes:
result.setAllowed(true);
result.setEvaluated(true);
result.setMatchedPolicyName("HR Department Access");
```

**AccessResult After Rule:**
```java
AccessResult {
    allowed: true
    evaluated: true
    matchedPolicyName: "HR Department Access"
    denialReason: null
}
```

---

### Step 12: Return Result to Interceptor

**Flow back:**
1. `evaluateAccess()` returns `AccessResult` with `allowed=true`
2. `checkAccess()` returns `AccessResult` to interceptor
3. Interceptor checks `result.isAllowed()`

**Code in Interceptor:**
```java
AccessResult result = accessControlService.checkAccess(username, endpoint, method);

if (result.isAllowed()) {  // true
    System.out.println("✓ Access granted for jane to GET /api/hr/employees");
    return true;  // Continue to controller
}
```

---

### Step 13: Controller Execution

**If access is granted, request continues to controller:**

**Example Controller:**
```java
@RestController
@RequestMapping("/api/hr")
public class HrController {

    @GetMapping("/employees")
    public ResponseEntity<List<Employee>> getEmployees() {
        // This method executes because interceptor returned true
        List<Employee> employees = // ... fetch employees
        return ResponseEntity.ok(employees);
    }
}
```

**What happens:**
- Controller method executes normally
- Returns data to client
- No access control code needed in controller (handled by interceptor)

---

### Step 14: Success Response

**HTTP Response:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": 1,
    "name": "John Doe",
    "department": "HR"
  },
  ...
]
```

---

## 🔍 Role Evaluation Process - Detailed

### How Roles Are Loaded

**1. Database Structure:**
```
users table:
  id | username | department | level
  4  | jane     | HR         | 3

roles table:
  id | name   | description
  3  | USER   | Regular User

user_roles table (join table):
  user_id | role_id
  4       | 3
```

**2. JPA Entity Mapping:**
```java
@ManyToMany(fetch = FetchType.EAGER)
@JoinTable(
    name = "user_roles",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "role_id")
)
private Set<Role> roles = new HashSet<>();
```

**3. Role Extraction:**
```java
// In User.getRoleNames()
roles.stream()
    .map(Role::getName)  // Role{name:"USER"} → "USER"
    .collect(Collectors.toSet())  // ["USER"]
```

**4. Role Storage in AccessRequest:**
```java
request.setUserRoles(["USER"]);
```

**5. Role Check in Drools Rule:**
```drl
hasRole("USER")  // Calls request.hasRole("USER")
```

**6. Role Check Implementation:**
```java
public boolean hasRole(String role) {
    return userRoles != null && userRoles.contains(role);
    // ["USER"].contains("USER") → true
}
```

---

## 📊 Complete Flow Summary Table

| Step | Component | Action | Code Location | Key Data |
|------|-----------|--------|---------------|----------|
| 1 | Client | Sends HTTP request | N/A | GET /api/hr/employees |
| 2 | Spring MVC | Routes request | Framework | - |
| 3 | WebConfig | Registers interceptor | `WebConfig.addInterceptors()` | Interceptor registered |
| 4 | Interceptor | Extracts info, calls service | `AccessControlInterceptor.preHandle()` | username="jane" |
| 5 | AccessControlService | Loads user, builds request | `AccessControlService.checkAccess()` | Calls repository |
| 6 | UserRepository | Queries database | `UserRepository.findByUsername()` | SQL executed |
| 7 | User Entity | Extracts roles | `User.getRoleNames()` | roles=["USER"] |
| 8 | AccessRequest | Builds fact object | `AccessRequest` constructor | Fact created |
| 9 | DynamicRuleService | Creates Drools session | `DynamicRuleService.newKieSession()` | Session created |
| 10 | Drools | Inserts facts | `kieSession.insert()` | Facts in memory |
| 11 | Drools | Pattern matches | `fireAllRules()` | Rule matched |
| 12 | Drools Rule | Checks roles | `hasRole("USER")` | Role check passes |
| 13 | Drools Rule | Executes action | `result.allow()` | allowed=true |
| 14 | AccessControlService | Returns result | `evaluateAccess()` | Result returned |
| 15 | Interceptor | Allows request | `return true` | Request continues |
| 16 | Controller | Executes method | Controller method | Data returned |
| 17 | Client | Receives response | N/A | HTTP 200 OK |

---

## 🎓 Key Takeaways

### 1. Role Loading Flow
```
Database (user_roles table)
    ↓
JPA @ManyToMany mapping
    ↓
User.roles (Set<Role>)
    ↓
User.getRoleNames() → Set<String>
    ↓
AccessRequest.userRoles
    ↓
Drools rule: hasRole("USER")
    ↓
AccessRequest.hasRole() method
    ↓
userRoles.contains("USER")
```

### 2. Access Control Flow
```
Request → Interceptor → Service → Database → Roles → AccessRequest → Drools → Result → Controller
```

### 3. Role Evaluation Points
- **Database**: Roles loaded via JPA join
- **Service**: Roles extracted via `getRoleNames()`
- **AccessRequest**: Roles stored as `Set<String>`
- **Drools Rule**: Roles checked via `hasRole()` method
- **AccessRequest.hasRole()**: Final check using `contains()`

---

**Last Updated:** 2025
**Version:** 2.0 - Complete Flow with Role Evaluation
