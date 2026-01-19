package com.hunesion.drool_v2.service;

import com.hunesion.drool_v2.dto.PolicyGroupDTO;
import com.hunesion.drool_v2.model.entity.*;
import com.hunesion.drool_v2.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PolicyGroupService {

    private final PolicyGroupRepository policyGroupRepository;
    private final EquipmentPolicyRepository policyRepository;
    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final RoleRepository roleRepository;
    private final DynamicRuleService dynamicRuleService;

    @Autowired
    public PolicyGroupService(
            PolicyGroupRepository policyGroupRepository,
            EquipmentPolicyRepository policyRepository,
            UserRepository userRepository,
            UserGroupRepository userGroupRepository,
            RoleRepository roleRepository,
            DynamicRuleService dynamicRuleService) {
        this.policyGroupRepository = policyGroupRepository;
        this.policyRepository = policyRepository;
        this.userRepository = userRepository;
        this.userGroupRepository = userGroupRepository;
        this.roleRepository = roleRepository;
        this.dynamicRuleService = dynamicRuleService;
    }

    // ========== CRUD Operations ==========

    public List<PolicyGroup> getAllPolicyGroups() {
        return policyGroupRepository.findAll();
    }

    public List<PolicyGroup> getEnabledPolicyGroups() {
        return policyGroupRepository.findByEnabledTrue();
    }

    public PolicyGroup getPolicyGroupById(Long id) {
        return policyGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PolicyGroup not found with id: " + id));
    }

    @Transactional
    public PolicyGroup createPolicyGroup(PolicyGroupDTO dto) {
        if (policyGroupRepository.existsByGroupName(dto.getGroupName())) {
            throw new RuntimeException("PolicyGroup already exists: " + dto.getGroupName());
        }

        PolicyGroup policyGroup = new PolicyGroup();
        policyGroup.setGroupName(dto.getGroupName());
        policyGroup.setDescription(dto.getDescription());
        policyGroup.setEnabled(dto.isEnabled());

        PolicyGroup saved = policyGroupRepository.save(policyGroup);

        // Add policies if provided
        if (dto.getPolicyIds() != null && !dto.getPolicyIds().isEmpty()) {
            addPoliciesToGroup(saved.getId(), dto.getPolicyIds());
        }

        return policyGroupRepository.findById(saved.getId()).orElse(saved);
    }

    @Transactional
    public PolicyGroup updatePolicyGroup(Long id, PolicyGroupDTO dto) {
        PolicyGroup existing = getPolicyGroupById(id);

        existing.setGroupName(dto.getGroupName());
        existing.setDescription(dto.getDescription());
        existing.setEnabled(dto.isEnabled());

        return policyGroupRepository.save(existing);
    }

    @Transactional
    public void deletePolicyGroup(Long id) {
        policyGroupRepository.deleteById(id);
        dynamicRuleService.rebuildRules();
    }

    @Transactional
    public PolicyGroup togglePolicyGroup(Long id, boolean enabled) {
        PolicyGroup policyGroup = getPolicyGroupById(id);
        policyGroup.setEnabled(enabled);
        return policyGroupRepository.save(policyGroup);
    }

    // ========== Policy Members Management ==========

    public List<EquipmentPolicy> getPoliciesInGroup(Long policyGroupId) {
        PolicyGroup policyGroup = getPolicyGroupById(policyGroupId);
        return policyGroup.getPolicyMembers().stream()
                .map(PolicyGroupMember::getPolicy)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addPoliciesToGroup(Long policyGroupId, Set<Long> policyIds) {
        PolicyGroup policyGroup = getPolicyGroupById(policyGroupId);

        policyIds.forEach(policyId -> {
            EquipmentPolicy policy = policyRepository.findById(policyId)
                    .orElseThrow(() -> new RuntimeException("Policy not found: " + policyId));

            boolean exists = policyGroup.getPolicyMembers().stream()
                    .anyMatch(m -> m.getPolicy().getId().equals(policyId));

            if (!exists) {
                PolicyGroupMember member = new PolicyGroupMember(policyGroup, policy);
                policyGroup.getPolicyMembers().add(member);
            }
        });

        policyGroupRepository.save(policyGroup);
        dynamicRuleService.rebuildRules();
    }

    @Transactional
    public void removePoliciesFromGroup(Long policyGroupId, Set<Long> policyIds) {
        PolicyGroup policyGroup = getPolicyGroupById(policyGroupId);

        policyGroup.getPolicyMembers().removeIf(
                member -> policyIds.contains(member.getPolicy().getId())
        );

        policyGroupRepository.save(policyGroup);
        dynamicRuleService.rebuildRules();
    }

    @Transactional
    public void replacePoliciesInGroup(Long policyGroupId, Set<Long> policyIds) {
        PolicyGroup policyGroup = getPolicyGroupById(policyGroupId);

        policyGroup.getPolicyMembers().clear();

        policyIds.forEach(policyId -> {
            EquipmentPolicy policy = policyRepository.findById(policyId)
                    .orElseThrow(() -> new RuntimeException("Policy not found: " + policyId));
            PolicyGroupMember member = new PolicyGroupMember(policyGroup, policy);
            policyGroup.getPolicyMembers().add(member);
        });

        policyGroupRepository.save(policyGroup);
        dynamicRuleService.rebuildRules();
    }

    // ========== User Assignments ==========

    public List<User> getUserAssignments(Long policyGroupId) {
        PolicyGroup policyGroup = getPolicyGroupById(policyGroupId);
        return policyGroup.getUserAssignments().stream()
                .map(PolicyGroupUserAssignment::getUser)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addUserAssignments(Long policyGroupId, Set<Long> userIds) {
        PolicyGroup policyGroup = getPolicyGroupById(policyGroupId);

        userIds.forEach(userId -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            boolean exists = policyGroup.getUserAssignments().stream()
                    .anyMatch(a -> a.getUser().getId().equals(userId));

            if (!exists) {
                PolicyGroupUserAssignment assignment = new PolicyGroupUserAssignment(policyGroup, user);
                policyGroup.getUserAssignments().add(assignment);
            }
        });

        policyGroupRepository.save(policyGroup);
        dynamicRuleService.rebuildRules();
    }

    @Transactional
    public void removeUserAssignments(Long policyGroupId, Set<Long> userIds) {
        PolicyGroup policyGroup = getPolicyGroupById(policyGroupId);

        policyGroup.getUserAssignments().removeIf(
                assignment -> userIds.contains(assignment.getUser().getId())
        );

        policyGroupRepository.save(policyGroup);
        dynamicRuleService.rebuildRules();
    }

    // ========== UserGroup Assignments ==========

    public List<UserGroup> getUserGroupAssignments(Long policyGroupId) {
        PolicyGroup policyGroup = getPolicyGroupById(policyGroupId);
        return policyGroup.getUserGroupAssignments().stream()
                .map(PolicyGroupUserGroupAssignment::getUserGroup)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addUserGroupAssignments(Long policyGroupId, Set<Long> userGroupIds) {
        PolicyGroup policyGroup = getPolicyGroupById(policyGroupId);

        userGroupIds.forEach(userGroupId -> {
            UserGroup userGroup = userGroupRepository.findById(userGroupId)
                    .orElseThrow(() -> new RuntimeException("UserGroup not found: " + userGroupId));

            boolean exists = policyGroup.getUserGroupAssignments().stream()
                    .anyMatch(a -> a.getUserGroup().getId().equals(userGroupId));

            if (!exists) {
                PolicyGroupUserGroupAssignment assignment = new PolicyGroupUserGroupAssignment(policyGroup, userGroup);
                policyGroup.getUserGroupAssignments().add(assignment);
            }
        });

        policyGroupRepository.save(policyGroup);
        dynamicRuleService.rebuildRules();
    }

    @Transactional
    public void removeUserGroupAssignments(Long policyGroupId, Set<Long> userGroupIds) {
        PolicyGroup policyGroup = getPolicyGroupById(policyGroupId);

        policyGroup.getUserGroupAssignments().removeIf(
                assignment -> userGroupIds.contains(assignment.getUserGroup().getId())
        );

        policyGroupRepository.save(policyGroup);
        dynamicRuleService.rebuildRules();
    }

    // ========== Role Assignments ==========

    public List<Role> getRoleAssignments(Long policyGroupId) {
        PolicyGroup policyGroup = getPolicyGroupById(policyGroupId);
        return policyGroup.getRoleAssignments().stream()
                .map(PolicyGroupRoleAssignment::getRole)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addRoleAssignments(Long policyGroupId, Set<Long> roleIds) {
        PolicyGroup policyGroup = getPolicyGroupById(policyGroupId);

        roleIds.forEach(roleId -> {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));

            boolean exists = policyGroup.getRoleAssignments().stream()
                    .anyMatch(a -> a.getRole().getId().equals(roleId));

            if (!exists) {
                PolicyGroupRoleAssignment assignment = new PolicyGroupRoleAssignment(policyGroup, role);
                policyGroup.getRoleAssignments().add(assignment);
            }
        });

        policyGroupRepository.save(policyGroup);
        dynamicRuleService.rebuildRules();
    }

    @Transactional
    public void removeRoleAssignments(Long policyGroupId, Set<Long> roleIds) {
        PolicyGroup policyGroup = getPolicyGroupById(policyGroupId);

        policyGroup.getRoleAssignments().removeIf(
                assignment -> roleIds.contains(assignment.getRole().getId())
        );

        policyGroupRepository.save(policyGroup);
        dynamicRuleService.rebuildRules();
    }
}
