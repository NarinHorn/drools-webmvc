package com.hunesion.drool_v2.service;

import com.hunesion.drool_v2.entity.User;
import com.hunesion.drool_v2.model.AccessRequest;
import com.hunesion.drool_v2.model.AccessResult;
import com.hunesion.drool_v2.repository.UserRepository;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AccessControlService - Evaluates access requests against policies using Drools
 */
@Service
public class AccessControlService {

    private final DynamicRuleService dynamicRuleService;
    private final UserRepository userRepository;

    @Autowired
    public AccessControlService(DynamicRuleService dynamicRuleService,
                                UserRepository userRepository) {
        this.dynamicRuleService = dynamicRuleService;
        this.userRepository = userRepository;
    }

    /**
     * Check if a user has access to a specific endpoint
     */
    public AccessResult checkAccess(String username, String endpoint, String httpMethod) {
        User user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null) {
            AccessResult result = new AccessResult();
            result.deny("User Not Found", "User does not exist: " + username);
            return result;
        }

        if (!user.isActive()) {
            AccessResult result = new AccessResult();
            result.deny("User Inactive", "User account is disabled");
            return result;
        }

        // Build access request from user data
        AccessRequest request = new AccessRequest();
        request.setUsername(username);
        request.setUserRoles(user.getRoleNames());
        request.setEndpoint(endpoint);
        request.setHttpMethod(httpMethod);
        request.setDepartment(user.getDepartment());
        request.setUserLevel(user.getLevel());
        
        // Copy user attributes
        if (user.getAttributes() != null) {
            user.getAttributes().forEach((k, v) -> request.setAttribute(k, v));
        }

        return evaluateAccess(request);
    }

    /**
     * Evaluate access request directly (for testing or custom requests)
     */
    public AccessResult evaluateAccess(AccessRequest request) {
        KieSession kieSession = dynamicRuleService.newKieSession();
        AccessResult result = new AccessResult();

        try {
            kieSession.insert(request);
            kieSession.insert(result);
            
            int rulesFired = kieSession.fireAllRules();
            System.out.println("Access control rules fired: " + rulesFired + " for " + request.getEndpoint());
            
            // If no rules matched, deny by default
            if (!result.isEvaluated()) {
                result.deny("No Policy Match", "No access policy found for this endpoint");
            }
            
        } finally {
            kieSession.dispose();
        }

        return result;
    }

    /**
     * Check access with a pre-built AccessRequest
     */
    public boolean isAllowed(AccessRequest request) {
        return evaluateAccess(request).isAllowed();
    }

    /**
     * Simple check for username, endpoint, method combination
     */
    public boolean isAllowed(String username, String endpoint, String httpMethod) {
        return checkAccess(username, endpoint, httpMethod).isAllowed();
    }
}
