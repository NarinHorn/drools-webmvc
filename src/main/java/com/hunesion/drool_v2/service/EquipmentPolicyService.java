package com.hunesion.drool_v2.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hunesion.drool_v2.dto.*;
import com.hunesion.drool_v2.model.entity.*;
import com.hunesion.drool_v2.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
    private final ObjectMapper objectMapper;
    private final PolicyConfigCache policyConfigCache;

    @Autowired
    public EquipmentPolicyService(
            EquipmentPolicyRepository policyRepository,
            UserRepository userRepository,
            UserGroupRepository groupRepository,
            EquipmentRepository equipmentRepository,
            RoleRepository roleRepository,
            CommandListRepository commandListRepository,
            EquipmentPolicyRuleGenerator ruleGenerator,
            DynamicRuleService dynamicRuleService,
            ObjectMapper objectMapper,
            PolicyConfigCache policyConfigCache) {
        this.policyRepository = policyRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.equipmentRepository = equipmentRepository;
        this.roleRepository = roleRepository;
        this.commandListRepository = commandListRepository;
        this.ruleGenerator = ruleGenerator;
        this.dynamicRuleService = dynamicRuleService;
        this.objectMapper = objectMapper;
        this.policyConfigCache = policyConfigCache;
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
        
        // Build policy_config JSON from DTO
        String policyConfigJson = buildPolicyConfigJson(dto);
        policy.setPolicyConfig(policyConfigJson);
        
        EquipmentPolicy saved = policyRepository.save(policy);

        // Create assignments
        createAssignments(saved, dto);

        // Generate DRL
        String drl = ruleGenerator.generatePolicyRule(saved);
        saved.setGeneratedRuleDrl(drl);
        saved = policyRepository.save(saved);

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

        // Build and update policy_config JSON
        String policyConfigJson = buildPolicyConfigJson(dto);
        existing.setPolicyConfig(policyConfigJson);

        EquipmentPolicy saved = policyRepository.save(existing);

        // Update assignments
        updateAssignments(saved, dto);

        // Generate DRL
        String drl = ruleGenerator.generatePolicyRule(saved);
        saved.setGeneratedRuleDrl(drl);
        saved = policyRepository.save(saved);

        // Evict cache for this policy
        policyConfigCache.evictPolicyConfig(id);

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

        // Don't create normalized entities - they're now in JSONB
        return policy;
    }

    /**
     * Build policy_config JSON from DTO
     */
    private String buildPolicyConfigJson(EquipmentPolicyDTO dto) {
        Map<String, Object> policyConfig = new HashMap<>();

        // Convert commonSettings
        if (dto.getCommonSettings() != null) {
            policyConfig.put("commonSettings", convertCommonSettingsToMap(dto.getCommonSettings()));
        }

        // Convert allowedTime
        if (dto.getAllowedTime() != null) {
            policyConfig.put("allowedTime", convertAllowedTimeToMap(dto.getAllowedTime()));
        }

        // Convert loginControl
        if (dto.getLoginControl() != null) {
            policyConfig.put("loginControl", convertLoginControlToMap(dto.getLoginControl()));
        }

        // Convert commandSettings
        if (dto.getCommandSettings() != null) {
            policyConfig.put("commandSettings", dto.getCommandSettings().stream()
                .map(this::convertCommandSettingsToMap)
                .collect(Collectors.toList()));
        }

        // Add customConditions if provided
        if (dto.getCustomConditions() != null) {
            policyConfig.put("customConditions", dto.getCustomConditions());
        }

        // Add customMetadata if provided
        if (dto.getCustomMetadata() != null) {
            policyConfig.put("customMetadata", dto.getCustomMetadata());
        }

        try {
            return objectMapper.writeValueAsString(policyConfig);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize policy config", e);
        }
    }

    // Helper methods to convert DTOs to Maps
    private Map<String, Object> convertCommonSettingsToMap(PolicyCommonSettingsDTO dto) {
        Map<String, Object> map = new HashMap<>();
        map.put("servicePort", dto.getServicePort());
        map.put("idleTimeMinutes", dto.getIdleTimeMinutes());
        map.put("timeoutMinutes", dto.getTimeoutMinutes());
        map.put("blockingPolicyType", dto.getBlockingPolicyType());
        map.put("sessionBlockingCount", dto.getSessionBlockingCount());
        map.put("maxTelnetSessions", dto.getMaxTelnetSessions());
        map.put("telnetBorderless", dto.isTelnetBorderless());
        map.put("maxSshSessions", dto.getMaxSshSessions());
        map.put("sshBorderless", dto.isSshBorderless());
        map.put("maxRdpSessions", dto.getMaxRdpSessions());
        map.put("rdpBorderless", dto.isRdpBorderless());
        map.put("allowedProtocols", dto.getAllowedProtocols());
        map.put("allowedDbms", dto.getAllowedDbms());
        return map;
    }

    private Map<String, Object> convertAllowedTimeToMap(PolicyAllowedTimeDTO dto) {
        Map<String, Object> map = new HashMap<>();
        if (dto.getStartDate() != null) {
            map.put("startDate", dto.getStartDate().toString());
        }
        if (dto.getEndDate() != null) {
            map.put("endDate", dto.getEndDate().toString());
        }
        map.put("borderless", dto.isBorderless());
        map.put("timeZone", dto.getTimeZone());
        if (dto.getTimeSlots() != null) {
            map.put("timeSlots", dto.getTimeSlots().stream()
                .map(ts -> {
                    Map<String, Object> slot = new HashMap<>();
                    slot.put("dayOfWeek", ts.getDayOfWeek());
                    slot.put("hourStart", ts.getHourStart());
                    slot.put("hourEnd", ts.getHourEnd());
                    return slot;
                })
                .collect(Collectors.toList()));
        }
        return map;
    }

    private Map<String, Object> convertLoginControlToMap(PolicyLoginControlDTO dto) {
        Map<String, Object> map = new HashMap<>();
        map.put("ipFilteringType", dto.getIpFilteringType());
        map.put("accountLockEnabled", dto.isAccountLockEnabled());
        map.put("maxFailureAttempts", dto.getMaxFailureAttempts());
        map.put("lockoutDurationMinutes", dto.getLockoutDurationMinutes());
        map.put("twoFactorType", dto.getTwoFactorType());
        map.put("allowedIps", dto.getAllowedIps());
        return map;
    }

    private Map<String, Object> convertCommandSettingsToMap(PolicyCommandSettingsDTO dto) {
        Map<String, Object> map = new HashMap<>();
        map.put("protocolType", dto.getProtocolType());
        map.put("controlMethod", dto.getControlMethod());
        map.put("controlTarget", dto.getControlTarget());
        map.put("commandListIds", dto.getCommandListIds());
        return map;
    }

    // Deprecated: These methods are kept for backward compatibility but are no longer used
    // Policy config is now stored in JSONB
    @Deprecated
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

    @Deprecated
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

    @Deprecated
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

    // Deprecated: These update methods are no longer used - policy config is now in JSONB
    @Deprecated
    private void updateCommonSettings(EquipmentPolicy policy, PolicyCommonSettingsDTO dto) {
        // No longer used - settings are in JSONB
    }

    @Deprecated
    private void updateAllowedTime(EquipmentPolicy policy, PolicyAllowedTimeDTO dto) {
        // No longer used - settings are in JSONB
    }

    @Deprecated
    private void updateCommandSettings(EquipmentPolicy policy, List<PolicyCommandSettingsDTO> dtoList) {
        // No longer used - settings are in JSONB
    }

    @Deprecated
    private void updateLoginControl(EquipmentPolicy policy, PolicyLoginControlDTO dto) {
        // No longer used - settings are in JSONB
    }
}