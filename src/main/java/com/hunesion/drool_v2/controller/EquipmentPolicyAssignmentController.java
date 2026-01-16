package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.model.entity.*;
import com.hunesion.drool_v2.service.EquipmentPolicyAssignmentService;
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
@RequestMapping("/api/equipment-policies/{policyId}/assignments")
@Tag(name = "Policy Assignment Management", 
     description = "Manage policy assignments to users, groups, equipment, and roles")
public class EquipmentPolicyAssignmentController {

    private final EquipmentPolicyAssignmentService assignmentService;

    @Autowired
    public EquipmentPolicyAssignmentController(EquipmentPolicyAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    // ========== USER ASSIGNMENTS ==========

    @Operation(summary = "Get user assignments", 
               description = "Get all users assigned to this policy")
    @GetMapping("/users")
    public ResponseEntity<List<User>> getUserAssignments(@PathVariable Long policyId) {
        return ResponseEntity.ok(assignmentService.getUserAssignments(policyId));
    }

    @Operation(summary = "Add users to policy", 
               description = "Assign policy to multiple users")
    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> addUserAssignments(
            @PathVariable Long policyId,
            @RequestBody Set<Long> userIds) {
        assignmentService.addUserAssignments(policyId, userIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Users assigned successfully");
        response.put("count", userIds.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Replace user assignments", 
               description = "Replace all user assignments with new ones")
    @PutMapping("/users")
    public ResponseEntity<Map<String, Object>> replaceUserAssignments(
            @PathVariable Long policyId,
            @RequestBody Set<Long> userIds) {
        assignmentService.replaceUserAssignments(policyId, userIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User assignments replaced successfully");
        response.put("count", userIds.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove users from policy", 
               description = "Remove multiple users from policy")
    @DeleteMapping("/users")
    public ResponseEntity<Map<String, Object>> removeUserAssignments(
            @PathVariable Long policyId,
            @RequestBody Set<Long> userIds) {
        assignmentService.removeUserAssignments(policyId, userIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Users removed successfully");
        response.put("count", userIds.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove single user from policy", 
               description = "Remove a specific user from policy")
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, String>> removeUserAssignment(
            @PathVariable Long policyId,
            @PathVariable Long userId) {
        assignmentService.removeUserAssignment(policyId, userId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User removed successfully");
        response.put("userId", userId.toString());
        return ResponseEntity.ok(response);
    }

    // ========== GROUP ASSIGNMENTS ==========

    @Operation(summary = "Get group assignments", 
               description = "Get all groups assigned to this policy")
    @GetMapping("/groups")
    public ResponseEntity<List<UserGroup>> getGroupAssignments(@PathVariable Long policyId) {
        return ResponseEntity.ok(assignmentService.getGroupAssignments(policyId));
    }

    @Operation(summary = "Add groups to policy", 
               description = "Assign policy to multiple groups")
    @PostMapping("/groups")
    public ResponseEntity<Map<String, Object>> addGroupAssignments(
            @PathVariable Long policyId,
            @RequestBody Set<Long> groupIds) {
        assignmentService.addGroupAssignments(policyId, groupIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Groups assigned successfully");
        response.put("count", groupIds.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Replace group assignments", 
               description = "Replace all group assignments with new ones")
    @PutMapping("/groups")
    public ResponseEntity<Map<String, Object>> replaceGroupAssignments(
            @PathVariable Long policyId,
            @RequestBody Set<Long> groupIds) {
        assignmentService.replaceGroupAssignments(policyId, groupIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Group assignments replaced successfully");
        response.put("count", groupIds.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove groups from policy", 
               description = "Remove multiple groups from policy")
    @DeleteMapping("/groups")
    public ResponseEntity<Map<String, Object>> removeGroupAssignments(
            @PathVariable Long policyId,
            @RequestBody Set<Long> groupIds) {
        assignmentService.removeGroupAssignments(policyId, groupIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Groups removed successfully");
        response.put("count", groupIds.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove single group from policy", 
               description = "Remove a specific group from policy")
    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<Map<String, String>> removeGroupAssignment(
            @PathVariable Long policyId,
            @PathVariable Long groupId) {
        assignmentService.removeGroupAssignment(policyId, groupId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Group removed successfully");
        response.put("groupId", groupId.toString());
        return ResponseEntity.ok(response);
    }

    // ========== EQUIPMENT ASSIGNMENTS ==========

    @Operation(summary = "Get equipment assignments", 
               description = "Get all equipment assigned to this policy")
    @GetMapping("/equipment")
    public ResponseEntity<List<Equipment>> getEquipmentAssignments(@PathVariable Long policyId) {
        return ResponseEntity.ok(assignmentService.getEquipmentAssignments(policyId));
    }

    @Operation(summary = "Add equipment to policy", 
               description = "Assign policy to multiple equipment")
    @PostMapping("/equipment")
    public ResponseEntity<Map<String, Object>> addEquipmentAssignments(
            @PathVariable Long policyId,
            @RequestBody Set<Long> equipmentIds) {
        assignmentService.addEquipmentAssignments(policyId, equipmentIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Equipment assigned successfully");
        response.put("count", equipmentIds.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Replace equipment assignments", 
               description = "Replace all equipment assignments with new ones")
    @PutMapping("/equipment")
    public ResponseEntity<Map<String, Object>> replaceEquipmentAssignments(
            @PathVariable Long policyId,
            @RequestBody Set<Long> equipmentIds) {
        assignmentService.replaceEquipmentAssignments(policyId, equipmentIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Equipment assignments replaced successfully");
        response.put("count", equipmentIds.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove equipment from policy", 
               description = "Remove multiple equipment from policy")
    @DeleteMapping("/equipment")
    public ResponseEntity<Map<String, Object>> removeEquipmentAssignments(
            @PathVariable Long policyId,
            @RequestBody Set<Long> equipmentIds) {
        assignmentService.removeEquipmentAssignments(policyId, equipmentIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Equipment removed successfully");
        response.put("count", equipmentIds.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove single equipment from policy", 
               description = "Remove a specific equipment from policy")
    @DeleteMapping("/equipment/{equipmentId}")
    public ResponseEntity<Map<String, String>> removeEquipmentAssignment(
            @PathVariable Long policyId,
            @PathVariable Long equipmentId) {
        assignmentService.removeEquipmentAssignment(policyId, equipmentId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Equipment removed successfully");
        response.put("equipmentId", equipmentId.toString());
        return ResponseEntity.ok(response);
    }

    // ========== ROLE ASSIGNMENTS ==========

    @Operation(summary = "Get role assignments", 
               description = "Get all roles assigned to this policy")
    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getRoleAssignments(@PathVariable Long policyId) {
        return ResponseEntity.ok(assignmentService.getRoleAssignments(policyId));
    }

    @Operation(summary = "Add roles to policy", 
               description = "Assign policy to multiple roles")
    @PostMapping("/roles")
    public ResponseEntity<Map<String, Object>> addRoleAssignments(
            @PathVariable Long policyId,
            @RequestBody Set<Long> roleIds) {
        assignmentService.addRoleAssignments(policyId, roleIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Roles assigned successfully");
        response.put("count", roleIds.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Replace role assignments", 
               description = "Replace all role assignments with new ones")
    @PutMapping("/roles")
    public ResponseEntity<Map<String, Object>> replaceRoleAssignments(
            @PathVariable Long policyId,
            @RequestBody Set<Long> roleIds) {
        assignmentService.replaceRoleAssignments(policyId, roleIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Role assignments replaced successfully");
        response.put("count", roleIds.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove roles from policy", 
               description = "Remove multiple roles from policy")
    @DeleteMapping("/roles")
    public ResponseEntity<Map<String, Object>> removeRoleAssignments(
            @PathVariable Long policyId,
            @RequestBody Set<Long> roleIds) {
        assignmentService.removeRoleAssignments(policyId, roleIds);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Roles removed successfully");
        response.put("count", roleIds.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove single role from policy", 
               description = "Remove a specific role from policy")
    @DeleteMapping("/roles/{roleId}")
    public ResponseEntity<Map<String, String>> removeRoleAssignment(
            @PathVariable Long policyId,
            @PathVariable Long roleId) {
        assignmentService.removeRoleAssignment(policyId, roleId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Role removed successfully");
        response.put("roleId", roleId.toString());
        return ResponseEntity.ok(response);
    }
}
