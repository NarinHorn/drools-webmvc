package com.hunesion.drool_v2.service;

import com.hunesion.drool_v2.model.entity.EquipmentPolicy;
import com.hunesion.drool_v2.model.entity.PolicyCommonSettings;
import com.hunesion.drool_v2.model.entity.PolicyCommandSettings;
import com.hunesion.drool_v2.model.entity.PolicyLoginControl;
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
     */
    public String generatePolicyRule(EquipmentPolicy policy) {
        StringBuilder drl = new StringBuilder();
        String ruleName = sanitizeRuleName(policy.getPolicyName());

        drl.append("rule \"").append(ruleName).append("\"\n");
        drl.append("    salience ").append(policy.getPriority()).append("\n");
        drl.append("    when\n");
        drl.append("        $request : EquipmentAccessRequest(\n");

        // Check if policy is assigned
        drl.append("            isAssignedToPolicy(").append(policy.getId()).append(")\n");

        // Build conditions based on policy settings
        StringBuilder conditions = new StringBuilder();

        // Check if policy uses JSONB config (new approach) or normalized tables (old approach)
        String policyConfigJson = policy.getPolicyConfig();
        if (policyConfigJson != null && !policyConfigJson.isEmpty()) {
            // NEW: Use cached JSONB config parsing
            Map<String, Object> config = policyConfigCache.getParsedConfig(
                policy.getId(), 
                policyConfigJson
            );

            // Generate from commonSettings
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
                    if (conditions.length() > 0) {
                        conditions.append("            , (").append(dbmsCheck).append(")\n");
                    } else {
                        conditions.append("            (").append(dbmsCheck).append(")\n");
                    }
                }
            }

            // Generate from allowedTime
            @SuppressWarnings("unchecked")
            Map<String, Object> allowedTime = (Map<String, Object>) config.get("allowedTime");
            if (allowedTime != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> timeSlots = (List<Map<String, Object>>) allowedTime.get("timeSlots");
                if (timeSlots != null && !timeSlots.isEmpty()) {
                    if (conditions.length() > 0) {
                        conditions.append("            , isWithinAllowedTime()\n");
                    } else {
                        conditions.append("            isWithinAllowedTime()\n");
                    }
                }
            }

            // Generate from commandSettings
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> commandSettings = (List<Map<String, Object>>) config.get("commandSettings");
            if (commandSettings != null && !commandSettings.isEmpty()) {
                for (Map<String, Object> cmdSetting : commandSettings) {
                    String controlMethod = (String) cmdSetting.get("controlMethod");
                    if ("blacklist".equals(controlMethod)) {
                        if (conditions.length() > 0) {
                            conditions.append("            , !isCommandBlocked(command)\n");
                        } else {
                            conditions.append("            !isCommandBlocked(command)\n");
                        }
                    }
                }
            }

            // Generate from loginControl
            @SuppressWarnings("unchecked")
            Map<String, Object> loginControl = (Map<String, Object>) config.get("loginControl");
            if (loginControl != null) {
                String ipFilteringType = (String) loginControl.get("ipFilteringType");
                if (ipFilteringType != null && !"no_restrictions".equals(ipFilteringType)) {
                    if (conditions.length() > 0) {
                        conditions.append("            , isIpAllowed(clientIp)\n");
                    } else {
                        conditions.append("            isIpAllowed(clientIp)\n");
                    }
                }
            }

            // Generate from customConditions (like AccessPolicy)
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
                        if (conditions.length() > 0) conditions.append(",\n");
                        conditions.append("            ").append(conditionStr);
                    }
                }
            }
        } else {
            // OLD: Fall back to normalized tables (backward compatibility)
            PolicyCommonSettings commonSettings = policy.getCommonSettings();
            if (commonSettings != null && !commonSettings.getAllowedProtocols().isEmpty()) {
                List<String> protocols = commonSettings.getAllowedProtocols().stream()
                        .map(p -> p.getProtocol())
                        .collect(Collectors.toList());

                String protocolCheck = protocols.stream()
                        .map(p -> "hasProtocol(\"" + p + "\")")
                        .collect(Collectors.joining(" || "));

                conditions.append("            , (").append(protocolCheck).append(")\n");
            }

            if (commonSettings != null && !commonSettings.getAllowedDbms().isEmpty()) {
                List<String> dbms = commonSettings.getAllowedDbms().stream()
                        .map(d -> d.getDbmsType())
                        .collect(Collectors.toList());

                String dbmsCheck = dbms.stream()
                        .map(d -> "hasDbmsType(\"" + d + "\")")
                        .collect(Collectors.joining(" || "));

                if (conditions.length() > 0) {
                    conditions.append("            , (").append(dbmsCheck).append(")\n");
                } else {
                    conditions.append("            (").append(dbmsCheck).append(")\n");
                }
            }

            if (policy.getAllowedTime() != null && !policy.getAllowedTime().getTimeSlots().isEmpty()) {
                if (conditions.length() > 0) {
                    conditions.append("            , isWithinAllowedTime()\n");
                } else {
                    conditions.append("            isWithinAllowedTime()\n");
                }
            }

            if (!policy.getCommandSettings().isEmpty()) {
                for (PolicyCommandSettings cmdSettings : policy.getCommandSettings()) {
                    if ("blacklist".equals(cmdSettings.getControlMethod())) {
                        if (conditions.length() > 0) {
                            conditions.append("            , !isCommandBlocked(command)\n");
                        } else {
                            conditions.append("            !isCommandBlocked(command)\n");
                        }
                    }
                }
            }

            PolicyLoginControl loginControl = policy.getLoginControl();
            if (loginControl != null && !"no_restrictions".equals(loginControl.getIpFilteringType())) {
                if (conditions.length() > 0) {
                    conditions.append("            , isIpAllowed(clientIp)\n");
                } else {
                    conditions.append("            isIpAllowed(clientIp)\n");
                }
            }
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

        return drl.toString();
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