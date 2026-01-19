package com.hunesion.drool_v2.service;

import com.hunesion.drool_v2.model.entity.*;
import com.hunesion.drool_v2.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * EquipmentPolicyAssignmentService - Manages policy assignments to users, groups, equipment, and roles
 */
@Service
public class EquipmentPolicyAssignmentService {

    private final EquipmentPolicyRepository policyRepository;
    private final UserRepository userRepository;
    private final UserGroupRepository groupRepository;
    private final EquipmentRepository equipmentRepository;
    private final RoleRepository roleRepository;
    private final DynamicRuleService dynamicRuleService;

    @Autowired
    public EquipmentPolicyAssignmentService(
            EquipmentPolicyRepository policyRepository,
            UserRepository userRepository,
            UserGroupRepository groupRepository,
            EquipmentRepository equipmentRepository,
            RoleRepository roleRepository,
            DynamicRuleService dynamicRuleService) {
        this.policyRepository = policyRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.equipmentRepository = equipmentRepository;
        this.roleRepository = roleRepository;
        this.dynamicRuleService = dynamicRuleService;
    }

    // ========== USER ASSIGNMENTS ==========

    public List<User> getUserAssignments(Long policyId) {
        EquipmentPolicy policy = getPolicyOrThrow(policyId);
        return policy.getUserAssignments().stream()
                .map(PolicyUserAssignment::getUser)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addUserAssignments(Long policyId, Set<Long> userIds) {
        EquipmentPolicy policy = getPolicyOrThrow(policyId);
        
        userIds.forEach(userId -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            
            // Check if assignment already exists
            boolean exists = policy.getUserAssignments().stream()
                    .anyMatch(a -> a.getUser().getId().equals(userId));
            
            if (!exists) {
                PolicyUserAssignment assignment = new PolicyUserAssignment(policy, user);
                policy.getUserAssignments().add(assignment);
            }
        });
        
        policyRepository.save(policy);
        dynamicRuleService.rebuildRules();
    }

    @Transactional
    public void removeUserAssignments(Long policyId, Set<Long> userIds) {
        EquipmentPolicy policy = getPolicyOrThrow(policyId);
        
        policy.getUserAssignments().removeIf(
                assignment -> userIds.contains(assignment.getUser().getId())
        );
        
        policyRepository.save(policy);
        dynamicRuleService.rebuildRules();
    }

    @Transactional
    public void removeUserAssignment(Long policyId, Long userId) {
        removeUserAssignments(policyId, Set.of(userId));
    }

    @Transactional
    public void replaceUserAssignments(Long policyId, Set<Long> userIds) {
        EquipmentPolicy policy = getPolicyOrThrow(policyId);
        
        // Clear existing
        policy.getUserAssignments().clear();
        
        // Add new
        userIds.forEach(userId -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            PolicyUserAssignment assignment = new PolicyUserAssignment(policy, user);
            policy.getUserAssignments().add(assignment);
        });
        
        policyRepository.save(policy);
        dynamicRuleService.rebuildRules();
    }

    // ========== GROUP ASSIGNMENTS ==========

    public List<UserGroup> getGroupAssignments(Long policyId) {
        EquipmentPolicy policy = getPolicyOrThrow(policyId);
        return policy.getGroupAssignments().stream()
                .map(PolicyUserGroupAssignment::getGroup)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addGroupAssignments(Long policyId, Set<Long> groupIds) {
        EquipmentPolicy policy = getPolicyOrThrow(policyId);
        
        groupIds.forEach(groupId -> {
            UserGroup group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));
            
            boolean exists = policy.getGroupAssignments().stream()
                    .anyMatch(a -> a.getGroup().getId().equals(groupId));
            
            if (!exists) {
                PolicyUserGroupAssignment assignment = new PolicyUserGroupAssignment(policy, group);
                policy.getGroupAssignments().add(assignment);
            }
        });
        
        policyRepository.save(policy);
        dynamicRuleService.rebuildRules();
    }

    @Transactional
    public void removeGroupAssignments(Long policyId, Set<Long> groupIds) {
        EquipmentPolicy policy = getPolicyOrThrow(policyId);
        
        policy.getGroupAssignments().removeIf(
                assignment -> groupIds.contains(assignment.getGroup().getId())
        );
        
        policyRepository.save(policy);
        dynamicRuleService.rebuildRules();
    }

    @Transactional
    public void removeGroupAssignment(Long policyId, Long groupId) {
        removeGroupAssignments(policyId, Set.of(groupId));
    }

    @Transactional
    public void replaceGroupAssignments(Long policyId, Set<Long> groupIds) {
        EquipmentPolicy policy = getPolicyOrThrow(policyId);
        
        policy.getGroupAssignments().clear();
        
        groupIds.forEach(groupId -> {
            UserGroup group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));
            PolicyUserGroupAssignment assignment = new PolicyUserGroupAssignment(policy, group);
            policy.getGroupAssignments().add(assignment);
        });
        
        policyRepository.save(policy);
        dynamicRuleService.rebuildRules();
    }

    // ========== EQUIPMENT ASSIGNMENTS ==========

    public List<Equipment> getEquipmentAssignments(Long policyId) {
        EquipmentPolicy policy = getPolicyOrThrow(policyId);
        return policy.getEquipmentAssignments().stream()
                .map(PolicyEquipmentAssignment::getEquipment)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addEquipmentAssignments(Long policyId, Set<Long> equipmentIds) {
        EquipmentPolicy policy = getPolicyOrThrow(policyId);
        
        equipmentIds.forEach(equipmentId -> {
            Equipment equipment = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new RuntimeException("Equipment not found: " + equipmentId));
            
            boolean exists = policy.getEquipmentAssignments().stream()
                    .anyMatch(a -> a.getEquipment().getId().equals(equipmentId));
            
            if (!exists) {
                PolicyEquipmentAssignment assignment = new PolicyEquipmentAssignment(policy, equipment);
                policy.getEquipmentAssignments().add(assignment);
            }
        });
        
        policyRepository.save(policy);
        dynamicRuleService.rebuildRules();
    }

    @Transactional
    public void removeEquipmentAssignments(Long policyId, Set<Long> equipmentIds) {
        EquipmentPolicy policy = getPolicyOrThrow(policyId);
        
        policy.getEquipmentAssignments().removeIf(
                assignment -> equipmentIds.contains(assignment.getEquipment().getId())
        );
        
        policyRepository.save(policy);
        dynamicRuleService.rebuildRules();
    }

    @Transactional
    public void removeEquipmentAssignment(Long policyId, Long equipmentId) {
        removeEquipmentAssignments(policyId, Set.of(equipmentId));
    }

    @Transactional
    public void replaceEquipmentAssignments(Long policyId, Set<Long> equipmentIds) {
        EquipmentPolicy policy = getPolicyOrThrow(policyId);
        
        policy.getEquipmentAssignments().clear();
        
        equipmentIds.forEach(equipmentId -> {
            Equipment equipment = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new RuntimeException("Equipment not found: " + equipmentId));
            PolicyEquipmentAssignment assignment = new PolicyEquipmentAssignment(policy, equipment);
            policy.getEquipmentAssignments().add(assignment);
        });
        
        policyRepository.save(policy);
        dynamicRuleService.rebuildRules();
    }

    // ========== ROLE ASSIGNMENTS ==========

    public List<Role> getRoleAssignments(Long policyId) {
        EquipmentPolicy policy = getPolicyOrThrow(policyId);
        return policy.getRoleAssignments().stream()
                .map(PolicyRoleAssignment::getRole)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addRoleAssignments(Long policyId, Set<Long> roleIds) {
        EquipmentPolicy policy = getPolicyOrThrow(policyId);
        
        roleIds.forEach(roleId -> {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
            
            boolean exists = policy.getRoleAssignments().stream()
                    .anyMatch(a -> a.getRole().getId().equals(roleId));
            
            if (!exists) {
                PolicyRoleAssignment assignment = new PolicyRoleAssignment(policy, role);
                policy.getRoleAssignments().add(assignment);
            }
        });
        
        policyRepository.save(policy);
        dynamicRuleService.rebuildRules();
    }

    @Transactional
    public void removeRoleAssignments(Long policyId, Set<Long> roleIds) {
        EquipmentPolicy policy = getPolicyOrThrow(policyId);
        
        policy.getRoleAssignments().removeIf(
                assignment -> roleIds.contains(assignment.getRole().getId())
        );
        
        policyRepository.save(policy);
        dynamicRuleService.rebuildRules();
    }

    @Transactional
    public void removeRoleAssignment(Long policyId, Long roleId) {
        removeRoleAssignments(policyId, Set.of(roleId));
    }

    @Transactional
    public void replaceRoleAssignments(Long policyId, Set<Long> roleIds) {
        EquipmentPolicy policy = getPolicyOrThrow(policyId);
        
        policy.getRoleAssignments().clear();
        
        roleIds.forEach(roleId -> {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
            PolicyRoleAssignment assignment = new PolicyRoleAssignment(policy, role);
            policy.getRoleAssignments().add(assignment);
        });
        
        policyRepository.save(policy);
        dynamicRuleService.rebuildRules();
    }

    // ========== HELPER METHODS ==========

    private EquipmentPolicy getPolicyOrThrow(Long policyId) {
        return policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found with id: " + policyId));
    }
}
