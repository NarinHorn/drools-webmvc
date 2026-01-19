package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.dto.PolicyGroupDTO;
import com.hunesion.drool_v2.model.entity.*;
import com.hunesion.drool_v2.service.PolicyGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/policy-groups")
@Tag(name = "Policy Groups", description = "Manage policy groups (bundles of policies)")
public class PolicyGroupController {

    private final PolicyGroupService policyGroupService;

    @Autowired
    public PolicyGroupController(PolicyGroupService policyGroupService) {
        this.policyGroupService = policyGroupService;
    }

    // ========== CRUD ==========

    @Operation(summary = "Get all policy groups")
    @GetMapping
    public ResponseEntity<List<PolicyGroup>> getAllPolicyGroups() {
        return ResponseEntity.ok(policyGroupService.getAllPolicyGroups());
    }

    @Operation(summary = "Get policy group by ID")
    @GetMapping("/{id}")
    public ResponseEntity<PolicyGroup> getPolicyGroupById(@PathVariable Long id) {
        return ResponseEntity.ok(policyGroupService.getPolicyGroupById(id));
    }

    @Operation(summary = "Create a new policy group")
    @PostMapping
    public ResponseEntity<PolicyGroup> createPolicyGroup(@RequestBody PolicyGroupDTO dto) {
        return ResponseEntity.ok(policyGroupService.createPolicyGroup(dto));
    }

    @Operation(summary = "Update a policy group")
    @PutMapping("/{id}")
    public ResponseEntity<PolicyGroup> updatePolicyGroup(
            @PathVariable Long id,
            @RequestBody PolicyGroupDTO dto) {
        return ResponseEntity.ok(policyGroupService.updatePolicyGroup(id, dto));
    }

    @Operation(summary = "Delete a policy group")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deletePolicyGroup(@PathVariable Long id) {
        policyGroupService.deletePolicyGroup(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "PolicyGroup deleted successfully");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Enable or disable a policy group")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<PolicyGroup> togglePolicyGroup(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(policyGroupService.togglePolicyGroup(id, enabled));
    }

    // ========== Policy Members ==========

    @Operation(summary = "Get policies in this group")
    @GetMapping("/{id}/policies")
    public ResponseEntity<List<EquipmentPolicy>> getPoliciesInGroup(@PathVariable Long id) {
        return ResponseEntity.ok(policyGroupService.getPoliciesInGroup(id));
    }

    @Operation(summary = "Add policies to this group")
    @PostMapping("/{id}/policies")
    public ResponseEntity<Map<String, Object>> addPoliciesToGroup(
            @PathVariable Long id,
            @RequestBody Set<Long> policyIds) {
        policyGroupService.addPoliciesToGroup(id, policyIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Policies added to group successfully");
        response.put("count", policyIds.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Replace all policies in this group")
    @PutMapping("/{id}/policies")
    public ResponseEntity<Map<String, Object>> replacePoliciesInGroup(
            @PathVariable Long id,
            @RequestBody Set<Long> policyIds) {
        policyGroupService.replacePoliciesInGroup(id, policyIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Policies replaced successfully");
        response.put("count", policyIds.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove policies from this group")
    @DeleteMapping("/{id}/policies")
    public ResponseEntity<Map<String, Object>> removePoliciesFromGroup(
            @PathVariable Long id,
            @RequestBody Set<Long> policyIds) {
        policyGroupService.removePoliciesFromGroup(id, policyIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Policies removed from group successfully");
        response.put("count", policyIds.size());
        return ResponseEntity.ok(response);
    }

    // ========== User Assignments ==========

    @Operation(summary = "Get users assigned to this policy group")
    @GetMapping("/{id}/users")
    public ResponseEntity<List<User>> getUserAssignments(@PathVariable Long id) {
        return ResponseEntity.ok(policyGroupService.getUserAssignments(id));
    }

    @Operation(summary = "Assign this policy group to users")
    @PostMapping("/{id}/users")
    public ResponseEntity<Map<String, Object>> addUserAssignments(
            @PathVariable Long id,
            @RequestBody Set<Long> userIds) {
        policyGroupService.addUserAssignments(id, userIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Users assigned successfully");
        response.put("count", userIds.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove users from this policy group")
    @DeleteMapping("/{id}/users")
    public ResponseEntity<Map<String, Object>> removeUserAssignments(
            @PathVariable Long id,
            @RequestBody Set<Long> userIds) {
        policyGroupService.removeUserAssignments(id, userIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Users removed successfully");
        response.put("count", userIds.size());
        return ResponseEntity.ok(response);
    }

    // ========== UserGroup Assignments ==========

    @Operation(summary = "Get user groups assigned to this policy group")
    @GetMapping("/{id}/user-groups")
    public ResponseEntity<List<UserGroup>> getUserGroupAssignments(@PathVariable Long id) {
        return ResponseEntity.ok(policyGroupService.getUserGroupAssignments(id));
    }

    @Operation(summary = "Assign this policy group to user groups")
    @PostMapping("/{id}/user-groups")
    public ResponseEntity<Map<String, Object>> addUserGroupAssignments(
            @PathVariable Long id,
            @RequestBody Set<Long> userGroupIds) {
        policyGroupService.addUserGroupAssignments(id, userGroupIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User groups assigned successfully");
        response.put("count", userGroupIds.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove user groups from this policy group")
    @DeleteMapping("/{id}/user-groups")
    public ResponseEntity<Map<String, Object>> removeUserGroupAssignments(
            @PathVariable Long id,
            @RequestBody Set<Long> userGroupIds) {
        policyGroupService.removeUserGroupAssignments(id, userGroupIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User groups removed successfully");
        response.put("count", userGroupIds.size());
        return ResponseEntity.ok(response);
    }

    // ========== Role Assignments ==========

    @Operation(summary = "Get roles assigned to this policy group")
    @GetMapping("/{id}/roles")
    public ResponseEntity<List<Role>> getRoleAssignments(@PathVariable Long id) {
        return ResponseEntity.ok(policyGroupService.getRoleAssignments(id));
    }

    @Operation(summary = "Assign this policy group to roles")
    @PostMapping("/{id}/roles")
    public ResponseEntity<Map<String, Object>> addRoleAssignments(
            @PathVariable Long id,
            @RequestBody Set<Long> roleIds) {
        policyGroupService.addRoleAssignments(id, roleIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Roles assigned successfully");
        response.put("count", roleIds.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove roles from this policy group")
    @DeleteMapping("/{id}/roles")
    public ResponseEntity<Map<String, Object>> removeRoleAssignments(
            @PathVariable Long id,
            @RequestBody Set<Long> roleIds) {
        policyGroupService.removeRoleAssignments(id, roleIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Roles removed successfully");
        response.put("count", roleIds.size());
        return ResponseEntity.ok(response);
    }
}
