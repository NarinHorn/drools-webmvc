package com.hunesion.drool_v2.service;

import com.hunesion.drool_v2.dto.*;
import com.hunesion.drool_v2.model.entity.*;
import com.hunesion.drool_v2.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * EquipmentPolicyService - CRUD operations for equipment policies
 */
@Service
public class EquipmentPolicyService {

    private final EquipmentPolicyRepository policyRepository;
    private final UserRepository userRepository;
    private final UserGroupRepository groupRepository;
    private final EquipmentRepository equipmentRepository;
    private final RoleRepository roleRepository;
    private final CommandListRepository commandListRepository;
    private final EquipmentPolicyRuleGenerator ruleGenerator;
    private final DynamicRuleService dynamicRuleService;

    @Autowired
    public EquipmentPolicyService(
            EquipmentPolicyRepository policyRepository,
            UserRepository userRepository,
            UserGroupRepository groupRepository,
            EquipmentRepository equipmentRepository,
            RoleRepository roleRepository,
            CommandListRepository commandListRepository,
            EquipmentPolicyRuleGenerator ruleGenerator,
            DynamicRuleService dynamicRuleService) {
        this.policyRepository = policyRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.equipmentRepository = equipmentRepository;
        this.roleRepository = roleRepository;
        this.commandListRepository = commandListRepository;
        this.ruleGenerator = ruleGenerator;
        this.dynamicRuleService = dynamicRuleService;
    }

    public List<EquipmentPolicy> getAllPolicies() {
        return policyRepository.findAll();
    }

    public List<EquipmentPolicy> getEnabledPolicies() {
        return policyRepository.findByEnabledTrueOrderByPriorityDesc();
    }

    public EquipmentPolicy getPolicyById(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found with id: " + id));
    }

    @Transactional
    public EquipmentPolicy createPolicy(EquipmentPolicyDTO dto) {
        if (policyRepository.existsByPolicyName(dto.getPolicyName())) {
            throw new RuntimeException("Policy already exists: " + dto.getPolicyName());
        }

        EquipmentPolicy policy = convertDtoToEntity(dto);
        EquipmentPolicy saved = policyRepository.save(policy);

        // Create assignments
        createAssignments(saved, dto);

        // Rebuild Drools rules
        dynamicRuleService.rebuildRules();

        return saved;
    }

    @Transactional
    public EquipmentPolicy updatePolicy(Long id, EquipmentPolicyDTO dto) {
        EquipmentPolicy existing = getPolicyById(id);

        existing.setPolicyName(dto.getPolicyName());
        existing.setDescription(dto.getDescription());

        // Only update if provided, otherwise keep existing value
        if (dto.getPolicyClassification() != null) {
            existing.setPolicyClassification(dto.getPolicyClassification());
        }
        if (dto.getPolicyApplication() != null) {
            existing.setPolicyApplication(dto.getPolicyApplication());
        }

        existing.setEnabled(dto.isEnabled());
        if (dto.getPriority() != null) {
            existing.setPriority(dto.getPriority());
        }

        // Update settings
        updateCommonSettings(existing, dto.getCommonSettings());
        updateAllowedTime(existing, dto.getAllowedTime());
        updateCommandSettings(existing, dto.getCommandSettings());
        updateLoginControl(existing, dto.getLoginControl());

        EquipmentPolicy saved = policyRepository.save(existing);

        // Update assignments
        updateAssignments(saved, dto);

        // Rebuild Drools rules
        dynamicRuleService.rebuildRules();

        return saved;
    }

    @Transactional
    public void deletePolicy(Long id) {
        policyRepository.deleteById(id);
        dynamicRuleService.rebuildRules();
    }

    @Transactional
    public EquipmentPolicy togglePolicy(Long id, boolean enabled) {
        EquipmentPolicy policy = getPolicyById(id);
        policy.setEnabled(enabled);
        EquipmentPolicy saved = policyRepository.save(policy);
        dynamicRuleService.rebuildRules();
        return saved;
    }

    private EquipmentPolicy convertDtoToEntity(EquipmentPolicyDTO dto) {
        EquipmentPolicy policy = new EquipmentPolicy();
        policy.setPolicyName(dto.getPolicyName());
        policy.setDescription(dto.getDescription());

        // Provide default values for required fields
        policy.setPolicyClassification(
                dto.getPolicyClassification() != null
                        ? dto.getPolicyClassification()
                        : "common"  // Default classification
        );
        policy.setPolicyApplication(
                dto.getPolicyApplication() != null
                        ? dto.getPolicyApplication()
                        : "apply"  // Default application
        );

        policy.setEnabled(dto.isEnabled());
        policy.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);

        if (dto.getEquipmentBasicPolicyId() != null) {
            EquipmentPolicy basicPolicy = policyRepository.findById(dto.getEquipmentBasicPolicyId())
                    .orElseThrow(() -> new RuntimeException("Basic policy not found"));
            policy.setEquipmentBasicPolicy(basicPolicy);
        }

        // Create settings
        if (dto.getCommonSettings() != null) {
            policy.setCommonSettings(convertCommonSettings(dto.getCommonSettings(), policy));
        }
        if (dto.getAllowedTime() != null) {
            policy.setAllowedTime(convertAllowedTime(dto.getAllowedTime(), policy));
        }
        if (dto.getLoginControl() != null) {
            policy.setLoginControl(convertLoginControl(dto.getLoginControl(), policy));
        }

        return policy;
    }

    private PolicyCommonSettings convertCommonSettings(PolicyCommonSettingsDTO dto, EquipmentPolicy policy) {
        PolicyCommonSettings settings = new PolicyCommonSettings();
        settings.setPolicy(policy);
        settings.setServicePort(dto.getServicePort());
        settings.setIdleTimeMinutes(dto.getIdleTimeMinutes());
        settings.setTimeoutMinutes(dto.getTimeoutMinutes());
        settings.setBlockingPolicyType(dto.getBlockingPolicyType());
        settings.setSessionBlockingCount(dto.getSessionBlockingCount());
        settings.setMaxTelnetSessions(dto.getMaxTelnetSessions());
        settings.setTelnetBorderless(dto.isTelnetBorderless());
        settings.setMaxSshSessions(dto.getMaxSshSessions());
        settings.setSshBorderless(dto.isSshBorderless());
        settings.setMaxRdpSessions(dto.getMaxRdpSessions());
        settings.setRdpBorderless(dto.isRdpBorderless());

        // Add protocols
        if (dto.getAllowedProtocols() != null) {
            Set<PolicyAllowedProtocol> protocols = dto.getAllowedProtocols().stream()
                    .map(p -> new PolicyAllowedProtocol(settings, p))
                    .collect(Collectors.toSet());
            settings.setAllowedProtocols(protocols);
        }

        // Add DBMS
        if (dto.getAllowedDbms() != null) {
            Set<PolicyAllowedDbms> dbms = dto.getAllowedDbms().stream()
                    .map(d -> new PolicyAllowedDbms(settings, d))
                    .collect(Collectors.toSet());
            settings.setAllowedDbms(dbms);
        }

        return settings;
    }

    private PolicyAllowedTime convertAllowedTime(PolicyAllowedTimeDTO dto, EquipmentPolicy policy) {
        PolicyAllowedTime allowedTime = new PolicyAllowedTime();
        allowedTime.setPolicy(policy);
        allowedTime.setStartDate(dto.getStartDate());
        allowedTime.setEndDate(dto.getEndDate());
        allowedTime.setBorderless(dto.isBorderless());
        allowedTime.setTimeZone(dto.getTimeZone());

        if (dto.getTimeSlots() != null) {
            Set<PolicyTimeSlot> timeSlots = dto.getTimeSlots().stream()
                    .map(ts -> new PolicyTimeSlot(allowedTime, ts.getDayOfWeek(), ts.getHourStart(), ts.getHourEnd()))
                    .collect(Collectors.toSet());
            allowedTime.setTimeSlots(timeSlots);
        }

        return allowedTime;
    }

    private PolicyLoginControl convertLoginControl(PolicyLoginControlDTO dto, EquipmentPolicy policy) {
        PolicyLoginControl loginControl = new PolicyLoginControl();
        loginControl.setPolicy(policy);
        loginControl.setIpFilteringType(dto.getIpFilteringType());
        loginControl.setAccountLockEnabled(dto.isAccountLockEnabled());
        loginControl.setMaxFailureAttempts(dto.getMaxFailureAttempts());
        loginControl.setLockoutDurationMinutes(dto.getLockoutDurationMinutes());
        loginControl.setTwoFactorType(dto.getTwoFactorType());

        if (dto.getAllowedIps() != null) {
            Set<PolicyAllowedIp> allowedIps = dto.getAllowedIps().stream()
                    .map(ip -> {
                        PolicyAllowedIp allowedIp = new PolicyAllowedIp();
                        allowedIp.setPolicy(loginControl);
                        allowedIp.setIpAddress(ip);
                        return allowedIp;
                    })
                    .collect(Collectors.toSet());
            loginControl.setAllowedIps(allowedIps);
        }

        return loginControl;
    }

    private void createAssignments(EquipmentPolicy policy, EquipmentPolicyDTO dto) {
        // User assignments
        if (dto.getUserIds() != null) {
            dto.getUserIds().forEach(userId -> {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found: " + userId));
                PolicyUserAssignment assignment = new PolicyUserAssignment(policy, user);
                policy.getUserAssignments().add(assignment);
            });
        }

        // Group assignments
        if (dto.getGroupIds() != null) {
            dto.getGroupIds().forEach(groupId -> {
                UserGroup group = groupRepository.findById(groupId)
                        .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));
                PolicyGroupAssignment assignment = new PolicyGroupAssignment(policy, group);
                policy.getGroupAssignments().add(assignment);
            });
        }

        // Equipment assignments
        if (dto.getEquipmentIds() != null) {
            dto.getEquipmentIds().forEach(equipmentId -> {
                Equipment equipment = equipmentRepository.findById(equipmentId)
                        .orElseThrow(() -> new RuntimeException("Equipment not found: " + equipmentId));
                PolicyEquipmentAssignment assignment = new PolicyEquipmentAssignment(policy, equipment);
                policy.getEquipmentAssignments().add(assignment);
            });
        }

        // Role assignments
        if (dto.getRoleIds() != null) {
            dto.getRoleIds().forEach(roleId -> {
                Role role = roleRepository.findById(roleId)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
                PolicyRoleAssignment assignment = new PolicyRoleAssignment(policy, role);
                policy.getRoleAssignments().add(assignment);
            });
        }
    }

    private void updateAssignments(EquipmentPolicy policy, EquipmentPolicyDTO dto) {
        // Clear existing assignments
        policy.getUserAssignments().clear();
        policy.getGroupAssignments().clear();
        policy.getEquipmentAssignments().clear();
        policy.getRoleAssignments().clear();

        // Create new assignments
        createAssignments(policy, dto);
    }

    private void updateCommonSettings(EquipmentPolicy policy, PolicyCommonSettingsDTO dto) {
        if (dto == null) {
            if (policy.getCommonSettings() != null) {
                policy.setCommonSettings(null);
            }
            return;
        }

        PolicyCommonSettings settings = policy.getCommonSettings();
        if (settings == null) {
            settings = convertCommonSettings(dto, policy);
            policy.setCommonSettings(settings);
        } else {
            // Update existing
            settings.setServicePort(dto.getServicePort());
            settings.setIdleTimeMinutes(dto.getIdleTimeMinutes());
            settings.setTimeoutMinutes(dto.getTimeoutMinutes());
            // ... update other fields
        }
    }

    private void updateAllowedTime(EquipmentPolicy policy, PolicyAllowedTimeDTO dto) {
        if (dto == null) {
            if (policy.getAllowedTime() != null) {
                policy.setAllowedTime(null);
            }
            return;
        }

        PolicyAllowedTime allowedTime = policy.getAllowedTime();
        if (allowedTime == null) {
            allowedTime = convertAllowedTime(dto, policy);
            policy.setAllowedTime(allowedTime);
        } else {
            // Update existing
            allowedTime.setStartDate(dto.getStartDate());
            allowedTime.setEndDate(dto.getEndDate());
            allowedTime.setBorderless(dto.isBorderless());
            allowedTime.setTimeZone(dto.getTimeZone());
        }
    }

    private void updateCommandSettings(EquipmentPolicy policy, List<PolicyCommandSettingsDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            policy.getCommandSettings().clear();
            return;
        }

        // Clear and recreate
        policy.getCommandSettings().clear();
        for (PolicyCommandSettingsDTO dto : dtoList) {
            PolicyCommandSettings settings = new PolicyCommandSettings();
            settings.setPolicy(policy);
            settings.setProtocolType(dto.getProtocolType());
            settings.setControlMethod(dto.getControlMethod());
            settings.setControlTarget(dto.getControlTarget());

            if (dto.getCommandListIds() != null) {
                Set<CommandList> commandLists = dto.getCommandListIds().stream()
                        .map(id -> commandListRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Command list not found: " + id)))
                        .collect(Collectors.toSet());
                settings.setCommandLists(commandLists);
            }

            policy.getCommandSettings().add(settings);
        }
    }

    private void updateLoginControl(EquipmentPolicy policy, PolicyLoginControlDTO dto) {
        if (dto == null) {
            if (policy.getLoginControl() != null) {
                policy.setLoginControl(null);
            }
            return;
        }

        PolicyLoginControl loginControl = policy.getLoginControl();
        if (loginControl == null) {
            loginControl = convertLoginControl(dto, policy);
            policy.setLoginControl(loginControl);
        } else {
            // Update existing
            loginControl.setIpFilteringType(dto.getIpFilteringType());
            loginControl.setAccountLockEnabled(dto.isAccountLockEnabled());
            loginControl.setMaxFailureAttempts(dto.getMaxFailureAttempts());
            loginControl.setLockoutDurationMinutes(dto.getLockoutDurationMinutes());
            loginControl.setTwoFactorType(dto.getTwoFactorType());
        }
    }
}