package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.model.entity.PolicyType;
import com.hunesion.drool_v2.repository.PolicyTypeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policy-types")
@Tag(name = "Policy Type Management", description = "Manage policy types (commonSettings, allowedTime, loginControl, commandSettings)")
public class PolicyTypeController {
    
    private final PolicyTypeRepository policyTypeRepository;
    
    @Autowired
    public PolicyTypeController(PolicyTypeRepository policyTypeRepository) {
        this.policyTypeRepository = policyTypeRepository;
    }
    
    @Operation(
        summary = "Get all policy types", 
        description = "Retrieves all available policy types"
    )
    @GetMapping
    public ResponseEntity<List<PolicyType>> getAllPolicyTypes() {
        return ResponseEntity.ok(policyTypeRepository.findAll());
    }
    
    @Operation(
        summary = "Get active policy types",
        description = "Retrieves only active policy types"
    )
    @GetMapping("/active")
    public ResponseEntity<List<PolicyType>> getActivePolicyTypes() {
        return ResponseEntity.ok(policyTypeRepository.findByActiveTrue());
    }
    
    @Operation(
        summary = "Get policy type by ID",
        description = "Retrieves a specific policy type by its ID"
    )
    @GetMapping("/{id}")
    public ResponseEntity<PolicyType> getPolicyTypeById(@PathVariable Long id) {
        return policyTypeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @Operation(
        summary = "Get policy type by code",
        description = "Retrieves a policy type by its type code (e.g., 'commonSettings', 'allowedTime')"
    )
    @GetMapping("/code/{typeCode}")
    public ResponseEntity<PolicyType> getPolicyTypeByCode(@PathVariable String typeCode) {
        return policyTypeRepository.findByTypeCode(typeCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
