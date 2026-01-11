package com.hunesion.drool_v2.service;

import com.hunesion.drool_v2.dto.EquipmentAccessRequestDTO;
import com.hunesion.drool_v2.dto.EquipmentAccessResponseDTO;
import com.hunesion.drool_v2.model.EquipmentAccessRequest;
import com.hunesion.drool_v2.model.EquipmentAccessResult;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * EquipmentAccessControlService - Evaluates equipment access using Drools
 */
@Service
public class EquipmentAccessControlService {

    private final PolicyFactLoader factLoader;
    private final DynamicRuleService dynamicRuleService;

    @Autowired
    public EquipmentAccessControlService(
            PolicyFactLoader factLoader,
            DynamicRuleService dynamicRuleService) {
        this.factLoader = factLoader;
        this.dynamicRuleService = dynamicRuleService;
    }

    /**
     * Check equipment access for a user
     */
    public EquipmentAccessResponseDTO checkAccess(EquipmentAccessRequestDTO requestDto) {
        // Load policy data into fact
        EquipmentAccessRequest request = factLoader.loadPoliciesIntoFact(
                requestDto.getUsername(),
                requestDto.getEquipmentId()
        );

        // Set request-specific data
        request.setProtocol(requestDto.getProtocol());
        request.setDbmsType(requestDto.getDbmsType());
        request.setCommand(requestDto.getCommand());
        request.setClientIp(requestDto.getClientIp());
        if (requestDto.getRequestTime() != null) {
            request.setRequestTime(requestDto.getRequestTime());
        }

        // Evaluate using Drools
        EquipmentAccessResult result = evaluateAccess(request);

        // Convert to DTO
        EquipmentAccessResponseDTO response = new EquipmentAccessResponseDTO();
        response.setAllowed(result.isAllowed());
        response.setMatchedPolicyName(result.getMatchedPolicyName());
        response.setDenialReason(result.getDenialReason());
        response.setDenialCode(result.getDenialCode());

        return response;
    }

    /**
     * Evaluate access request using Drools
     */
    private EquipmentAccessResult evaluateAccess(EquipmentAccessRequest request) {
        KieSession kieSession = dynamicRuleService.newKieSession();
        EquipmentAccessResult result = new EquipmentAccessResult();

        try {
            kieSession.insert(request);
            kieSession.insert(result);

            int rulesFired = kieSession.fireAllRules();
            System.out.println("Equipment access rules fired: " + rulesFired + " for user: " + request.getUsername());

            // If no rules matched, deny by default
            if (!result.isEvaluated()) {
                result.deny("No Policy Match", "No matching policy found for this equipment access");
            }

        } finally {
            kieSession.dispose();
        }

        return result;
    }
}