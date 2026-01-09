package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.dto.PolicyDTO;
import com.hunesion.drool_v2.entity.AccessPolicy;
import com.hunesion.drool_v2.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Create a new policy")
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
