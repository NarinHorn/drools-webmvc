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

        // Note: Assignments are now managed via separate endpoints
        // /api/equipment-policies/{id}/assignments/*

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

        // Note: Assignments are now managed via separate endpoints
        // /api/equipment-policies/{id}/assignments/*

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

    // Assignment management has been moved to EquipmentPolicyAssignmentService
    // Use endpoints: /api/equipment-policies/{id}/assignments/*
}