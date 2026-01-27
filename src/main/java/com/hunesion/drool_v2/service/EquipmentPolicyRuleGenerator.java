package com.hunesion.drool_v2.service;

import com.hunesion.drool_v2.model.entity.EquipmentPolicy;
import com.hunesion.drool_v2.repository.EquipmentPolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * EquipmentPolicyRuleGenerator - Generates Drools DRL rules from EquipmentPolicy entities
 * This service converts normalized database data into Drools rules
 */
@Service
public class EquipmentPolicyRuleGenerator {

    private final EquipmentPolicyRepository policyRepository;
    private final PolicyConfigCache policyConfigCache;

    @Autowired
    public EquipmentPolicyRuleGenerator(
            EquipmentPolicyRepository policyRepository,
            PolicyConfigCache policyConfigCache) {
        this.policyRepository = policyRepository;
        this.policyConfigCache = policyConfigCache;
    }

    /**
     * Generate DRL for all enabled policies
     */
    @Transactional(readOnly = true)
    public String generateAllPolicyRules() {
        List<EquipmentPolicy> policies = policyRepository.findByEnabledTrueOrderByPriorityDesc();

        StringBuilder combinedDrl = new StringBuilder();
        combinedDrl.append("package rules.equipment;\n\n");
        combinedDrl.append("import com.hunesion.drool_v2.model.EquipmentAccessRequest;\n");
        combinedDrl.append("import com.hunesion.drool_v2.model.EquipmentAccessResult;\n\n");

        for (EquipmentPolicy policy : policies) {
            if (!"apply".equals(policy.getPolicyApplication())) {
                continue;
            }

            combinedDrl.append(generatePolicyRule(policy));
            combinedDrl.append("\n\n");
        }

        return combinedDrl.toString();
    }

    /**
     * Generate DRL for a single policy
     * Each policy handles only one type of configuration
     */
    public String generatePolicyRule(EquipmentPolicy policy) {
        StringBuilder drl = new StringBuilder();
        String ruleName = sanitizeRuleName(policy.getPolicyName());
        String typeCode = policy.getPolicyType().getTypeCode();

        drl.append("rule \"").append(ruleName).append("\"\n");
        drl.append("    salience ").append(policy.getPriority()).append("\n");
        drl.append("    when\n");
        drl.append("        $request : EquipmentAccessRequest(\n");

        // Check if policy is assigned
        drl.append("            isAssignedToPolicy(").append(policy.getId()).append(")\n");

        // Build conditions based on policy type
        StringBuilder conditions = new StringBuilder();

        String policyConfigJson = policy.getPolicyConfig();
        if (policyConfigJson != null && !policyConfigJson.isEmpty()) {
            // Use cached JSONB config parsing
            Map<String, Object> config = policyConfigCache.getParsedConfig(
                policy.getId(),
                policyConfigJson
            );

            // Generate conditions based on policy type
            switch (typeCode) {
                case "commonSettings":
                    generateCommonSettingsConditions(conditions, config);
                    break;
                case "allowedTime":
                    generateAllowedTimeConditions(conditions);
                    break;
                case "loginControl":
                    generateLoginControlConditions(conditions, config);
                    break;
                case "commandSettings":
                    generateCommandSettingsConditions(conditions, config);
                    break;
                default:
                    // Unknown type - no conditions
                    break;
            }

            // Always check customConditions if present (can be in any policy type)
            generateCustomConditions(conditions, config);
        }

        drl.append(conditions);
        drl.append("        )\n");
        drl.append("        $result : EquipmentAccessResult(evaluated == false)\n");
        drl.append("    then\n");
        drl.append("        modify($result) {\n");
        drl.append("            setAllowed(true),\n");
        drl.append("            setEvaluated(true),\n");
        drl.append("            setMatchedPolicyName(\"").append(ruleName).append("\")\n");
        drl.append("        }\n");
        drl.append("        System.out.println(\"✓ Equipment access ALLOWED by policy: ").append(ruleName).append("\");\n");
        drl.append("end\n");

        // Log generated DRL for debugging
        System.out.println("=== Generated DRL for policy: " + policy.getPolicyName() + " (ID: " + policy.getId() + ", Type: " + typeCode + ") ===");
        System.out.println(drl.toString());
        System.out.println("=== End DRL ===\n");

        return drl.toString();
    }

    private void generateCommonSettingsConditions(StringBuilder conditions, Map<String, Object> config) {
        @SuppressWarnings("unchecked")
        Map<String, Object> commonSettings = (Map<String, Object>) config.get("commonSettings");
        if (commonSettings != null) {
            @SuppressWarnings("unchecked")
            List<String> protocols = (List<String>) commonSettings.get("allowedProtocols");
            if (protocols != null && !protocols.isEmpty()) {
                String protocolCheck = protocols.stream()
                        .map(p -> "hasProtocol(\"" + p + "\")")
                        .collect(Collectors.joining(" || "));
                conditions.append("            , (").append(protocolCheck).append(")\n");
            }

            @SuppressWarnings("unchecked")
            List<String> dbms = (List<String>) commonSettings.get("allowedDbms");
            if (dbms != null && !dbms.isEmpty()) {
                String dbmsCheck = dbms.stream()
                        .map(d -> "hasDbmsType(\"" + d + "\")")
                        .collect(Collectors.joining(" || "));
                // dbmsType == null means request is not a DB request (e.g., SSH), so skip DBMS check
                conditions.append("            , (dbmsType == null || ").append(dbmsCheck).append(")\n");
            }
        }
    }

    private void generateAllowedTimeConditions(StringBuilder conditions) {
        // Always require time check
        // If no timeSlots defined, isWithinAllowedTime() will return false (deny access)
        conditions.append("            , isWithinAllowedTime()\n");
    }

    private void generateLoginControlConditions(StringBuilder conditions, Map<String, Object> config) {
        @SuppressWarnings("unchecked")
        Map<String, Object> loginControl = (Map<String, Object>) config.get("loginControl");
        if (loginControl != null) {
            String ipFilteringType = (String) loginControl.get("ipFilteringType");
            if (ipFilteringType != null && !"no_restrictions".equals(ipFilteringType)) {
                conditions.append("            , isIpAllowed(clientIp)\n");
            }
        }
    }

    private void generateCommandSettingsConditions(StringBuilder conditions, Map<String, Object> config) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> commandSettings = (List<Map<String, Object>>) config.get("commandSettings");
        if (commandSettings != null && !commandSettings.isEmpty()) {
            for (Map<String, Object> cmdSetting : commandSettings) {
                String controlMethod = (String) cmdSetting.get("controlMethod");
                if ("blacklist".equals(controlMethod)) {
                    conditions.append("            , !isCommandBlocked(command)\n");
                }
            }
        }
    }

    private void generateCustomConditions(StringBuilder conditions, Map<String, Object> config) {
        @SuppressWarnings("unchecked")
        Map<String, Object> customConditions = (Map<String, Object>) config.get("customConditions");
        if (customConditions != null) {
            for (Map.Entry<String, Object> entry : customConditions.entrySet()) {
                String attribute = entry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, String> condition = (Map<String, String>) entry.getValue();
                String operator = condition.get("operator");
                String value = condition.get("value");

                String conditionStr = buildConditionFromJson(attribute, operator, value);
                if (conditionStr != null) {
                    conditions.append("            , ").append(conditionStr).append("\n");
                }
            }
        }
    }

    private String sanitizeRuleName(String name) {
        return name.replaceAll("[^a-zA-Z0-9\\s_-]", "").trim();
    }

    private String buildConditionFromJson(String attribute, String operator, String value) {
        String getter = "attributes.get(\"" + attribute + "\")";
        return switch (operator.toLowerCase()) {
            case "equals" -> getter + " != null && " + getter + ".toString().equals(\"" + value + "\")";
            case "notequals" -> getter + " == null || !" + getter + ".toString().equals(\"" + value + "\")";
            case "contains" -> getter + " != null && " + getter + ".toString().contains(\"" + value + "\")";
            case "matches" -> getter + " != null && " + getter + ".toString().matches(\"" + value + "\")";
            case "greaterthan" -> getter + " != null && Double.parseDouble(" + getter + ".toString()) > " + value;
            case "lessthan" -> getter + " != null && Double.parseDouble(" + getter + ".toString()) < " + value;
            default -> null;
        };
    }
}