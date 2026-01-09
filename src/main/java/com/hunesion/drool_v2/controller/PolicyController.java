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

    @Operation(
        summary = "Get all policies",
        description = "Retrieves a list of all access policies in the system, including both enabled and disabled policies."
    )
    @GetMapping
    public ResponseEntity<List<AccessPolicy>> getAllPolicies() {
        return ResponseEntity.ok(policyService.getAllPolicies());
    }

    @Operation(
        summary = "Get enabled policies",
        description = "Retrieves only the policies that are currently enabled and active. Policies are ordered by priority (highest first)."
    )
    @GetMapping("/enabled")
    public ResponseEntity<List<AccessPolicy>> getEnabledPolicies() {
        return ResponseEntity.ok(policyService.getEnabledPolicies());
    }

    @Operation(
        summary = "Get policy by ID",
        description = "Retrieves a specific access policy by its unique identifier. Returns 404 if the policy does not exist."
    )
    @GetMapping("/{id}")
    public ResponseEntity<AccessPolicy> getPolicyById(@PathVariable Long id) {
        return ResponseEntity.ok(policyService.getPolicyById(id));
    }

    @Operation(
        summary = "Create a new policy",
        description = "Creates a new access policy with the specified rules and conditions. The policy will be automatically converted to DRL format and enabled. Requires a unique policy name."
    )
    @PostMapping
    public ResponseEntity<AccessPolicy> createPolicy(@RequestBody PolicyDTO dto) {
        AccessPolicy created = policyService.createPolicy(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
        summary = "Update an existing policy",
        description = "Updates an existing access policy with new rules, conditions, or settings. The DRL will be regenerated automatically. The policy ID must exist."
    )
    @PutMapping("/{id}")
    public ResponseEntity<AccessPolicy> updatePolicy(@PathVariable Long id, @RequestBody PolicyDTO dto) {
        AccessPolicy updated = policyService.updatePolicy(id, dto);
        return ResponseEntity.ok(updated);
    }

    @Operation(
        summary = "Delete a policy",
        description = "Permanently deletes an access policy from the system. This action cannot be undone. The rules will be automatically rebuilt after deletion."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deletePolicy(@PathVariable Long id) {
        policyService.deletePolicy(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Policy deleted successfully");
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Enable or disable a policy",
        description = "Toggles the enabled/disabled state of a policy. Disabled policies are not evaluated during access control checks. Useful for temporarily disabling policies without deleting them."
    )
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<AccessPolicy> togglePolicy(@PathVariable Long id, 
                                                      @RequestParam boolean enabled) {
        AccessPolicy updated = policyService.togglePolicy(id, enabled);
        return ResponseEntity.ok(updated);
    }

    @Operation(
        summary = "Preview generated DRL",
        description = "Generates and returns the DRL (Drools Rule Language) code for a policy without saving it. Useful for validating policy structure before creating or updating a policy."
    )
    @PostMapping("/preview-drl")
    public ResponseEntity<Map<String, String>> previewDrl(@RequestBody PolicyDTO dto) {
        String drl = policyService.previewDrl(dto);
        Map<String, String> response = new HashMap<>();
        response.put("drl", drl);
        return ResponseEntity.ok(response);
    }
}
