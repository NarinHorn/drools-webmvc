package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.dto.EquipmentPolicyDTO;
import com.hunesion.drool_v2.model.entity.EquipmentPolicy;
import com.hunesion.drool_v2.service.EquipmentPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/equipment-policies")
@Tag(name = "Equipment Policy Management", description = "API for managing equipment access policies")
public class EquipmentPolicyController {

    private final EquipmentPolicyService policyService;

    @Autowired
    public EquipmentPolicyController(EquipmentPolicyService policyService) {
        this.policyService = policyService;
    }

    @Operation(summary = "Get all policies", description = "Retrieves all equipment policies")
    @GetMapping
    public ResponseEntity<List<EquipmentPolicy>> getAllPolicies() {
        return ResponseEntity.ok(policyService.getAllPolicies());
    }

    @Operation(summary = "Get enabled policies", description = "Retrieves only enabled equipment policies")
    @GetMapping("/enabled")
    public ResponseEntity<List<EquipmentPolicy>> getEnabledPolicies() {
        return ResponseEntity.ok(policyService.getEnabledPolicies());
    }

    @Operation(summary = "Get policy by ID", description = "Retrieves a specific policy by its ID")
    @GetMapping("/{id}")
    public ResponseEntity<EquipmentPolicy> getPolicyById(@PathVariable Long id) {
        return ResponseEntity.ok(policyService.getPolicyById(id));
    }

    @Operation(summary = "Create new policy", description = "Creates a new equipment policy")
    @PostMapping
    public ResponseEntity<EquipmentPolicy> createPolicy(@RequestBody EquipmentPolicyDTO dto) {
        EquipmentPolicy created = policyService.createPolicy(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update policy", description = "Updates an existing equipment policy")
    @PutMapping("/{id}")
    public ResponseEntity<EquipmentPolicy> updatePolicy(
            @PathVariable Long id,
            @RequestBody EquipmentPolicyDTO dto) {
        EquipmentPolicy updated = policyService.updatePolicy(id, dto);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete policy", description = "Deletes an equipment policy")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deletePolicy(@PathVariable Long id) {
        policyService.deletePolicy(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Policy deleted successfully");
        response.put("id", id.toString());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Toggle policy", description = "Enable or disable a policy")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<EquipmentPolicy> togglePolicy(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        EquipmentPolicy updated = policyService.togglePolicy(id, enabled);
        return ResponseEntity.ok(updated);
    }
}