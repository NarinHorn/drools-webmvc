//package com.hunesion.drool_v2.runner;
//
//import com.hunesion.drool_v2.dto.ConditionDTO;
//import com.hunesion.drool_v2.dto.PolicyDTO;
//import com.hunesion.drool_v2.entity.Role;
//import com.hunesion.drool_v2.entity.User;
//import com.hunesion.drool_v2.repository.RoleRepository;
//import com.hunesion.drool_v2.repository.UserRepository;
//import com.hunesion.drool_v2.service.PolicyService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * DataInitializer - Creates sample data for testing the ABAC system
// *
// * Creates:
// * - Roles: ADMIN, MANAGER, USER, VIEWER
// * - Users with different roles and departments
// * - Sample access policies
// *
// * DISABLED: This class is commented out to allow manual database initialization.
// * Use the generated SQL script (initial_data.sql) to insert data manually.
// */
////@Component
////@Order(1)
//public class DataInitializer implements CommandLineRunner {
//
//    private final RoleRepository roleRepository;
//    private final UserRepository userRepository;
//    private final PolicyService policyService;
//
//    @Autowired
//    public DataInitializer(RoleRepository roleRepository,
//                          UserRepository userRepository,
//                          PolicyService policyService) {
//        this.roleRepository = roleRepository;
//        this.userRepository = userRepository;
//        this.policyService = policyService;
//    }
//
//    @Override
//    public void run(String... args) {
//        System.out.println("\n========================================");
//        System.out.println("INITIALIZING ABAC DEMO DATA");
//        System.out.println("========================================\n");
//
//        createRoles();
//        createUsers();
//        createSamplePolicies();
//
//        // Regenerate all policies' DRL to ensure they use the latest regex conversion logic
//        // This fixes any existing policies that were created with the old buggy regex
//        System.out.println("\nRegenerating all policies' DRL with fixed regex patterns...");
//        policyService.regenerateAllPoliciesDrl();
//
//        System.out.println("\n========================================");
//        System.out.println("DATA INITIALIZATION COMPLETED");
//        System.out.println("========================================\n");
//    }
//
//    private void createRoles() {
//        System.out.println("Creating roles...");
//
//        if (!roleRepository.existsByName("ADMIN")) {
//            roleRepository.save(new Role("ADMIN", "System Administrator with full access"));
//        }
//        if (!roleRepository.existsByName("MANAGER")) {
//            roleRepository.save(new Role("MANAGER", "Department Manager"));
//        }
//        if (!roleRepository.existsByName("USER")) {
//            roleRepository.save(new Role("USER", "Regular User"));
//        }
//        if (!roleRepository.existsByName("VIEWER")) {
//            roleRepository.save(new Role("VIEWER", "Read-only access"));
//        }
//
//        System.out.println("  ✓ Roles created: ADMIN, MANAGER, USER, VIEWER");
//    }
//
//    private void createUsers() {
//        System.out.println("Creating users...");
//
//        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
//        Role managerRole = roleRepository.findByName("MANAGER").orElseThrow();
//        Role userRole = roleRepository.findByName("USER").orElseThrow();
//        Role viewerRole = roleRepository.findByName("VIEWER").orElseThrow();
//
//        // Admin user
//        if (!userRepository.existsByUsername("admin")) {
//            User admin = new User("admin", "admin123", "admin@example.com");
//            admin.setDepartment("IT");
//            admin.setLevel(10);
//            admin.addRole(adminRole);
//            userRepository.save(admin);
//            System.out.println("  ✓ Created user: admin (ADMIN)");
//        }
//
//        // Manager user
//        if (!userRepository.existsByUsername("manager")) {
//            User manager = new User("manager", "manager123", "manager@example.com");
//            manager.setDepartment("SALES");
//            manager.setLevel(5);
//            manager.addRole(managerRole);
//            userRepository.save(manager);
//            System.out.println("  ✓ Created user: manager (MANAGER, SALES dept)");
//        }
//
//        // Regular user in SALES
//        if (!userRepository.existsByUsername("john")) {
//            User john = new User("john", "john123", "john@example.com");
//            john.setDepartment("SALES");
//            john.setLevel(3);
//            john.addRole(userRole);
//            userRepository.save(john);
//            System.out.println("  ✓ Created user: john (USER, SALES dept)");
//        }
//
//        // Regular user in HR
//        if (!userRepository.existsByUsername("jane")) {
//            User jane = new User("jane", "jane123", "jane@example.com");
//            jane.setDepartment("HR");
//            jane.setLevel(3);
//            jane.addRole(userRole);
//            userRepository.save(jane);
//            System.out.println("  ✓ Created user: jane (USER, HR dept)");
//        }
//
//        // Viewer user
//        if (!userRepository.existsByUsername("viewer")) {
//            User viewer = new User("viewer", "viewer123", "viewer@example.com");
//            viewer.setDepartment("GUEST");
//            viewer.setLevel(1);
//            viewer.addRole(viewerRole);
//            userRepository.save(viewer);
//            System.out.println("  ✓ Created user: viewer (VIEWER)");
//        }
//    }
//
//    private void createSamplePolicies() {
//        System.out.println("Creating sample access policies...");
//
//        try {
//            // Policy 1: Admin has full access to everything
//            PolicyDTO adminPolicy = new PolicyDTO();
//            adminPolicy.setPolicyName("Admin Full Access");
//            adminPolicy.setDescription("Administrators have full access to all endpoints");
//            adminPolicy.setEndpoint("/api/**");
//            adminPolicy.setHttpMethod("*");
//            adminPolicy.setAllowedRoles(List.of("ADMIN"));
//            adminPolicy.setEffect("ALLOW");
//            adminPolicy.setPriority(100);
//            policyService.createPolicy(adminPolicy);
//            System.out.println("  ✓ Policy: Admin Full Access");
//
//            // Policy 2: Managers can access reports
//            PolicyDTO managerReports = new PolicyDTO();
//            managerReports.setPolicyName("Manager Reports Access");
//            managerReports.setDescription("Managers can view reports");
//            managerReports.setEndpoint("/api/reports/**");
//            managerReports.setHttpMethod("GET");
//            managerReports.setAllowedRoles(List.of("MANAGER"));
//            managerReports.setEffect("ALLOW");
//            managerReports.setPriority(50);
//            policyService.createPolicy(managerReports);
//            System.out.println("  ✓ Policy: Manager Reports Access");
//
//            // Policy 3: Users can access their own profile
//            PolicyDTO userProfile = new PolicyDTO();
//            userProfile.setPolicyName("User Profile Access");
//            userProfile.setDescription("Users can access profile endpoints");
//            userProfile.setEndpoint("/api/profile/**");
//            userProfile.setHttpMethod("*");
//            userProfile.setAllowedRoles(List.of("USER", "MANAGER", "VIEWER"));
//            userProfile.setEffect("ALLOW");
//            userProfile.setPriority(30);
//            policyService.createPolicy(userProfile);
//            System.out.println("  ✓ Policy: User Profile Access");
//
//            // Policy 4: Sales department access to sales data
//            PolicyDTO salesAccess = new PolicyDTO();
//            salesAccess.setPolicyName("Sales Department Data Access");
//            salesAccess.setDescription("Sales department users can access sales data");
//            salesAccess.setEndpoint("/api/sales/**");
//            salesAccess.setHttpMethod("GET");
//            salesAccess.setAllowedRoles(List.of("USER", "MANAGER"));
//            Map<String, ConditionDTO> salesConditions = new HashMap<>();
//            salesConditions.put("department", new ConditionDTO("equals", "SALES"));
//            salesAccess.setConditions(salesConditions);
//            salesAccess.setEffect("ALLOW");
//            salesAccess.setPriority(40);
//            policyService.createPolicy(salesAccess);
//            System.out.println("  ✓ Policy: Sales Department Data Access");
//
//            // Policy 5: Level 5+ can access management endpoints
//            PolicyDTO managementAccess = new PolicyDTO();
//            managementAccess.setPolicyName("Management Level Access");
//            managementAccess.setDescription("Users with level 5+ can access management");
//            managementAccess.setEndpoint("/api/management/**");
//            managementAccess.setHttpMethod("*");
//            managementAccess.setAllowedRoles(List.of("USER", "MANAGER"));
//            Map<String, ConditionDTO> levelConditions = new HashMap<>();
//            levelConditions.put("userLevel", new ConditionDTO("greaterThanOrEqual", "5"));
//            managementAccess.setConditions(levelConditions);
//            managementAccess.setEffect("ALLOW");
//            managementAccess.setPriority(35);
//            policyService.createPolicy(managementAccess);
//            System.out.println("  ✓ Policy: Management Level Access");
//
//            // Policy 6: Viewers can only read public data
//            PolicyDTO viewerPublic = new PolicyDTO();
//            viewerPublic.setPolicyName("Viewer Public Data Access");
//            viewerPublic.setDescription("Viewers can access public data endpoints");
//            viewerPublic.setEndpoint("/api/public/**");
//            viewerPublic.setHttpMethod("GET");
//            viewerPublic.setAllowedRoles(List.of("VIEWER", "USER", "MANAGER"));
//            viewerPublic.setEffect("ALLOW");
//            viewerPublic.setPriority(20);
//            policyService.createPolicy(viewerPublic);
//            System.out.println("  ✓ Policy: Viewer Public Data Access");
//
//        } catch (Exception e) {
//            System.out.println("  Note: Some policies may already exist - " + e.getMessage());
//        }
//    }
//}
