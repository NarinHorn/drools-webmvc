package com.hunesion.drool_v2.controller;

import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(
        summary = "Get all reports",
        description = "Retrieves a list of all available reports. Access is controlled by policies requiring MANAGER role or higher."
    )
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

    @Operation(
        summary = "Get report by ID",
        description = "Retrieves a specific report by its ID. Access is controlled by policies requiring MANAGER role or higher."
    )
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

    @Operation(
        summary = "Get user profile",
        description = "Retrieves the profile information for the authenticated user. The username is extracted from the X-Username header. Access is typically granted to USER, MANAGER, or VIEWER roles."
    )
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@RequestHeader("X-Username") String username) {
        Map<String, Object> response = new HashMap<>();
        response.put("endpoint", "/api/profile");
        response.put("username", username);
        response.put("message", "This is your profile data");
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Update user profile",
        description = "Updates the profile information for the authenticated user. The username is extracted from the X-Username header. Access is typically granted to USER or MANAGER roles."
    )
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

    @Operation(
        summary = "Get sales data",
        description = "Retrieves sales data including products and amounts. Access requires USER or MANAGER role AND the user must belong to the SALES department."
    )
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

    @Operation(
        summary = "Get sale by ID",
        description = "Retrieves a specific sale record by its ID. Access requires USER or MANAGER role AND the user must belong to the SALES department."
    )
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

    @Operation(
        summary = "Get management dashboard data",
        description = "Retrieves high-level management data including total employees, revenue, and active projects. Access requires USER or MANAGER role AND user level must be 5 or higher."
    )
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

    @Operation(
        summary = "Create management decision",
        description = "Records a management decision. Access requires USER or MANAGER role AND user level must be 5 or higher."
    )
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

    @Operation(
        summary = "Get public information",
        description = "Retrieves public company information. This endpoint is accessible to VIEWER, USER, and MANAGER roles."
    )
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

    @Operation(
        summary = "Get admin dashboard",
        description = "Retrieves administrative dashboard data including system statistics. Access is restricted to users with ADMIN role only."
    )
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

    @Operation(
        summary = "Update system settings",
        description = "Updates system-wide settings. Access is restricted to users with ADMIN role only. Use with caution as this affects the entire system."
    )
    @PostMapping("/admin/settings")
    public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> settings) {
        Map<String, Object> response = new HashMap<>();
        response.put("endpoint", "/api/admin/settings");
        response.put("message", "Settings updated successfully");
        response.put("settings", settings);
        return ResponseEntity.ok(response);
    }
}
