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
