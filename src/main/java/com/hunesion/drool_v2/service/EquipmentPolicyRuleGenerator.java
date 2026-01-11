package com.hunesion.drool_v2.service;

import com.hunesion.drool_v2.model.entity.EquipmentPolicy;
import com.hunesion.drool_v2.model.entity.PolicyCommonSettings;
import com.hunesion.drool_v2.model.entity.PolicyCommandSettings;
import com.hunesion.drool_v2.model.entity.PolicyLoginControl;
import com.hunesion.drool_v2.repository.EquipmentPolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * EquipmentPolicyRuleGenerator - Generates Drools DRL rules from EquipmentPolicy entities
 * This service converts normalized database data into Drools rules
 */
@Service
public class EquipmentPolicyRuleGenerator {

    private final EquipmentPolicyRepository policyRepository;

    @Autowired
    public EquipmentPolicyRuleGenerator(EquipmentPolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
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

        // Protocol check - Check request protocol directly against this policy's allowed protocols
        PolicyCommonSettings commonSettings = policy.getCommonSettings();
        if (commonSettings != null && !commonSettings.getAllowedProtocols().isEmpty()) {
            List<String> protocols = commonSettings.getAllowedProtocols().stream()
                    .map(p -> p.getProtocol())
                    .collect(Collectors.toList());

            // Use hasProtocol() to check request's protocol against this policy's allowed protocols
            String protocolCheck = protocols.stream()
                    .map(p -> "hasProtocol(\"" + p + "\")")
                    .collect(Collectors.joining(" || "));

            conditions.append("            , (").append(protocolCheck).append(")\n");
        }

        // DBMS check - Check request DBMS directly against this policy's allowed DBMS
        if (commonSettings != null && !commonSettings.getAllowedDbms().isEmpty()) {
            List<String> dbms = commonSettings.getAllowedDbms().stream()
                    .map(d -> d.getDbmsType())
                    .collect(Collectors.toList());

            // Use hasDbmsType() to check request's DBMS against this policy's allowed DBMS
            String dbmsCheck = dbms.stream()
                    .map(d -> "hasDbmsType(\"" + d + "\")")
                    .collect(Collectors.joining(" || "));

            if (conditions.length() > 0) {
                conditions.append("            , (").append(dbmsCheck).append(")\n");
            } else {
                conditions.append("            (").append(dbmsCheck).append(")\n");
            }
        }

        // Time slot check
        if (policy.getAllowedTime() != null && !policy.getAllowedTime().getTimeSlots().isEmpty()) {
            if (conditions.length() > 0) {
                conditions.append("            , isWithinAllowedTime()\n");
            } else {
                conditions.append("            isWithinAllowedTime()\n");
            }
        }

        // Command check
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

        // IP filtering check
        PolicyLoginControl loginControl = policy.getLoginControl();
        if (loginControl != null && !"no_restrictions".equals(loginControl.getIpFilteringType())) {
            if (conditions.length() > 0) {
                conditions.append("            , isIpAllowed(clientIp)\n");
            } else {
                conditions.append("            isIpAllowed(clientIp)\n");
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
}