package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.model.AccessRequest;
import com.hunesion.drool_v2.model.AccessResult;
import com.hunesion.drool_v2.service.AccessControlService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AccessCheckController - REST API for testing access control
 * 
 * This controller provides endpoints to test if a user has access to specific endpoints
 * Useful for frontend to check access before making requests
 */
@RestController
@RequestMapping("/api/access")
public class AccessCheckController {

    private final AccessControlService accessControlService;

    @Autowired
    public AccessCheckController(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @Operation(
        summary = "Check access by username and endpoint",
        description = "Checks if a user has access to a specific endpoint using query parameters. Retrieves user information from the database and evaluates against all active policies. Returns whether access is allowed and which policy matched."
    )
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkAccess(
            @RequestParam String username,
            @RequestParam String endpoint,
            @RequestParam(defaultValue = "GET") String method) {
        
        AccessResult result = accessControlService.checkAccess(username, endpoint, method);
        
        Map<String, Object> response = new HashMap<>();
        response.put("username", username);
        response.put("endpoint", endpoint);
        response.put("method", method);
        response.put("allowed", result.isAllowed());
        response.put("matchedPolicy", result.getMatchedPolicyName());
        if (!result.isAllowed()) {
            response.put("reason", result.getDenialReason());
        }
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Check access with custom request",
        description = "Evaluates access control using a fully customized AccessRequest object. Allows testing with custom attributes, roles, and conditions. Useful for testing policies with specific connection parameters (e.g., SSH hostname, port) or other custom attributes."
    )
    @PostMapping("/check")
    public ResponseEntity<AccessResult> checkAccessWithRequest(@RequestBody AccessRequest request) {
        AccessResult result = accessControlService.evaluateAccess(request);
        return ResponseEntity.ok(result);
    }
}
