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
    private final DynamicRuleService dynamicRuleService;
    private final ObjectMapper objectMapper;
    private final PolicyConfigCache policyConfigCache;
    private final PolicyTypeRepository policyTypeRepository;

    @Autowired
    public EquipmentPolicyService(
            EquipmentPolicyRepository policyRepository,
            UserRepository userRepository,
            UserGroupRepository groupRepository,
            EquipmentRepository equipmentRepository,
            RoleRepository roleRepository,
            CommandListRepository commandListRepository,
            DynamicRuleService dynamicRuleService,
            ObjectMapper objectMapper,
            PolicyConfigCache policyConfigCache,
            PolicyTypeRepository policyTypeRepository) {
        this.policyRepository = policyRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.equipmentRepository = equipmentRepository;
        this.roleRepository = roleRepository;
        this.commandListRepository = commandListRepository;
        this.dynamicRuleService = dynamicRuleService;
        this.objectMapper = objectMapper;
        this.policyConfigCache = policyConfigCache;
        this.policyTypeRepository = policyTypeRepository;
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

        // Validate and get policy type
        if (dto.getPolicyTypeId() == null) {
            throw new RuntimeException("Policy type ID is required");
        }
        PolicyType policyType = policyTypeRepository.findById(dto.getPolicyTypeId())
                .orElseThrow(() -> new RuntimeException("Policy type not found: " + dto.getPolicyTypeId()));

        EquipmentPolicy policy = convertDtoToEntity(dto);
        policy.setPolicyType(policyType);
        
        // Build policy_config JSON for this specific policy type
        String policyConfigJson = buildPolicyConfigJsonForType(dto, policyType.getTypeCode());
        policy.setPolicyConfig(policyConfigJson);
        
        EquipmentPolicy saved = policyRepository.save(policy);

        // Note: Assignments are now managed via separate endpoints
        // /api/equipment-policies/{id}/assignments/*

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

        // Update policy type if provided (optional - usually shouldn't change)
        if (dto.getPolicyTypeId() != null && !dto.getPolicyTypeId().equals(existing.getPolicyType().getId())) {
            PolicyType policyType = policyTypeRepository.findById(dto.getPolicyTypeId())
                    .orElseThrow(() -> new RuntimeException("Policy type not found: " + dto.getPolicyTypeId()));
            existing.setPolicyType(policyType);
        }

        // Build and update policy_config JSON for the policy type
        String typeCode = existing.getPolicyType().getTypeCode();
        String policyConfigJson = buildPolicyConfigJsonForType(dto, typeCode);
        existing.setPolicyConfig(policyConfigJson);

        EquipmentPolicy saved = policyRepository.save(existing);

        // Note: Assignments are now managed via separate endpoints
        // /api/equipment-policies/{id}/assignments/*

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
     * Build policy_config JSON from DTO for a specific policy type.
     * Only includes the section matching the policy type.
     * If dto.getPolicyConfig() is provided, use it directly (must match the type).
     * Otherwise, build from individual DTO fields.
     */
    private String buildPolicyConfigJsonForType(EquipmentPolicyDTO dto, String typeCode) {
        Map<String, Object> policyConfig = new HashMap<>();

        // If raw policyConfig is provided, validate and use it
        if (dto.getPolicyConfig() != null && !dto.getPolicyConfig().isEmpty()) {
            // Validate that the config contains the correct section for this type
            if (!dto.getPolicyConfig().containsKey(typeCode)) {
                throw new RuntimeException(
                    "Policy config must contain '" + typeCode + "' section for policy type: " + typeCode
                );
            }
            policyConfig = new HashMap<>(dto.getPolicyConfig());
        } else {
            // Build from individual DTO fields based on policy type
            switch (typeCode) {
                case "commonSettings":
                    if (dto.getCommonSettings() != null) {
                        policyConfig.put("commonSettings", convertCommonSettingsToMap(dto.getCommonSettings()));
                    }
                    break;
                case "allowedTime":
                    if (dto.getAllowedTime() != null) {
                        policyConfig.put("allowedTime", convertAllowedTimeToMap(dto.getAllowedTime()));
                    }
                    break;
                case "loginControl":
                    if (dto.getLoginControl() != null) {
                        policyConfig.put("loginControl", convertLoginControlToMap(dto.getLoginControl()));
                    }
                    break;
                case "commandSettings":
                    if (dto.getCommandSettings() != null) {
                        policyConfig.put("commandSettings", dto.getCommandSettings().stream()
                            .map(this::convertCommandSettingsToMap)
                            .collect(Collectors.toList()));
                    }
                    break;
                default:
                    throw new RuntimeException("Unknown policy type: " + typeCode);
            }
        }

        // Add customConditions if provided (can be in any policy type)
        if (dto.getCustomConditions() != null) {
            policyConfig.put("customConditions", dto.getCustomConditions());
        }

        // Add customMetadata if provided (can be in any policy type)
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