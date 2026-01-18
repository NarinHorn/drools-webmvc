# Project Setup Guide - RBAC/ABAC Policy Management with Drools (Access & Equipment Policies)

This guide walks through creating the RBAC + ABAC system used in this project. The current implementation supports two scopes:
- **AccessPolicy** (API-level) for endpoint authorization
- **EquipmentPolicy** (device/session-level) for SSH/RDP/DB access, commands, time windows, and richer attributes

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Step 1: Initialize Gradle Project](#step-1-initialize-gradle-project)
3. [Step 2: Create Directory Structure](#step-2-create-directory-structure)
4. [Step 3: Configure Gradle Build Files](#step-3-configure-gradle-build-files)
5. [Step 4: Create Application Configuration](#step-4-create-application-configuration)
6. [Step 5: Create Entity Classes](#step-5-create-entity-classes)
7. [Step 6: Create Repository Interfaces](#step-6-create-repository-interfaces)
8. [Step 7: Create Model Classes](#step-7-create-model-classes)
9. [Step 8: Create Service Classes](#step-8-create-service-classes)
10. [Step 9: Create Controller Classes](#step-9-create-controller-classes)
11. [Step 10: Create Interceptor](#step-10-create-interceptor)
12. [Step 11: Create Configuration Classes](#step-11-create-configuration-classes)
13. [Step 12: Create DTO Classes](#step-12-create-dto-classes)
14. [Step 13: Create Main Application Class](#step-13-create-main-application-class)
15. [Step 14: Database Setup](#step-14-database-setup)
16. [Step 15: Docker Configuration](#step-15-docker-configuration)
17. [Step 16: Run and Test](#step-16-run-and-test)
18. [Step 17: Verification Checklist](#step-17-verification-checklist)
19. [Troubleshooting](#troubleshooting)

---

## Prerequisites

Before starting, ensure you have the following installed:

- **Java Development Kit (JDK)**: Version 21 or higher
    - Download from: https://adoptium.net/
    - Verify: `java -version`

- **Gradle**: Version 8.14.2 or higher (or use Gradle Wrapper)
    - Download from: https://gradle.org/install/
    - Verify: `gradle -v`

- **PostgreSQL**: Version 16 or higher
    - Download from: https://www.postgresql.org/download/
    - Or use Docker (recommended)

- **Docker & Docker Compose** (Optional but recommended)
    - Download from: https://www.docker.com/get-started
    - Verify: `docker --version` and `docker-compose --version`

- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions

---

## Step 1: Initialize Gradle Project

### 1.1 Create Project Directory

```bash
mkdir drools-webmvc
cd drools-webmvc
```

### 1.2 Initialize Gradle Project Structure

Create the basic directory structure:

```bash
mkdir -p src/main/java/com/hunesion/drool_v2
mkdir -p src/main/resources/META-INF
mkdir -p src/main/resources/sql
mkdir -p src/test/java/com/hunesion/drool_v2
mkdir -p gradle/wrapper
```

---

## Step 2: Create Directory Structure

Create the following subdirectories for your Java packages:

```bash
# Main source directories
mkdir -p src/main/java/com/hunesion/drool_v2/{config,controller,dto,entity,interceptor,model,repository,runner,service}

# Resource directories
mkdir -p src/main/resources/META-INF
mkdir -p src/main/resources/sql
```

Your final structure should look like:

```
drools-webmvc/
├── build.gradle
├── settings.gradle
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew
├── gradlew.bat
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── hunesion/
│   │   │           └── drool_v2/
│   │   │               ├── config/
│   │   │               ├── controller/
│   │   │               ├── dto/
│   │   │               ├── entity/
│   │   │               ├── interceptor/
│   │   │               ├── model/
│   │   │               ├── repository/
│   │   │               ├── runner/
│   │   │               └── service/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── META-INF/
│   │       │   └── kmodule.xml
│   │       └── sql/
│   │           └── initial_data.sql
│   └── test/
│       └── java/
│           └── com/
│               └── hunesion/
│                   └── drool_v2/
└── Dockerfile
└── docker-compose.yml
```

---

## Step 3: Configure Gradle Build Files

### 3.1 Create `settings.gradle`

Create `settings.gradle` in the root directory:

```gradle
rootProject.name = 'drool_v2'
```

### 3.2 Create `build.gradle`

Create `build.gradle` in the root directory:

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.1'
    id 'io.spring.dependency-management' version '1.1.6'
}

group = 'com.hunesion'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '21'

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

ext {
    droolsVersion = '8.44.0.Final'
}

dependencies {
    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    
    // Drools Dependencies
    implementation "org.drools:drools-core:${droolsVersion}"
    implementation "org.drools:drools-compiler:${droolsVersion}"
    implementation "org.drools:drools-mvel:${droolsVersion}"
    
    // PostgreSQL Database
    runtimeOnly 'org.postgresql:postgresql'
    
    // Jackson for JSON processing
    implementation 'com.fasterxml.jackson.core:jackson-databind'
    
    // Swagger / OpenAPI for API documentation
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0'
    
    // Lombok for reducing boilerplate
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    // Test dependencies
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

### 3.3 Initialize Gradle Wrapper

Run the following command to generate Gradle wrapper files:

```bash
gradle wrapper --gradle-version 8.14.2
```

This will create:
- `gradlew` (Unix script)
- `gradlew.bat` (Windows script)
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`

---

## Step 4: Create Application Configuration

### 4.1 Create `application.yml`

Create `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: drool_v2
  
  datasource:
    url: jdbc:postgresql://localhost:5432/abacdb
    username: postgres
    password: Rin25052001
    driver-class-name: org.postgresql.Driver
  
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true

server:
  port: 8081
```

**Note**: Change the database password in production!

### 4.2 Create `kmodule.xml`

Create `src/main/resources/META-INF/kmodule.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<kmodule xmlns="http://www.drools.org/xsd/kmodule">
    <!-- 
        KieBase: A repository of all the application's knowledge definitions (rules, processes, etc.)
        KieSession: A runtime session where rules are fired against facts
    -->
    <kbase name="rules" packages="rules">
        <ksession name="rulesSession"/>
    </kbase>
</kmodule>
```

---

## Step 5: Create Entity Classes

### 5.1 Create `Role.java`

Create `src/main/java/com/hunesion/drool_v2/entity/Role.java`:

```java
package com.hunesion.drool_v2.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    @JsonIgnore
    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();

    public Role() {
    }

    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
```

### 5.2 Create `User.java`

Create `src/main/java/com/hunesion/drool_v2/entity/User.java`:

```java
package com.hunesion.drool_v2.entity;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String email;

    private String department;

    private Integer level;

    private boolean active = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_attributes", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "attribute_key")
    @Column(name = "attribute_value")
    private Map<String, String> attributes = new HashMap<>();

    public User() {
    }

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    public Set<String> getRoleNames() {
        return roles.stream().map(Role::getName).collect(Collectors.toSet());
    }

    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(r -> r.getName().equals(roleName));
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", department='" + department + '\'' +
                ", level=" + level +
                ", active=" + active +
                ", roles=" + getRoleNames() +
                '}';
    }
}
```

### 5.3 Create `AccessPolicy.java`

Create `src/main/java/com/hunesion/drool_v2/entity/AccessPolicy.java`:

```java
package com.hunesion.drool_v2.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "access_policies")
public class AccessPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String policyName;

    private String description;

    @Column(nullable = false)
    private String endpoint;

    @Column(nullable = false)
    private String httpMethod;

    @Column(columnDefinition = "TEXT")
    private String allowedRoles;

    @Column(columnDefinition = "TEXT")
    private String conditions;

    @Column(nullable = false)
    private String effect;

    private Integer priority = 0;

    private boolean enabled = true;

    @Column(columnDefinition = "TEXT")
    private String generatedDrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public AccessPolicy() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getAllowedRoles() {
        return allowedRoles;
    }

    public void setAllowedRoles(String allowedRoles) {
        this.allowedRoles = allowedRoles;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getGeneratedDrl() {
        return generatedDrl;
    }

    public void setGeneratedDrl(String generatedDrl) {
        this.generatedDrl = generatedDrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "AccessPolicy{" +
                "id=" + id +
                ", policyName='" + policyName + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                ", effect='" + effect + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
```

**Note**: The current codebase also includes **EquipmentPolicy** and related assignment entities for device/session-level control (see `src/main/java/com/hunesion/drool_v2/model/entity/EquipmentPolicy.java`). Those are generated similarly but use JSONB `policy_config` and multiple assignment mappings (user/group/role/equipment).

---

## Step 6: Create Repository Interfaces

### 6.1 Create `RoleRepository.java`

Create `src/main/java/com/hunesion/drool_v2/repository/RoleRepository.java`:

```java
package com.hunesion.drool_v2.repository;

import com.hunesion.drool_v2.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    boolean existsByName(String name);
}
```

### 6.2 Create `UserRepository.java`

Create `src/main/java/com/hunesion/drool_v2/repository/UserRepository.java`:

```java
package com.hunesion.drool_v2.repository;

import com.hunesion.drool_v2.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
```

### 6.3 Create `AccessPolicyRepository.java`

Create `src/main/java/com/hunesion/drool_v2/repository/AccessPolicyRepository.java`:

```java
package com.hunesion.drool_v2.repository;

import com.hunesion.drool_v2.entity.AccessPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessPolicyRepository extends JpaRepository<AccessPolicy, Long> {
    Optional<AccessPolicy> findByPolicyName(String policyName);
    List<AccessPolicy> findByEnabledTrueOrderByPriorityDesc();
    List<AccessPolicy> findByEndpointAndHttpMethod(String endpoint, String httpMethod);
    boolean existsByPolicyName(String policyName);
}
```

---

## Step 7: Create Model Classes

### 7.1 Create `AccessRequest.java`

Create `src/main/java/com/hunesion/drool_v2/model/AccessRequest.java`:

```java
package com.hunesion.drool_v2.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * AccessRequest - Drools fact representing an access request to be evaluated
 * This contains all context needed for policy evaluation
 */
public class AccessRequest {

    private String username;
    private Set<String> userRoles = new HashSet<>();
    private String endpoint;
    private String httpMethod;
    private String department;
    private Integer userLevel;
    private Map<String, Object> attributes = new HashMap<>();

    public AccessRequest() {
    }

    public AccessRequest(String username, Set<String> userRoles, String endpoint, String httpMethod) {
        this.username = username;
        this.userRoles = userRoles;
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Set<String> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(Set<String> userRoles) {
        this.userRoles = userRoles;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getUserLevel() {
        return userLevel;
    }

    public void setUserLevel(Integer userLevel) {
        this.userLevel = userLevel;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public void setAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }

    public boolean hasRole(String role) {
        return userRoles != null && userRoles.contains(role);
    }

    public boolean hasAnyRole(String... roles) {
        if (userRoles == null) return false;
        for (String role : roles) {
            if (userRoles.contains(role)) return true;
        }
        return false;
    }

    public boolean endpointMatches(String pattern) {
        if (endpoint == null || pattern == null) return false;
        // Pattern is already a regex (converted by PolicyService.convertEndpointToRegex)
        return endpoint.matches(pattern);
    }

    @Override
    public String toString() {
        return "AccessRequest{" +
                "username='" + username + '\'' +
                ", userRoles=" + userRoles +
                ", endpoint='" + endpoint + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                ", department='" + department + '\'' +
                ", userLevel=" + userLevel +
                '}';
    }
}
```

### 7.2 Create `AccessResult.java`

Create `src/main/java/com/hunesion/drool_v2/model/AccessResult.java`:

```java
package com.hunesion.drool_v2.model;

/**
 * AccessResult - Drools fact that holds the result of policy evaluation
 * Rules will modify this object to indicate whether access is allowed or denied
 */
public class AccessResult {

    private boolean allowed = false;
    private boolean evaluated = false;
    private String matchedPolicyName;
    private String denialReason;

    public AccessResult() {
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
        this.evaluated = true;
    }

    public boolean isEvaluated() {
        return evaluated;
    }

    public void setEvaluated(boolean evaluated) {
        this.evaluated = evaluated;
    }

    public String getMatchedPolicyName() {
        return matchedPolicyName;
    }

    public void setMatchedPolicyName(String matchedPolicyName) {
        this.matchedPolicyName = matchedPolicyName;
    }

    public String getDenialReason() {
        return denialReason;
    }

    public void setDenialReason(String denialReason) {
        this.denialReason = denialReason;
    }

    public void allow(String policyName) {
        this.allowed = true;
        this.evaluated = true;
        this.matchedPolicyName = policyName;
    }

    public void deny(String policyName, String reason) {
        this.allowed = false;
        this.evaluated = true;
        this.matchedPolicyName = policyName;
        this.denialReason = reason;
    }

    @Override
    public String toString() {
        return "AccessResult{" +
                "allowed=" + allowed +
                ", evaluated=" + evaluated +
                ", matchedPolicyName='" + matchedPolicyName + '\'' +
                ", denialReason='" + denialReason + '\'' +
                '}';
    }
}
```

---

## Step 8: Create Service Classes

### 8.1 Create `DynamicRuleService.java`

Create `src/main/java/com/hunesion/drool_v2/service/DynamicRuleService.java`:

```java
package com.hunesion.drool_v2.service;

import com.hunesion.drool_v2.entity.AccessPolicy;
import com.hunesion.drool_v2.repository.AccessPolicyRepository;
import jakarta.annotation.PostConstruct;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * DynamicRuleService - Manages KieContainer lifecycle for dynamic rule loading
 * 
 * This service:
 * - Loads static rules from classpath on startup
 * - Loads AccessPolicy dynamic rules from database
 * - Loads EquipmentPolicy dynamic rules generated by `EquipmentPolicyRuleGenerator`
 * - Provides thread-safe access to KieSession
 * - Supports hot-reloading of rules when policies change
 */
@Service
public class DynamicRuleService {

    private final AccessPolicyRepository accessPolicyRepository;
    private final KieServices kieServices;
    private volatile KieContainer kieContainer;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private static final String STATIC_RULES_PATH = "rules/";
    private static final String DYNAMIC_RULES_PATH = "src/main/resources/rules/dynamic/";

    @Autowired
    public DynamicRuleService(AccessPolicyRepository accessPolicyRepository) {
        this.accessPolicyRepository = accessPolicyRepository;
        this.kieServices = KieServices.Factory.get();
    }

    @PostConstruct
    public void init() {
        rebuildRules();
    }

    /**
     * Rebuilds the KieContainer with all rules (static + dynamic from DB)
     * Called when policies are created, updated, or deleted
     */
    public void rebuildRules() {
        lock.writeLock().lock();
        try {
            KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

            // Load static rules from classpath
            loadStaticRules(kieFileSystem);

            // Load dynamic rules from database
            loadDynamicRulesFromDatabase(kieFileSystem);

            // Build and verify
            KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
            kieBuilder.buildAll();

            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                throw new RuntimeException("Drools rule compilation errors:\n" 
                        + kieBuilder.getResults().getMessages());
            }

            // Create new container
            this.kieContainer = kieServices.newKieContainer(
                    kieBuilder.getKieModule().getReleaseId()
            );

            System.out.println("✓ Rules rebuilt successfully");
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void loadStaticRules(KieFileSystem kieFileSystem) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:" + STATIC_RULES_PATH + "**/*.drl");
            
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                // Skip dynamic access policy rules that should come from DB
                if (filename != null && !filename.startsWith("access-policy")) {
                    String path = STATIC_RULES_PATH + filename;
                    kieFileSystem.write("src/main/resources/" + path,
                            kieServices.getResources().newInputStreamResource(resource.getInputStream()));
                    System.out.println("  Loaded static rule: " + filename);
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load static rules - " + e.getMessage());
        }
    }

    private void loadDynamicRulesFromDatabase(KieFileSystem kieFileSystem) {
        List<AccessPolicy> policies = accessPolicyRepository.findByEnabledTrueOrderByPriorityDesc();
        
        if (policies.isEmpty()) {
            System.out.println("  No dynamic policies found in database");
            // Add a default permissive rule
            String defaultRule = generateDefaultRule();
            kieFileSystem.write(DYNAMIC_RULES_PATH + "default-policy.drl", defaultRule);
            return;
        }

        // Combine all policy DRLs into one file
        StringBuilder combinedDrl = new StringBuilder();
        combinedDrl.append("package rules.dynamic;\n\n");
        combinedDrl.append("import com.hunesion.drool_v2.model.AccessRequest;\n");
        combinedDrl.append("import com.hunesion.drool_v2.model.AccessResult;\n\n");

        for (AccessPolicy policy : policies) {
            if (policy.getGeneratedDrl() != null && !policy.getGeneratedDrl().isEmpty()) {
                // Extract just the rule part (without package and imports)
                String drl = policy.getGeneratedDrl();
                int ruleStart = drl.indexOf("rule ");
                if (ruleStart >= 0) {
                    combinedDrl.append(drl.substring(ruleStart));
                    combinedDrl.append("\n\n");
                }
                System.out.println("  Loaded dynamic policy: " + policy.getPolicyName());
            }
        }

        kieFileSystem.write(DYNAMIC_RULES_PATH + "access-policies.drl", combinedDrl.toString());
    }

    private String generateDefaultRule() {
        return """
            package rules.dynamic;
            
            import com.hunesion.drool_v2.model.AccessRequest;
            import com.hunesion.drool_v2.model.AccessResult;
            
            // Default rule: Deny all if no other rules match (evaluated last due to low salience)
            rule "Default Deny All"
                salience -1000
                when
                    $request : AccessRequest()
                    $result : AccessResult(evaluated == false)
                then
                    $result.deny("Default Deny All", "No matching policy found");
                    System.out.println("Default Deny rule applied for: " + $request.getEndpoint());
            end
            """;
    }

    /**
     * Creates a new KieSession for rule evaluation
     * Thread-safe: uses read lock to allow concurrent sessions
     */
    public KieSession newKieSession() {
        lock.readLock().lock();
        try {
            return kieContainer.newKieSession();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the current KieContainer (for advanced usage)
     */
    public KieContainer getKieContainer() {
        lock.readLock().lock();
        try {
            return kieContainer;
        } finally {
            lock.readLock().unlock();
        }
    }
}
```

### 8.2 Create `PolicyService.java`

Create `src/main/java/com/hunesion/drool_v2/service/PolicyService.java`:

```java
package com.hunesion.drool_v2.service;

import com.hunesion.drool_v2.dto.ConditionDTO;
import com.hunesion.drool_v2.dto.PolicyDTO;
import com.hunesion.drool_v2.entity.AccessPolicy;
import com.hunesion.drool_v2.repository.AccessPolicyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PolicyService - CRUD operations for access policies and DRL generation
 * 
 * Converts structured PolicyDTO from frontend into DRL rules
 */
@Service
public class PolicyService {

    private final AccessPolicyRepository accessPolicyRepository;
    private final DynamicRuleService dynamicRuleService;
    private final ObjectMapper objectMapper;

    @Autowired
    public PolicyService(AccessPolicyRepository accessPolicyRepository,
                         DynamicRuleService dynamicRuleService) {
        this.accessPolicyRepository = accessPolicyRepository;
        this.dynamicRuleService = dynamicRuleService;
        this.objectMapper = new ObjectMapper();
    }

    public List<AccessPolicy> getAllPolicies() {
        return accessPolicyRepository.findAll();
    }

    public List<AccessPolicy> getEnabledPolicies() {
        return accessPolicyRepository.findByEnabledTrueOrderByPriorityDesc();
    }

    public AccessPolicy getPolicyById(Long id) {
        return accessPolicyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found with id: " + id));
    }

    public AccessPolicy getPolicyByName(String name) {
        return accessPolicyRepository.findByPolicyName(name)
                .orElseThrow(() -> new RuntimeException("Policy not found: " + name));
    }

    @Transactional
    public AccessPolicy createPolicy(PolicyDTO dto) {
        if (accessPolicyRepository.existsByPolicyName(dto.getPolicyName())) {
            throw new RuntimeException("Policy already exists: " + dto.getPolicyName());
        }

        AccessPolicy policy = convertDtoToEntity(dto);
        String drl = generateDrl(dto);
        policy.setGeneratedDrl(drl);

        AccessPolicy saved = accessPolicyRepository.save(policy);
        
        // Rebuild rules to include new policy
        dynamicRuleService.rebuildRules();
        
        return saved;
    }

    @Transactional
    public AccessPolicy updatePolicy(Long id, PolicyDTO dto) {
        AccessPolicy existing = getPolicyById(id);
        
        existing.setPolicyName(dto.getPolicyName());
        existing.setDescription(dto.getDescription());
        existing.setEndpoint(dto.getEndpoint());
        existing.setHttpMethod(dto.getHttpMethod());
        existing.setEffect(dto.getEffect());
        
        if (dto.getPriority() != null) {
            existing.setPriority(dto.getPriority());
        }
        if (dto.getEnabled() != null) {
            existing.setEnabled(dto.getEnabled());
        }
        
        // Convert roles and conditions to JSON strings
        try {
            if (dto.getAllowedRoles() != null) {
                existing.setAllowedRoles(objectMapper.writeValueAsString(dto.getAllowedRoles()));
            }
            if (dto.getConditions() != null) {
                existing.setConditions(objectMapper.writeValueAsString(dto.getConditions()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize policy data", e);
        }

        // Regenerate DRL
        String drl = generateDrl(dto);
        existing.setGeneratedDrl(drl);

        AccessPolicy saved = accessPolicyRepository.save(existing);
        
        // Rebuild rules
        dynamicRuleService.rebuildRules();
        
        return saved;
    }

    @Transactional
    public void deletePolicy(Long id) {
        accessPolicyRepository.deleteById(id);
        dynamicRuleService.rebuildRules();
    }

    @Transactional
    public AccessPolicy togglePolicy(Long id, boolean enabled) {
        AccessPolicy policy = getPolicyById(id);
        policy.setEnabled(enabled);
        AccessPolicy saved = accessPolicyRepository.save(policy);
        dynamicRuleService.rebuildRules();
        return saved;
    }

    /**
     * Regenerate DRL for all existing policies
     * Useful after fixing regex conversion logic
     */
    @Transactional
    public void regenerateAllPoliciesDrl() {
        System.out.println("Regenerating DRL for all existing policies...");
        List<AccessPolicy> policies = accessPolicyRepository.findAll();
        
        for (AccessPolicy policy : policies) {
            try {
                PolicyDTO dto = convertEntityToDto(policy);
                String newDrl = generateDrl(dto);
                policy.setGeneratedDrl(newDrl);
                accessPolicyRepository.save(policy);
                System.out.println("  ✓ Regenerated DRL for: " + policy.getPolicyName());
            } catch (Exception e) {
                System.err.println("  ✗ Failed to regenerate DRL for " + policy.getPolicyName() + ": " + e.getMessage());
            }
        }
        
        // Rebuild rules with updated DRL
        dynamicRuleService.rebuildRules();
        System.out.println("✓ All policies DRL regenerated and rules rebuilt");
    }

    /**
     * Convert AccessPolicy entity to PolicyDTO
     */
    private PolicyDTO convertEntityToDto(AccessPolicy policy) {
        PolicyDTO dto = new PolicyDTO();
        dto.setId(policy.getId());
        dto.setPolicyName(policy.getPolicyName());
        dto.setDescription(policy.getDescription());
        dto.setEndpoint(policy.getEndpoint());
        dto.setHttpMethod(policy.getHttpMethod());
        dto.setEffect(policy.getEffect());
        dto.setPriority(policy.getPriority());
        dto.setEnabled(policy.isEnabled());

        try {
            if (policy.getAllowedRoles() != null && !policy.getAllowedRoles().isEmpty()) {
                @SuppressWarnings("unchecked")
                List<String> roles = objectMapper.readValue(policy.getAllowedRoles(), List.class);
                dto.setAllowedRoles(roles);
            }
            if (policy.getConditions() != null && !policy.getConditions().isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, ConditionDTO> conditions = objectMapper.readValue(
                    policy.getConditions(), 
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, ConditionDTO.class)
                );
                dto.setConditions(conditions);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize policy data", e);
        }

        return dto;
    }

    private AccessPolicy convertDtoToEntity(PolicyDTO dto) {
        AccessPolicy policy = new AccessPolicy();
        policy.setPolicyName(dto.getPolicyName());
        policy.setDescription(dto.getDescription());
        policy.setEndpoint(dto.getEndpoint());
        policy.setHttpMethod(dto.getHttpMethod());
        policy.setEffect(dto.getEffect() != null ? dto.getEffect() : "ALLOW");
        policy.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
        policy.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);

        try {
            if (dto.getAllowedRoles() != null) {
                policy.setAllowedRoles(objectMapper.writeValueAsString(dto.getAllowedRoles()));
            }
            if (dto.getConditions() != null) {
                policy.setConditions(objectMapper.writeValueAsString(dto.getConditions()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize policy data", e);
        }

        return policy;
    }

    /**
     * Generates DRL rule from PolicyDTO
     * This is the core method that converts structured policy to Drools rule
     */
    public String generateDrl(PolicyDTO dto) {
        StringBuilder drl = new StringBuilder();
        
        // Package and imports
        drl.append("package rules.dynamic;\n\n");
        drl.append("import com.hunesion.drool_v2.model.AccessRequest;\n");
        drl.append("import com.hunesion.drool_v2.model.AccessResult;\n\n");

        // Rule definition
        String ruleName = sanitizeRuleName(dto.getPolicyName());
        drl.append("rule \"").append(ruleName).append("\"\n");
        
        // Salience (priority)
        int priority = dto.getPriority() != null ? dto.getPriority() : 0;
        drl.append("    salience ").append(priority).append("\n");
        
        // When clause
        drl.append("    when\n");
        drl.append("        $request : AccessRequest(\n");
        
        // Build conditions
        StringBuilder conditions = new StringBuilder();
        
        // Endpoint matching
        String endpoint = dto.getEndpoint();
        if (endpoint != null && !endpoint.isEmpty()) {
            String regexPattern = convertEndpointToRegex(endpoint);
            conditions.append("            endpointMatches(\"").append(regexPattern).append("\")");
        }
        
        // HTTP method
        if (dto.getHttpMethod() != null && !dto.getHttpMethod().equals("*")) {
            if (conditions.length() > 0) conditions.append(",\n");
            conditions.append("            httpMethod == \"").append(dto.getHttpMethod()).append("\"");
        }
        
        // Role check - at least one role must match
        if (dto.getAllowedRoles() != null && !dto.getAllowedRoles().isEmpty()) {
            if (conditions.length() > 0) conditions.append(",\n");
            String roleCheck = dto.getAllowedRoles().stream()
                    .map(role -> "hasRole(\"" + role + "\")")
                    .collect(Collectors.joining(" || "));
            conditions.append("            (").append(roleCheck).append(")");
        }
        
        // Additional attribute conditions
        if (dto.getConditions() != null && !dto.getConditions().isEmpty()) {
            for (Map.Entry<String, ConditionDTO> entry : dto.getConditions().entrySet()) {
                String attribute = entry.getKey();
                ConditionDTO condition = entry.getValue();
                String conditionStr = buildCondition(attribute, condition);
                if (conditionStr != null) {
                    if (conditions.length() > 0) conditions.append(",\n");
                    conditions.append("            ").append(conditionStr);
                }
            }
        }
        
        drl.append(conditions);
        drl.append("\n        )\n");
        drl.append("        $result : AccessResult(evaluated == false)\n");
        
        // Then clause
        drl.append("    then\n");
        
        if ("ALLOW".equalsIgnoreCase(dto.getEffect())) {
            drl.append("        $result.allow(\"").append(ruleName).append("\");\n");
            drl.append("        System.out.println(\"✓ Access ALLOWED by policy: ").append(ruleName).append("\");\n");
        } else {
            drl.append("        $result.deny(\"").append(ruleName).append("\", \"Access denied by policy\");\n");
            drl.append("        System.out.println(\"✗ Access DENIED by policy: ").append(ruleName).append("\");\n");
        }
        
        drl.append("end\n");
        
        return drl.toString();
    }

    private String sanitizeRuleName(String name) {
        return name.replaceAll("[^a-zA-Z0-9\\s_-]", "").trim();
    }

    private String convertEndpointToRegex(String endpoint) {
        // Convert /api/users/** to /api/users(/.*)? (matches /api/users and /api/users/anything)
        // Convert /api/users/* to /api/users/[^/]* (matches single segment like /api/users/123)
        
        // Handle ** pattern at the end: should match both base path and sub-paths
        if (endpoint.endsWith("/**")) {
            // Remove /** and add (/.*)? to match base path and any sub-paths
            // This allows /api/sales/** to match both /api/sales and /api/sales/anything
            String base = endpoint.substring(0, endpoint.length() - 3);
            return base + "(/.*)?";
        }
        
        // Handle ** in the middle (less common, but handle it)
        if (endpoint.contains("**")) {
            // Replace ** with placeholder first
            String regex = endpoint.replace("**", "__DOUBLE_STAR__");
            // Replace single * with [^/]*
            regex = regex.replace("*", "[^/]*");
            // Replace placeholder with .* (matches any characters including slashes)
            regex = regex.replace("__DOUBLE_STAR__", ".*");
            return regex;
        }
        
        // Handle single * pattern (matches single path segment)
        return endpoint.replace("*", "[^/]*");
    }

    private String buildCondition(String attribute, ConditionDTO condition) {
        String operator = condition.getOperator();
        String value = condition.getValue();
        
        return switch (attribute) {
            case "department" -> buildStringCondition("department", operator, value);
            case "userLevel" -> buildNumericCondition("userLevel", operator, value);
            default -> buildAttributeCondition(attribute, operator, value);
        };
    }

    private String buildStringCondition(String field, String operator, String value) {
        return switch (operator.toLowerCase()) {
            case "equals" -> field + " == \"" + value + "\"";
            case "notequals" -> field + " != \"" + value + "\"";
            case "contains" -> field + " != null && " + field + ".contains(\"" + value + "\")";
            case "matches" -> field + " != null && " + field + ".matches(\"" + value + "\")";
            default -> null;
        };
    }

    private String buildNumericCondition(String field, String operator, String value) {
        return switch (operator.toLowerCase()) {
            case "equals" -> field + " == " + value;
            case "notequals" -> field + " != " + value;
            case "greaterthan" -> field + " != null && " + field + " > " + value;
            case "lessthan" -> field + " != null && " + field + " < " + value;
            case "greaterthanorequal" -> field + " != null && " + field + " >= " + value;
            case "lessthanorequal" -> field + " != null && " + field + " <= " + value;
            default -> null;
        };
    }

    private String buildAttributeCondition(String attribute, String operator, String value) {
        String getter = "attributes.get(\"" + attribute + "\")";
        return switch (operator.toLowerCase()) {
            case "equals" -> getter + " != null && " + getter + ".toString().equals(\"" + value + "\")";
            case "notequals" -> getter + " == null || !" + getter + ".toString().equals(\"" + value + "\")";
            default -> null;
        };
    }

    /**
     * Preview DRL without saving - useful for frontend validation
     */
    public String previewDrl(PolicyDTO dto) {
        return generateDrl(dto);
    }
}
```

### 8.3 Create `AccessControlService.java`

Create `src/main/java/com/hunesion/drool_v2/service/AccessControlService.java`:

```java
package com.hunesion.drool_v2.service;

import com.hunesion.drool_v2.entity.User;
import com.hunesion.drool_v2.model.AccessRequest;
import com.hunesion.drool_v2.model.AccessResult;
import com.hunesion.drool_v2.repository.UserRepository;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AccessControlService - Evaluates access requests against policies using Drools
 */
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
        User user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null) {
            AccessResult result = new AccessResult();
            result.deny("User Not Found", "User does not exist: " + username);
            return result;
        }

        if (!user.isActive()) {
            AccessResult result = new AccessResult();
            result.deny("User Inactive", "User account is disabled");
            return result;
        }

        // Build access request from user data
        AccessRequest request = new AccessRequest();
        request.setUsername(username);
        request.setUserRoles(user.getRoleNames());
        request.setEndpoint(endpoint);
        request.setHttpMethod(httpMethod);
        request.setDepartment(user.getDepartment());
        request.setUserLevel(user.getLevel());
        
        // Copy user attributes
        if (user.getAttributes() != null) {
            user.getAttributes().forEach((k, v) -> request.setAttribute(k, v));
        }

        return evaluateAccess(request);
    }

    /**
     * Evaluate access request directly (for testing or custom requests)
     */
    public AccessResult evaluateAccess(AccessRequest request) {
        KieSession kieSession = dynamicRuleService.newKieSession();
        AccessResult result = new AccessResult();

        try {
            kieSession.insert(request);
            kieSession.insert(result);
            
            int rulesFired = kieSession.fireAllRules();
            System.out.println("Access control rules fired: " + rulesFired + " for " + request.getEndpoint());
            
            // If no rules matched, deny by default
            if (!result.isEvaluated()) {
                result.deny("No Policy Match", "No access policy found for this endpoint");
            }
            
        } finally {
            kieSession.dispose();
        }

        return result;
    }

    /**
     * Check access with a pre-built AccessRequest
     */
    public boolean isAllowed(AccessRequest request) {
        return evaluateAccess(request).isAllowed();
    }

    /**
     * Simple check for username, endpoint, method combination
     */
    public boolean isAllowed(String username, String endpoint, String httpMethod) {
        return checkAccess(username, endpoint, httpMethod).isAllowed();
    }
}
```

---

## Step 9: Create Controller Classes

### 9.1 Create `PolicyController.java`

Create `src/main/java/com/hunesion/drool_v2/controller/PolicyController.java`:

```java
package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.dto.PolicyDTO;
import com.hunesion.drool_v2.entity.AccessPolicy;
import com.hunesion.drool_v2.service.PolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PolicyController - REST API for managing access policies
 * 
 * This is the admin endpoint for CRUD operations on policies
 * Only users with ADMIN role should access these endpoints
 */
@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;

    @Autowired
    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    /**
     * Get all policies
     */
    @GetMapping
    public ResponseEntity<List<AccessPolicy>> getAllPolicies() {
        return ResponseEntity.ok(policyService.getAllPolicies());
    }

    /**
     * Get only enabled policies
     */
    @GetMapping("/enabled")
    public ResponseEntity<List<AccessPolicy>> getEnabledPolicies() {
        return ResponseEntity.ok(policyService.getEnabledPolicies());
    }

    /**
     * Get a single policy by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccessPolicy> getPolicyById(@PathVariable Long id) {
        return ResponseEntity.ok(policyService.getPolicyById(id));
    }

    /**
     * Create a new policy
     * 
     * Example request body:
     * {
     *   "policyName": "Manager Reports Access",
     *   "description": "Allow managers to access reports",
     *   "endpoint": "/api/reports/**",
     *   "httpMethod": "GET",
     *   "allowedRoles": ["ADMIN", "MANAGER"],
     *   "conditions": {
     *     "department": {"operator": "equals", "value": "SALES"}
     *   },
     *   "effect": "ALLOW",
     *   "priority": 10
     * }
     */
    @PostMapping
    public ResponseEntity<AccessPolicy> createPolicy(@RequestBody PolicyDTO dto) {
        AccessPolicy created = policyService.createPolicy(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing policy
     */
    @PutMapping("/{id}")
    public ResponseEntity<AccessPolicy> updatePolicy(@PathVariable Long id, @RequestBody PolicyDTO dto) {
        AccessPolicy updated = policyService.updatePolicy(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a policy
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deletePolicy(@PathVariable Long id) {
        policyService.deletePolicy(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Policy deleted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Enable or disable a policy
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<AccessPolicy> togglePolicy(@PathVariable Long id, 
                                                      @RequestParam boolean enabled) {
        AccessPolicy updated = policyService.togglePolicy(id, enabled);
        return ResponseEntity.ok(updated);
    }

    /**
     * Preview generated DRL without saving
     * Useful for frontend validation
     */
    @PostMapping("/preview-drl")
    public ResponseEntity<Map<String, String>> previewDrl(@RequestBody PolicyDTO dto) {
        String drl = policyService.previewDrl(dto);
        Map<String, String> response = new HashMap<>();
        response.put("drl", drl);
        return ResponseEntity.ok(response);
    }
}
```

### 9.2 Create `UserController.java`

Create `src/main/java/com/hunesion/drool_v2/controller/UserController.java`:

```java
package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.entity.Role;
import com.hunesion.drool_v2.entity.User;
import com.hunesion.drool_v2.repository.RoleRepository;
import com.hunesion.drool_v2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UserController - REST API for user management
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public UserController(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            return ResponseEntity.badRequest().build();
        }
        User saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        return userRepository.findById(id)
                .map(existing -> {
                    existing.setEmail(user.getEmail());
                    existing.setDepartment(user.getDepartment());
                    existing.setLevel(user.getLevel());
                    existing.setActive(user.isActive());
                    existing.setAttributes(user.getAttributes());
                    return ResponseEntity.ok(userRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{userId}/roles/{roleName}")
    public ResponseEntity<User> addRoleToUser(@PathVariable Long userId, 
                                               @PathVariable String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        
        user.addRole(role);
        return ResponseEntity.ok(userRepository.save(user));
    }

    @DeleteMapping("/{userId}/roles/{roleName}")
    public ResponseEntity<User> removeRoleFromUser(@PathVariable Long userId, 
                                                    @PathVariable String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        
        user.removeRole(role);
        return ResponseEntity.ok(userRepository.save(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User deleted successfully");
        return ResponseEntity.ok(response);
    }
}
```

### 9.3 Create `RoleController.java`

Create `src/main/java/com/hunesion/drool_v2/controller/RoleController.java`:

```java
package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.entity.Role;
import com.hunesion.drool_v2.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RoleController - REST API for role management
 */
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleRepository roleRepository;

    @Autowired
    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Role> getRoleById(@PathVariable Long id) {
        return roleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<Role> getRoleByName(@PathVariable String name) {
        return roleRepository.findByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Role> createRole(@RequestBody Role role) {
        if (roleRepository.existsByName(role.getName())) {
            return ResponseEntity.badRequest().build();
        }
        Role saved = roleRepository.save(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Role> updateRole(@PathVariable Long id, @RequestBody Role role) {
        return roleRepository.findById(id)
                .map(existing -> {
                    existing.setDescription(role.getDescription());
                    return ResponseEntity.ok(roleRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteRole(@PathVariable Long id) {
        roleRepository.deleteById(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Role deleted successfully");
        return ResponseEntity.ok(response);
    }
}
```

### 9.4 Create `AccessCheckController.java`

Create `src/main/java/com/hunesion/drool_v2/controller/AccessCheckController.java`:

```java
package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.model.AccessRequest;
import com.hunesion.drool_v2.model.AccessResult;
import com.hunesion.drool_v2.service.AccessControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AccessCheckController - REST API for testing access control
 * 
 * This controller provides endpoints to test if a user has access to specific endpoints
 * Useful for frontend to check access before making requests
 */
@RestController
@RequestMapping("/api/access")
public class AccessCheckController {

    private final AccessControlService accessControlService;

    @Autowired
    public AccessCheckController(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    /**
     * Check if a user has access to a specific endpoint
     * 
     * Example: GET /api/access/check?username=john&endpoint=/api/reports&method=GET
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkAccess(
            @RequestParam String username,
            @RequestParam String endpoint,
            @RequestParam(defaultValue = "GET") String method) {
        
        AccessResult result = accessControlService.checkAccess(username, endpoint, method);
        
        Map<String, Object> response = new HashMap<>();
        response.put("username", username);
        response.put("endpoint", endpoint);
        response.put("method", method);
        response.put("allowed", result.isAllowed());
        response.put("matchedPolicy", result.getMatchedPolicyName());
        if (!result.isAllowed()) {
            response.put("reason", result.getDenialReason());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Check access with full AccessRequest body
     * Allows testing with custom attributes
     */
    @PostMapping("/check")
    public ResponseEntity<AccessResult> checkAccessWithRequest(@RequestBody AccessRequest request) {
        AccessResult result = accessControlService.evaluateAccess(request);
        return ResponseEntity.ok(result);
    }
}
```

### 9.5 Create `SampleProtectedController.java`

Create `src/main/java/com/hunesion/drool_v2/controller/SampleProtectedController.java`:

```java
package com.hunesion.drool_v2.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SampleProtectedController - Sample endpoints to test ABAC policies
 * 
 * These endpoints are protected by the AccessControlInterceptor
 * Access is determined by policies configured via PolicyController
 */
@RestController
@RequestMapping("/api")
public class SampleProtectedController {

    // ==================== REPORTS ENDPOINTS ====================
    // Policy: Manager Reports Access (MANAGER role required)

    @GetMapping("/reports")
    public ResponseEntity<Map<String, Object>> getReports() {
        Map<String, Object> response = new HashMap<>();
        response.put("endpoint", "/api/reports");
        response.put("data", List.of(
                Map.of("id", 1, "name", "Sales Report Q4", "type", "SALES"),
                Map.of("id", 2, "name", "Inventory Report", "type", "INVENTORY"),
                Map.of("id", 3, "name", "Employee Report", "type", "HR")
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reports/{id}")
    public ResponseEntity<Map<String, Object>> getReportById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        response.put("endpoint", "/api/reports/" + id);
        response.put("report", Map.of(
                "id", id,
                "name", "Sample Report " + id,
                "content", "This is the content of report " + id
        ));
        return ResponseEntity.ok(response);
    }

    // ==================== PROFILE ENDPOINTS ====================
    // Policy: User Profile Access (USER, MANAGER, VIEWER roles)

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@RequestHeader("X-Username") String username) {
        Map<String, Object> response = new HashMap<>();
        response.put("endpoint", "/api/profile");
        response.put("username", username);
        response.put("message", "This is your profile data");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestHeader("X-Username") String username,
                                                              @RequestBody Map<String, Object> profileData) {
        Map<String, Object> response = new HashMap<>();
        response.put("endpoint", "/api/profile");
        response.put("message", "Profile updated successfully for " + username);
        response.put("updatedData", profileData);
        return ResponseEntity.ok(response);
    }

    // ==================== SALES ENDPOINTS ====================
    // Policy: Sales Department Data Access (USER/MANAGER + department=SALES)

    @GetMapping("/sales")
    public ResponseEntity<Map<String, Object>> getSalesData() {
        Map<String, Object> response = new HashMap<>();
        response.put("endpoint", "/api/sales");
        response.put("data", List.of(
                Map.of("id", 1, "product", "Product A", "amount", 15000),
                Map.of("id", 2, "product", "Product B", "amount", 23000),
                Map.of("id", 3, "product", "Product C", "amount", 8500)
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sales/{id}")
    public ResponseEntity<Map<String, Object>> getSaleById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        response.put("endpoint", "/api/sales/" + id);
        response.put("sale", Map.of(
                "id", id,
                "product", "Product " + id,
                "customer", "Customer " + id,
                "amount", 10000 + (id * 1000)
        ));
        return ResponseEntity.ok(response);
    }

    // ==================== MANAGEMENT ENDPOINTS ====================
    // Policy: Management Level Access (USER/MANAGER + level >= 5)

    @GetMapping("/management")
    public ResponseEntity<Map<String, Object>> getManagementData() {
        Map<String, Object> response = new HashMap<>();
        response.put("endpoint", "/api/management");
        response.put("data", Map.of(
                "totalEmployees", 150,
                "totalRevenue", 5000000,
                "activeProjects", 12
        ));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/management/decisions")
    public ResponseEntity<Map<String, Object>> createDecision(@RequestBody Map<String, Object> decision) {
        Map<String, Object> response = new HashMap<>();
        response.put("endpoint", "/api/management/decisions");
        response.put("message", "Decision recorded successfully");
        response.put("decision", decision);
        return ResponseEntity.ok(response);
    }

    // ==================== PUBLIC ENDPOINTS ====================
    // Policy: Viewer Public Data Access (VIEWER, USER, MANAGER)

    @GetMapping("/public/info")
    public ResponseEntity<Map<String, Object>> getPublicInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("endpoint", "/api/public/info");
        response.put("company", "Hunesion Inc.");
        response.put("version", "1.0.0");
        response.put("message", "Public information endpoint");
        return ResponseEntity.ok(response);
    }

    // ==================== ADMIN ONLY ENDPOINTS ====================
    // Policy: Admin Full Access (ADMIN role only)

    @GetMapping("/admin/dashboard")
    public ResponseEntity<Map<String, Object>> getAdminDashboard() {
        Map<String, Object> response = new HashMap<>();
        response.put("endpoint", "/api/admin/dashboard");
        response.put("data", Map.of(
                "totalUsers", 500,
                "activePolicies", 10,
                "systemHealth", "HEALTHY"
        ));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/settings")
    public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> settings) {
        Map<String, Object> response = new HashMap<>();
        response.put("endpoint", "/api/admin/settings");
        response.put("message", "Settings updated successfully");
        response.put("settings", settings);
        return ResponseEntity.ok(response);
    }
}
```

---

## Step 10: Create Interceptor

### 10.1 Create `AccessControlInterceptor.java`

Create `src/main/java/com/hunesion/drool_v2/interceptor/AccessControlInterceptor.java`:

```java
package com.hunesion.drool_v2.interceptor;

import com.hunesion.drool_v2.model.AccessResult;
import com.hunesion.drool_v2.service.AccessControlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Set;

/**
 * AccessControlInterceptor - Intercepts HTTP requests and evaluates access policies
 * 
 * This interceptor:
 * - Extracts user info from request header (X-Username)
 * - Evaluates access against Drools policies
 * - Allows or denies the request based on policy result
 */
@Component
public class AccessControlInterceptor implements HandlerInterceptor {

    private final AccessControlService accessControlService;

    // Endpoints that bypass access control (public endpoints)
    private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/public",
            "/error"
    );

    // Endpoints for admin policy management (only ADMIN role)
    private static final Set<String> ADMIN_ONLY_PATTERNS = Set.of(
            "/api/policies"
    );

    @Autowired
    public AccessControlInterceptor(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws IOException {
        
        String endpoint = request.getRequestURI();
        String method = request.getMethod();

        // Skip OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // Skip public endpoints
        if (isPublicEndpoint(endpoint)) {
            return true;
        }

        // Get username from header (in real app, this would come from JWT/session)
        String username = request.getHeader("X-Username");
        
        if (username == null || username.isEmpty()) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "Missing X-Username header", null);
            return false;
        }

        // Evaluate access
        AccessResult result = accessControlService.checkAccess(username, endpoint, method);

        if (result.isAllowed()) {
            System.out.println("✓ Access granted for " + username + " to " + method + " " + endpoint);
            return true;
        } else {
            System.out.println("✗ Access denied for " + username + " to " + method + " " + endpoint 
                    + " - Reason: " + result.getDenialReason());
            sendError(response, HttpServletResponse.SC_FORBIDDEN, 
                    result.getDenialReason(), result.getMatchedPolicyName());
            return false;
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

---

## Step 11: Create Configuration Classes

### 11.1 Create `WebConfig.java`

Create `src/main/java/com/hunesion/drool_v2/config/WebConfig.java`:

```java
package com.hunesion.drool_v2.config;

import com.hunesion.drool_v2.interceptor.AccessControlInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AccessControlInterceptor accessControlInterceptor;

    @Autowired
    public WebConfig(AccessControlInterceptor accessControlInterceptor) {
        this.accessControlInterceptor = accessControlInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessControlInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/public/**",
                        "/error"
                );
    }
}
```

### 11.2 Create `OpenApiConfig.java`

Create `src/main/java/com/hunesion/drool_v2/config/OpenApiConfig.java`:

```java
package com.hunesion.drool_v2.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI Configuration for Swagger UI
 * Adds X-Username header globally to all endpoints
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "X-Username";
        
        return new OpenAPI()
                .info(new Info()
                        .title("ABAC Policy Management API")
                        .version("1.0.0")
                        .description("Attribute-Based Access Control system using Drools. " +
                                "Use the X-Username header to specify the user making the request."))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name("X-Username")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("Username for authentication (e.g., admin, manager, john, jane, viewer)")));
    }
}
```

### 11.3 Create `DroolsConfig.java`

Create `src/main/java/com/hunesion/drool_v2/config/DroolsConfig.java`:

```java
package com.hunesion.drool_v2.config;

/**
 * DroolsConfig - DEPRECATED
 * 
 * This configuration has been replaced by DynamicRuleService which provides:
 * - Dynamic rule loading from database
 * - Hot-reloading of rules when policies change
 * - Thread-safe KieSession creation
 * 
 * See: com.hunesion.drool_v2.service.DynamicRuleService
 */
// @Configuration - Disabled in favor of DynamicRuleService
public class DroolsConfig {
    // Kept for reference - functionality moved to DynamicRuleService
}
```

---

## Step 12: Create DTO Classes

### 12.1 Create `PolicyDTO.java`

Create `src/main/java/com/hunesion/drool_v2/dto/PolicyDTO.java`:

```java
package com.hunesion.drool_v2.dto;

import java.util.List;
import java.util.Map;

/**
 * PolicyDTO - Data Transfer Object for creating/updating access policies from frontend
 * This structured format is converted to DRL by the backend
 */
public class PolicyDTO {

    private Long id;
    private String policyName;
    private String description;
    private String endpoint;
    private String httpMethod;
    private List<String> allowedRoles;
    private Map<String, ConditionDTO> conditions;
    private String effect;
    private Integer priority;
    private Boolean enabled;

    public PolicyDTO() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public List<String> getAllowedRoles() {
        return allowedRoles;
    }

    public void setAllowedRoles(List<String> allowedRoles) {
        this.allowedRoles = allowedRoles;
    }

    public Map<String, ConditionDTO> getConditions() {
        return conditions;
    }

    public void setConditions(Map<String, ConditionDTO> conditions) {
        this.conditions = conditions;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "PolicyDTO{" +
                "policyName='" + policyName + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                ", allowedRoles=" + allowedRoles +
                ", effect='" + effect + '\'' +
                '}';
    }
}
```

### 12.2 Create `ConditionDTO.java`

Create `src/main/java/com/hunesion/drool_v2/dto/ConditionDTO.java`:

```java
package com.hunesion.drool_v2.dto;

/**
 * ConditionDTO - Represents a condition in a policy
 * 
 * Supported operators:
 * - equals: attribute == value
 * - notEquals: attribute != value
 * - greaterThan: attribute > value
 * - lessThan: attribute < value
 * - greaterThanOrEqual: attribute >= value
 * - lessThanOrEqual: attribute <= value
 * - contains: collection contains value
 * - matches: string matches regex pattern
 */
public class ConditionDTO {

    private String operator;
    private String value;

    public ConditionDTO() {
    }

    public ConditionDTO(String operator, String value) {
        this.operator = operator;
        this.value = value;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "ConditionDTO{" +
                "operator='" + operator + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
```

---

## Step 13: Create Main Application Class

### 13.1 Create `DroolV2Application.java`

Create `src/main/java/com/hunesion/drool_v2/DroolV2Application.java`:

```java
package com.hunesion.drool_v2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DroolV2Application {

    public static void main(String[] args) {
        SpringApplication.run(DroolV2Application.class, args);
    }

}
```

---

## Step 14: Database Setup

### 14.1 Create PostgreSQL Database

**Option A: Using Docker (Recommended)**

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:16-alpine
    container_name: abac-postgres
    environment:
      POSTGRES_DB: abacdb
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: Rin25052001
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./src/main/resources/sql:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - abac-network

volumes:
  postgres_data:
    driver: local

networks:
  abac-network:
    driver: bridge
```

Start PostgreSQL:

```bash
docker-compose up -d postgres
```

**Option B: Local PostgreSQL**

```sql
CREATE DATABASE abacdb;
CREATE USER postgres WITH PASSWORD 'Rin25052001';
GRANT ALL PRIVILEGES ON DATABASE abacdb TO postgres;
```

### 14.2 Create Initial Data SQL

Create `src/main/resources/sql/initial_data.sql` with the sample data. Copy the complete SQL from the existing `initial_data.sql` file in your project. This file should include:

- Roles (ADMIN, MANAGER, USER, VIEWER)
- Users with role assignments
- Access Policies with generated DRL

**Note**: The SQL file should be placed in `src/main/resources/sql/` and will be automatically executed when using Docker, or you can run it manually against your PostgreSQL database.

---

## Step 15: Docker Configuration (Optional)

### 15.1 Create `Dockerfile`

Create `Dockerfile`:

```dockerfile
# Multi-stage build for Spring Boot application
# Stage 1: Build the application
FROM gradle:8.14.2-jdk21 AS build

WORKDIR /app

# Copy Gradle files
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Copy source code
COPY src ./src

# Build the application
RUN gradle clean build -x test --no-daemon

# Stage 2: Run the application
FROM openjdk:21-jdk-slim

WORKDIR /app

# Install wget for health check
RUN apt-get update && \
    apt-get install -y wget && \
    rm -rf /var/lib/apt/lists/*

# Create a non-root user
RUN groupadd -r spring && useradd -r -g spring spring

# Copy the JAR file from build stage
COPY --from=build /app/build/libs/drool_v2-0.0.1-SNAPSHOT.jar app.jar

# Change ownership
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/api/public || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Step 16: Run and Test

### 16.1 Build the Project

```bash
./gradlew clean build
```

### 16.2 Run the Application

```bash
./gradlew bootRun
```

Or using Java directly:

```bash
java -jar build/libs/drool_v2-0.0.1-SNAPSHOT.jar
```

### 16.3 Verify Application is Running

1. Check logs for successful startup
2. Access Swagger UI: http://localhost:8081/swagger-ui.html
3. Test public endpoint: http://localhost:8081/api/public

### 16.4 Test Access Control

Use Postman or curl to test:

```bash
# Test with admin user (should be allowed)
curl -H "X-Username: admin" http://localhost:8081/api/users

# Test with viewer user (should be denied for some endpoints)
curl -H "X-Username: viewer" http://localhost:8081/api/users

# Test access check endpoint
curl "http://localhost:8081/api/access/check?username=admin&endpoint=/api/users&method=GET"
```

---

## Step 17: Verification Checklist

- [ ] Project builds successfully (`./gradlew build`)
- [ ] Application starts without errors
- [ ] Database connection works
- [ ] Swagger UI is accessible at http://localhost:8081/swagger-ui.html
- [ ] Public endpoint works: `/api/public`
- [ ] Access control interceptor works
- [ ] Policies can be created via API
- [ ] Access evaluation works correctly
- [ ] Rules are loaded from database on startup
- [ ] Rules are hot-reloaded when policies change

---

## Troubleshooting

### Common Issues

1. **Port 8081 already in use**
   - Change port in `application.yml` or stop the conflicting service
   - Example: Change `server.port: 8081` to `server.port: 8082`

2. **Database connection failed**
   - Verify PostgreSQL is running: `docker ps` or check local PostgreSQL service
   - Check credentials in `application.yml`
   - Ensure database `abacdb` exists
   - Test connection: `psql -h localhost -U postgres -d abacdb`

3. **Gradle build fails**
   - Ensure JDK 21 is installed: `java -version`
   - Run `./gradlew clean build --refresh-dependencies`
   - Check internet connection for downloading dependencies

4. **Drools rules not loading**
   - Check `kmodule.xml` is in correct location: `src/main/resources/META-INF/`
   - Verify `DynamicRuleService` is properly configured
   - Check application logs for rule loading errors
   - Ensure policies exist in database with valid DRL

5. **Access denied when it should be allowed**
   - Check user roles are correctly assigned
   - Verify policy is enabled (`enabled = true`)
   - Check policy priority (higher priority rules are evaluated first)
   - Review endpoint pattern matching (use `/**` for sub-paths)

6. **Rules not hot-reloading**
   - Ensure `DynamicRuleService.rebuildRules()` is called after policy changes
   - Check transaction is committed before rebuilding rules
   - Verify no errors in rule compilation

---

## Next Steps

After setup is complete:

1. **Review API Documentation**
   - Access Swagger UI: http://localhost:8081/swagger-ui.html
   - Test endpoints using the interactive UI
   - Review the main `README.md` for detailed API usage

2. **Create Custom Policies**
   - Use the PolicyController to create new access policies
   - Test different endpoint patterns and conditions
   - Experiment with priority levels

3. **Test Access Control Scenarios**
   - Test different user roles accessing various endpoints
   - Verify attribute-based conditions (department, level)
   - Test policy priority and rule evaluation order

4. **Customize for Your Needs**
   - Add new entity attributes
   - Create custom conditions
   - Extend the DRL generation logic
   - Add more complex policy rules

5. **Production Considerations**
   - Change database passwords
   - Configure proper authentication (JWT/OAuth)
   - Set up logging and monitoring
   - Configure CORS properly
   - Add rate limiting
   - Set up CI/CD pipeline

---

## Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Drools Documentation](https://www.drools.org/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Gradle Documentation](https://docs.gradle.org/)
- [Spring Data JPA Documentation](https://spring.io/projects/spring-data-jpa)
- [OpenAPI/Swagger Documentation](https://swagger.io/specification/)

---

## Summary

This guide has walked you through creating a complete ABAC (Attribute-Based Access Control) system with:

- ✅ Spring Boot REST API
- ✅ Drools rule engine for dynamic policy evaluation
- ✅ PostgreSQL database for persistence
- ✅ Dynamic rule loading and hot-reloading
- ✅ Role-based and attribute-based access control
- ✅ Swagger UI for API documentation
- ✅ Docker support for easy deployment

**Project Setup Complete!** 🎉

You should now have a fully functional ABAC system with Drools integration. For detailed API usage and examples, refer to the main `README.md` file.