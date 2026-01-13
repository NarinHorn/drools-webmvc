package com.hunesion.drool_v2.service;

import com.hunesion.drool_v2.dto.ConditionDTO;
import com.hunesion.drool_v2.dto.PolicyDTO;
import com.hunesion.drool_v2.model.entity.AccessPolicy;
import com.hunesion.drool_v2.model.entity.AccessPolicyGroupAssignment;
import com.hunesion.drool_v2.model.entity.UserGroup;
import com.hunesion.drool_v2.repository.AccessPolicyRepository;
import com.hunesion.drool_v2.repository.UserGroupRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PolicyService - CRUD operations for access policies and DRL generation
 * 
 * Converts structured PolicyDTO from frontend into DRL rules
 */
@Service
public class PolicyService {

    private final AccessPolicyRepository accessPolicyRepository;
    private final DynamicRuleService dynamicRuleService;
    private final UserGroupRepository userGroupRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public PolicyService(AccessPolicyRepository accessPolicyRepository,
                         DynamicRuleService dynamicRuleService,
                         UserGroupRepository userGroupRepository) {
        this.accessPolicyRepository = accessPolicyRepository;
        this.dynamicRuleService = dynamicRuleService;
        this.userGroupRepository = userGroupRepository;
        this.objectMapper = new ObjectMapper();
    }

    public List<AccessPolicy> getAllPolicies() {
        return accessPolicyRepository.findAll();
    }

    public List<AccessPolicy> getEnabledPolicies() {
        return accessPolicyRepository.findByEnabledTrueOrderByPriorityDesc();
    }

    public AccessPolicy getPolicyById(Long id) {
        return accessPolicyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found with id: " + id));
    }

    public AccessPolicy getPolicyByName(String name) {
        return accessPolicyRepository.findByPolicyName(name)
                .orElseThrow(() -> new RuntimeException("Policy not found: " + name));
    }

    @Transactional
    public AccessPolicy createPolicy(PolicyDTO dto) {
        if (accessPolicyRepository.existsByPolicyName(dto.getPolicyName())) {
            throw new RuntimeException("Policy already exists: " + dto.getPolicyName());
        }

        AccessPolicy policy = convertDtoToEntity(dto);
        String drl = generateDrl(dto);
        policy.setGeneratedDrl(drl);

        AccessPolicy saved = accessPolicyRepository.save(policy);
        
        // Create group assignments
        createGroupAssignments(saved, dto);
        
        // Save again to persist group assignments
        saved = accessPolicyRepository.save(saved);
        
        // Rebuild rules to include new policy
        dynamicRuleService.rebuildRules();
        
        return saved;
    }

    @Transactional
    public AccessPolicy updatePolicy(Long id, PolicyDTO dto) {
        AccessPolicy existing = getPolicyById(id);
        
        existing.setPolicyName(dto.getPolicyName());
        existing.setDescription(dto.getDescription());
        existing.setEndpoint(dto.getEndpoint());
        existing.setHttpMethod(dto.getHttpMethod());
        existing.setEffect(dto.getEffect());
        
        if (dto.getPriority() != null) {
            existing.setPriority(dto.getPriority());
        }
        if (dto.getEnabled() != null) {
            existing.setEnabled(dto.getEnabled());
        }
        
        // Convert roles and conditions to JSON strings
        try {
            if (dto.getAllowedRoles() != null) {
                existing.setAllowedRoles(objectMapper.writeValueAsString(dto.getAllowedRoles()));
            }
            if (dto.getConditions() != null) {
                existing.setConditions(objectMapper.writeValueAsString(dto.getConditions()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize policy data", e);
        }

        // Clear existing group assignments
        existing.getGroupAssignments().clear();
        
        // Regenerate DRL
        String drl = generateDrl(dto);
        existing.setGeneratedDrl(drl);

        // Create group assignments
        createGroupAssignments(existing, dto);
        
        AccessPolicy saved = accessPolicyRepository.save(existing);
        
        // Rebuild rules
        dynamicRuleService.rebuildRules();
        
        return saved;
    }

    @Transactional
    public void deletePolicy(Long id) {
        accessPolicyRepository.deleteById(id);
        dynamicRuleService.rebuildRules();
    }

    @Transactional
    public AccessPolicy togglePolicy(Long id, boolean enabled) {
        AccessPolicy policy = getPolicyById(id);
        policy.setEnabled(enabled);
        AccessPolicy saved = accessPolicyRepository.save(policy);
        dynamicRuleService.rebuildRules();
        return saved;
    }

    /**
     * Regenerate DRL for all existing policies
     * Useful after fixing regex conversion logic
     */
    @Transactional
    public void regenerateAllPoliciesDrl() {
        System.out.println("Regenerating DRL for all existing policies...");
        List<AccessPolicy> policies = accessPolicyRepository.findAll();
        
        for (AccessPolicy policy : policies) {
            try {
                PolicyDTO dto = convertEntityToDto(policy);
                String newDrl = generateDrl(dto);
                policy.setGeneratedDrl(newDrl);
                accessPolicyRepository.save(policy);
                System.out.println("  ✓ Regenerated DRL for: " + policy.getPolicyName());
            } catch (Exception e) {
                System.err.println("  ✗ Failed to regenerate DRL for " + policy.getPolicyName() + ": " + e.getMessage());
            }
        }
        
        // Rebuild rules with updated DRL
        dynamicRuleService.rebuildRules();
        System.out.println("✓ All policies DRL regenerated and rules rebuilt");
    }

    /**
     * Convert AccessPolicy entity to PolicyDTO
     */
    private PolicyDTO convertEntityToDto(AccessPolicy policy) {
        PolicyDTO dto = new PolicyDTO();
        dto.setId(policy.getId());
        dto.setPolicyName(policy.getPolicyName());
        dto.setDescription(policy.getDescription());
        dto.setEndpoint(policy.getEndpoint());
        dto.setHttpMethod(policy.getHttpMethod());
        dto.setEffect(policy.getEffect());
        dto.setPriority(policy.getPriority());
        dto.setEnabled(policy.isEnabled());

        try {
            if (policy.getAllowedRoles() != null && !policy.getAllowedRoles().isEmpty()) {
                @SuppressWarnings("unchecked")
                List<String> roles = objectMapper.readValue(policy.getAllowedRoles(), List.class);
                dto.setAllowedRoles(roles);
            }
            if (policy.getConditions() != null && !policy.getConditions().isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, ConditionDTO> conditions = objectMapper.readValue(
                    policy.getConditions(), 
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, ConditionDTO.class)
                );
                dto.setConditions(conditions);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize policy data", e);
        }
        
        // Convert group assignments to groupIds
        if (policy.getGroupAssignments() != null && !policy.getGroupAssignments().isEmpty()) {
            List<Long> groupIds = policy.getGroupAssignments().stream()
                    .map(ga -> ga.getGroup().getId())
                    .collect(Collectors.toList());
            dto.setGroupIds(groupIds);
        }

        return dto;
    }

    private AccessPolicy convertDtoToEntity(PolicyDTO dto) {
        AccessPolicy policy = new AccessPolicy();
        policy.setPolicyName(dto.getPolicyName());
        policy.setDescription(dto.getDescription());
        policy.setEndpoint(dto.getEndpoint());
        policy.setHttpMethod(dto.getHttpMethod());
        policy.setEffect(dto.getEffect() != null ? dto.getEffect() : "ALLOW");
        policy.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
        policy.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);

        try {
            if (dto.getAllowedRoles() != null) {
                policy.setAllowedRoles(objectMapper.writeValueAsString(dto.getAllowedRoles()));
            }
            if (dto.getConditions() != null) {
                policy.setConditions(objectMapper.writeValueAsString(dto.getConditions()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize policy data", e);
        }

        return policy;
    }

    /**
     * Generates DRL rule from PolicyDTO
     * This is the core method that converts structured policy to Drools rule
     */
    public String generateDrl(PolicyDTO dto) {
        StringBuilder drl = new StringBuilder();
        
        // Package and imports
        drl.append("package rules.dynamic;\n\n");
        drl.append("import com.hunesion.drool_v2.model.AccessRequest;\n");
        drl.append("import com.hunesion.drool_v2.model.AccessResult;\n\n");

        // Rule definition
        String ruleName = sanitizeRuleName(dto.getPolicyName());
        drl.append("rule \"").append(ruleName).append("\"\n");
        
        // Salience (priority)
        int priority = dto.getPriority() != null ? dto.getPriority() : 0;
        drl.append("    salience ").append(priority).append("\n");
        
        // When clause
        drl.append("    when\n");
        drl.append("        $request : AccessRequest(\n");
        
        // Build conditions
        StringBuilder conditions = new StringBuilder();
        
        // Endpoint matching
        String endpoint = dto.getEndpoint();
        if (endpoint != null && !endpoint.isEmpty()) {
            String regexPattern = convertEndpointToRegex(endpoint);
            conditions.append("            endpointMatches(\"").append(regexPattern).append("\")");
        }
        
        // HTTP method
        if (dto.getHttpMethod() != null && !dto.getHttpMethod().equals("*")) {
            if (conditions.length() > 0) conditions.append(",\n");
            conditions.append("            httpMethod == \"").append(dto.getHttpMethod()).append("\"");
        }
        
        // Role check - at least one role must match
        String roleCheck = null;
        if (dto.getAllowedRoles() != null && !dto.getAllowedRoles().isEmpty()) {
            roleCheck = dto.getAllowedRoles().stream()
                    .map(role -> "hasRole(\"" + role + "\")")
                    .collect(Collectors.joining(" || "));
        }
        
        // Group check - at least one group must match
        String groupCheck = null;
        if (dto.getGroupIds() != null && !dto.getGroupIds().isEmpty()) {
            // Load group names from IDs
            List<String> groupNames = dto.getGroupIds().stream()
                    .map(groupId -> userGroupRepository.findById(groupId)
                            .orElseThrow(() -> new RuntimeException("Group not found: " + groupId))
                            .getGroupName())
                    .collect(Collectors.toList());
            
            groupCheck = groupNames.stream()
                    .map(group -> "hasGroup(\"" + group + "\")")
                    .collect(Collectors.joining(" || "));
        }
        
        // Combine role and group checks with OR if both exist, otherwise use whichever exists
        if (roleCheck != null && groupCheck != null) {
            // Both role and group checks exist - combine with OR
            if (conditions.length() > 0) conditions.append(",\n");
            conditions.append("            (").append(roleCheck).append(" || ").append(groupCheck).append(")");
        } else if (roleCheck != null) {
            // Only role check
            if (conditions.length() > 0) conditions.append(",\n");
            conditions.append("            (").append(roleCheck).append(")");
        } else if (groupCheck != null) {
            // Only group check
            if (conditions.length() > 0) conditions.append(",\n");
            conditions.append("            (").append(groupCheck).append(")");
        }
        
        // Additional attribute conditions
        if (dto.getConditions() != null && !dto.getConditions().isEmpty()) {
            for (Map.Entry<String, ConditionDTO> entry : dto.getConditions().entrySet()) {
                String attribute = entry.getKey();
                ConditionDTO condition = entry.getValue();
                String conditionStr = buildCondition(attribute, condition);
                if (conditionStr != null) {
                    if (conditions.length() > 0) conditions.append(",\n");
                    conditions.append("            ").append(conditionStr);
                }
            }
        }
        
        drl.append(conditions);
        drl.append("\n        )\n");
        drl.append("        $result : AccessResult(evaluated == false)\n");
        
        // Then clause
        drl.append("    then\n");
        
        if ("ALLOW".equalsIgnoreCase(dto.getEffect())) {
            drl.append("        $result.allow(\"").append(ruleName).append("\");\n");
            drl.append("        System.out.println(\"✓ Access ALLOWED by policy: ").append(ruleName).append("\");\n");
        } else {
            drl.append("        $result.deny(\"").append(ruleName).append("\", \"Access denied by policy\");\n");
            drl.append("        System.out.println(\"✗ Access DENIED by policy: ").append(ruleName).append("\");\n");
        }
        
        drl.append("end\n");
        
        return drl.toString();
    }

    private String sanitizeRuleName(String name) {
        return name.replaceAll("[^a-zA-Z0-9\\s_-]", "").trim();
    }

    private String convertEndpointToRegex(String endpoint) {
        // Convert /api/users/** to /api/users(/.*)? (matches /api/users and /api/users/anything)
        // Convert /api/users/* to /api/users/[^/]* (matches single segment like /api/users/123)
        
        // Handle ** pattern at the end: should match both base path and sub-paths
        if (endpoint.endsWith("/**")) {
            // Remove /** and add (/.*)? to match base path and any sub-paths
            // This allows /api/sales/** to match both /api/sales and /api/sales/anything
            String base = endpoint.substring(0, endpoint.length() - 3);
            return base + "(/.*)?";
        }
        
        // Handle ** in the middle (less common, but handle it)
        if (endpoint.contains("**")) {
            // Replace ** with placeholder first
            String regex = endpoint.replace("**", "__DOUBLE_STAR__");
            // Replace single * with [^/]*
            regex = regex.replace("*", "[^/]*");
            // Replace placeholder with .* (matches any characters including slashes)
            regex = regex.replace("__DOUBLE_STAR__", ".*");
            return regex;
        }
        
        // Handle single * pattern (matches single path segment)
        return endpoint.replace("*", "[^/]*");
    }

    private String buildCondition(String attribute, ConditionDTO condition) {
        String operator = condition.getOperator();
        String value = condition.getValue();
        
        return switch (attribute) {
            case "department" -> buildStringCondition("department", operator, value);
            case "userLevel" -> buildNumericCondition("userLevel", operator, value);
            default -> buildAttributeCondition(attribute, operator, value);
        };
    }

    private String buildStringCondition(String field, String operator, String value) {
        return switch (operator.toLowerCase()) {
            case "equals" -> field + " == \"" + value + "\"";
            case "notequals" -> field + " != \"" + value + "\"";
            case "contains" -> field + " != null && " + field + ".contains(\"" + value + "\")";
            case "matches" -> field + " != null && " + field + ".matches(\"" + value + "\")";
            default -> null;
        };
    }

    private String buildNumericCondition(String field, String operator, String value) {
        return switch (operator.toLowerCase()) {
            case "equals" -> field + " == " + value;
            case "notequals" -> field + " != " + value;
            case "greaterthan" -> field + " != null && " + field + " > " + value;
            case "lessthan" -> field + " != null && " + field + " < " + value;
            case "greaterthanorequal" -> field + " != null && " + field + " >= " + value;
            case "lessthanorequal" -> field + " != null && " + field + " <= " + value;
            default -> null;
        };
    }

    private String buildAttributeCondition(String attribute, String operator, String value) {
        String getter = "attributes.get(\"" + attribute + "\")";
        return switch (operator.toLowerCase()) {
            case "equals" -> getter + " != null && " + getter + ".toString().equals(\"" + value + "\")";
            case "notequals" -> getter + " == null || !" + getter + ".toString().equals(\"" + value + "\")";
            default -> null;
        };
    }

    /**
     * Preview DRL without saving - useful for frontend validation
     */
    public String previewDrl(PolicyDTO dto) {
        return generateDrl(dto);
    }
    
    /**
     * Create group assignments for an AccessPolicy
     */
    private void createGroupAssignments(AccessPolicy policy, PolicyDTO dto) {
        if (dto.getGroupIds() != null) {
            dto.getGroupIds().forEach(groupId -> {
                UserGroup group = userGroupRepository.findById(groupId)
                        .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));
                AccessPolicyGroupAssignment assignment = new AccessPolicyGroupAssignment(policy, group);
                policy.getGroupAssignments().add(assignment);
            });
        }
    }
}
