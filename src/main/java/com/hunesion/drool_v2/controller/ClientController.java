package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.dto.EquipmentAccessRequestDTO;
import com.hunesion.drool_v2.dto.EquipmentAccessResponseDTO;
import com.hunesion.drool_v2.model.entity.*;
import com.hunesion.drool_v2.repository.*;
import com.hunesion.drool_v2.service.EquipmentAccessControlService;
import com.hunesion.drool_v2.service.PolicyConfigCache;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/clients")
@Tag(name = "Client Action Control", description = "API for client simulator instead of real frontend in NextJS")
public class ClientController {

    private final EquipmentAccessControlService equipmentAccessControlService;
    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentPolicyRepository policyRepository;
    private final WorkGroupRepository workGroupRepository;
    private final AccountRepository accountRepository;
    private final PolicyConfigCache policyConfigCache;

    @Operation(
            summary = "User ssh on their equipment (Linux Server) that has been assigned by admin",
            description = "Evaluates if a user has SSH access to a specific equipment based on policies"
    )
    @GetMapping("/ssh")
    public ResponseEntity<EquipmentAccessResponseDTO> sshToTargetEquipment(
            @Parameter(description = "Username of the user requesting SSH access")
            @RequestParam String username,
            @Parameter(description = "Equipment ID (Linux Server) to SSH into")
            @RequestParam Long equipmentId,
            HttpServletRequest httpRequest) {

        // Build the access request DTO
        EquipmentAccessRequestDTO request = new EquipmentAccessRequestDTO();
        request.setUsername(username);
        request.setEquipmentId(equipmentId);
        request.setProtocol("SSH");  // SSH protocol for this endpoint
        request.setClientIp(getClientIp(httpRequest));
        log.info("client ip: " + getClientIp(httpRequest));
        request.setRequestTime(LocalDateTime.now());

        // Check access via Drools policy evaluation
        EquipmentAccessResponseDTO response = equipmentAccessControlService.checkAccess(request);

        // Return full response - frontend can check isAllowed() and show appropriate UI
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Debug SSH access policy evaluation",
            description = "Returns detailed information about policy resolution for debugging purposes"
    )
    @GetMapping("/ssh/debug")
    public ResponseEntity<Map<String, Object>> debugSshAccess(
            @Parameter(description = "Username of the user requesting SSH access")
            @RequestParam String username,
            @Parameter(description = "Equipment ID (Linux Server) to SSH into")
            @RequestParam Long equipmentId,
            HttpServletRequest httpRequest) {

        Map<String, Object> debug = new LinkedHashMap<>();

        // Build request
        EquipmentAccessRequestDTO request = new EquipmentAccessRequestDTO();
        request.setUsername(username);
        request.setEquipmentId(equipmentId);
        request.setProtocol("SSH");
        request.setClientIp(getClientIp(httpRequest));
        request.setRequestTime(LocalDateTime.now());

        // Check access
        EquipmentAccessResponseDTO response = equipmentAccessControlService.checkAccess(request);

        // Build debug info
        debug.put("requestInfo", Map.of(
                "username", username,
                "equipmentId", equipmentId,
                "protocol", "SSH",
                "clientIp", request.getClientIp(),
                "requestTime", request.getRequestTime().toString()
        ));
        debug.put("accessAllowed", response.isAllowed());
        debug.put("matchedPolicyName", response.getMatchedPolicyName());
        debug.put("denialReason", response.getDenialReason());
        debug.put("denialCode", response.getDenialCode());

        return ResponseEntity.ok(debug);
    }

    /**
     * Extract client IP from request (handles proxies)
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    // ========== POLICY RESOLUTION TESTING ENDPOINTS ==========
    // Priority: WORK_GROUP(300) > USER(200) > USER_TYPE(100) > GLOBAL(0)
    // Higher priority value = more specific = wins

    /**
     * Simulate: User logs in to client application
     * Returns the login method policy that should be applied
     */
    @Operation(summary = "Get login policy for user",
               description = "Returns the effective login method policy for a user based on priority: WORK_GROUP > USER > USER_TYPE > GLOBAL")
    @GetMapping("/login-policy")
    public ResponseEntity<Map<String, Object>> getLoginPolicy(
            @Parameter(description = "Username to check login policy for")
            @RequestParam String username) {
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("username", username);
        result.put("userType", user.getUserType() != null ? user.getUserType().getTypeCode() : null);
        result.put("priorityOrder", "WORK_GROUP(300) > USER(200) > USER_TYPE(100) > GLOBAL(0)");

        List<PolicyMatch> matches = new ArrayList<>();

        // 1. Check WORK_GROUP policies via work groups (priority 300 - HIGHEST)
        List<WorkGroup> userWorkGroups = workGroupRepository.findByUserId(user.getId());
        for (WorkGroup wg : userWorkGroups) {
            for (EquipmentPolicy p : wg.getPolicies()) {
                if ("loginMethods".equals(p.getPolicyType().getTypeCode()) && p.isEnabled()) {
                    matches.add(new PolicyMatch("WORK_GROUP:" + wg.getWorkGroupName(), p.getPriority(), p));
                }
            }
        }

        // 2. Check USER-specific policies (priority 200)
        List<EquipmentPolicy> userPolicies = policyRepository.findAssignedToUser(user.getId());
        for (EquipmentPolicy p : userPolicies) {
            if ("loginMethods".equals(p.getPolicyType().getTypeCode()) && p.isEnabled()) {
                matches.add(new PolicyMatch("USER", p.getPriority(), p));
            }
        }

        // 3. Check USER_TYPE policies (priority 100)
        if (user.getUserType() != null) {
            List<EquipmentPolicy> userTypePolicies = policyRepository.findAssignedToUserType(user.getUserType().getId());
            for (EquipmentPolicy p : userTypePolicies) {
                if ("loginMethods".equals(p.getPolicyType().getTypeCode()) && p.isEnabled()) {
                    matches.add(new PolicyMatch("USER_TYPE:" + user.getUserType().getTypeCode(), p.getPriority(), p));
                }
            }
        }

        // 4. Check GLOBAL policies (priority 0)
        List<EquipmentPolicy> globalPolicies = policyRepository.findByEnabledTrueOrderByPriorityDesc();
        for (EquipmentPolicy p : globalPolicies) {
            if ("loginMethods".equals(p.getPolicyType().getTypeCode()) && 
                p.isEnabled() && 
                p.getPolicyName().startsWith("Global")) {
                matches.add(new PolicyMatch("GLOBAL", 0, p));
            }
        }

        // Sort by priority (highest wins)
        matches.sort((a, b) -> Integer.compare(b.priority, a.priority));

        result.put("allMatchingPolicies", matches.stream().map(m -> Map.of(
                "source", m.source,
                "priority", m.priority,
                "policyName", m.policy.getPolicyName()
        )).collect(Collectors.toList()));

        if (!matches.isEmpty()) {
            PolicyMatch winner = matches.get(0);
            result.put("effectivePolicy", Map.of(
                    "source", winner.source,
                    "priority", winner.priority,
                    "policyName", winner.policy.getPolicyName(),
                    "config", policyConfigCache.getParsedConfig(winner.policy.getId(), winner.policy.getPolicyConfig())
            ));
            result.put("resolution", "Policy '" + winner.policy.getPolicyName() + "' from " + winner.source + " (priority " + winner.priority + ") wins");
        } else {
            result.put("effectivePolicy", null);
            result.put("resolution", "No login policy found");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Simulate: User views their equipment list
     */
    @Operation(summary = "Get user's accessible equipment",
               description = "Returns equipment accessible to user via work groups")
    @GetMapping("/equipment-list")
    public ResponseEntity<Map<String, Object>> getEquipmentList(
            @Parameter(description = "Username to get equipment list for")
            @RequestParam String username) {
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("username", username);
        result.put("userType", user.getUserType() != null ? user.getUserType().getTypeCode() : null);

        // Get work groups for user
        List<WorkGroup> userWorkGroups = workGroupRepository.findByUserId(user.getId());
        Set<Equipment> accessibleEquipment = new HashSet<>();

        for (WorkGroup wg : userWorkGroups) {
            accessibleEquipment.addAll(wg.getEquipment());
        }

        result.put("workGroups", userWorkGroups.stream()
                .map(wg -> Map.of(
                        "id", wg.getId(), 
                        "name", wg.getWorkGroupName(),
                        "equipmentCount", wg.getEquipment().size(),
                        "policyCount", wg.getPolicies().size()
                ))
                .collect(Collectors.toList()));

        result.put("accessibleEquipment", accessibleEquipment.stream()
                .map(e -> Map.of(
                        "id", e.getId(),
                        "deviceName", e.getDeviceName(),
                        "ipAddress", e.getIpAddress(),
                        "deviceType", e.getDeviceType(),
                        "protocol", e.getProtocol()
                ))
                .collect(Collectors.toList()));

        result.put("totalEquipment", accessibleEquipment.size());

        return ResponseEntity.ok(result);
    }

    /**
     * Simulate: User clicks on equipment to get session timeout policy
     */
    @Operation(summary = "Get session timeout policy for user accessing equipment",
               description = "Returns the effective session timeout based on equipment, account type, and global defaults")
    @GetMapping("/session-timeout")
    public ResponseEntity<Map<String, Object>> getSessionTimeout(
            @Parameter(description = "Username requesting access")
            @RequestParam String username,
            @Parameter(description = "Equipment ID to access")
            @RequestParam Long equipmentId,
            @Parameter(description = "Optional account name on the equipment")
            @RequestParam(required = false) String accountName) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Equipment not found: " + equipmentId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("username", username);
        result.put("equipment", equipment.getDeviceName());
        result.put("deviceType", equipment.getDeviceType());
        result.put("priorityOrder", "EQUIPMENT > ACCOUNT_TYPE > GLOBAL");

        List<PolicyMatch> matches = new ArrayList<>();

        // 1. Check equipment-specific policies (device type based)
        List<EquipmentPolicy> equipmentPolicies = policyRepository.findAssignedToEquipment(equipmentId);
        for (EquipmentPolicy p : equipmentPolicies) {
            if ("sessionTimeout".equals(p.getPolicyType().getTypeCode()) && p.isEnabled()) {
                matches.add(new PolicyMatch("EQUIPMENT:" + equipment.getDeviceName(), p.getPriority(), p));
            }
        }

        // 2. Check account type policies if account specified
        if (accountName != null) {
            List<Account> accounts = accountRepository.findByEquipmentIdAndActiveTrue(equipmentId);
            for (Account acc : accounts) {
                if (acc.getAccountName().equals(accountName) && acc.getAccountType() != null) {
                    result.put("accountName", accountName);
                    result.put("accountType", acc.getAccountType().getTypeCode());
                    
                    List<EquipmentPolicy> accTypePolicies = policyRepository.findAssignedToAccountType(acc.getAccountType().getId());
                    for (EquipmentPolicy p : accTypePolicies) {
                        if ("sessionTimeout".equals(p.getPolicyType().getTypeCode()) && p.isEnabled()) {
                            matches.add(new PolicyMatch("ACCOUNT_TYPE:" + acc.getAccountType().getTypeCode(), p.getPriority(), p));
                        }
                    }
                }
            }
        }

        // 3. Check GLOBAL policies
        List<EquipmentPolicy> globalPolicies = policyRepository.findByEnabledTrueOrderByPriorityDesc();
        for (EquipmentPolicy p : globalPolicies) {
            if ("sessionTimeout".equals(p.getPolicyType().getTypeCode()) && 
                p.isEnabled() && 
                p.getPolicyName().startsWith("Global")) {
                matches.add(new PolicyMatch("GLOBAL", 0, p));
            }
        }

        matches.sort((a, b) -> Integer.compare(b.priority, a.priority));

        result.put("allMatchingPolicies", matches.stream().map(m -> Map.of(
                "source", m.source,
                "priority", m.priority,
                "policyName", m.policy.getPolicyName()
        )).collect(Collectors.toList()));

        if (!matches.isEmpty()) {
            PolicyMatch winner = matches.get(0);
            Map<String, Object> config = policyConfigCache.getParsedConfig(winner.policy.getId(), winner.policy.getPolicyConfig());
            result.put("effectivePolicy", Map.of(
                    "source", winner.source,
                    "priority", winner.priority,
                    "policyName", winner.policy.getPolicyName(),
                    "config", config
            ));
            
            // Extract timeout value for easy reading
            if (config.containsKey("sessionTimeout")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> timeoutConfig = (Map<String, Object>) config.get("sessionTimeout");
                result.put("effectiveTimeoutSeconds", timeoutConfig.get("timeoutSeconds"));
            }
            
            result.put("resolution", "Policy '" + winner.policy.getPolicyName() + "' from " + winner.source + " (priority " + winner.priority + ") wins");
        } else {
            result.put("effectivePolicy", null);
            result.put("resolution", "No session timeout policy found");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get available accounts on equipment for user
     */
    @Operation(summary = "Get available accounts on equipment",
               description = "Returns accounts available on the equipment within user's work groups")
    @GetMapping("/equipment/{equipmentId}/accounts")
    public ResponseEntity<Map<String, Object>> getEquipmentAccounts(
            @Parameter(description = "Username requesting access")
            @RequestParam String username,
            @PathVariable Long equipmentId) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Equipment not found: " + equipmentId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("username", username);
        result.put("equipment", equipment.getDeviceName());

        // Get accounts from work groups that contain both user and equipment
        List<WorkGroup> workGroups = workGroupRepository.findByUserIdAndEquipmentId(user.getId(), equipmentId);
        Set<Account> availableAccounts = new HashSet<>();

        for (WorkGroup wg : workGroups) {
            availableAccounts.addAll(wg.getAccounts());
        }

        result.put("workGroups", workGroups.stream()
                .map(wg -> wg.getWorkGroupName())
                .collect(Collectors.toList()));

        result.put("availableAccounts", availableAccounts.stream()
                .map(a -> Map.of(
                        "id", a.getId(),
                        "accountName", a.getAccountName(),
                        "username", a.getUsername(),
                        "accountType", a.getAccountType() != null ? a.getAccountType().getTypeCode() : "N/A"
                ))
                .collect(Collectors.toList()));

        return ResponseEntity.ok(result);
    }

    /**
     * Get all policies applied to a user (comprehensive view)
     */
    @Operation(summary = "Get all policies for user",
               description = "Returns all policies that apply to a user from all sources")
    @GetMapping("/all-policies")
    public ResponseEntity<Map<String, Object>> getAllUserPolicies(
            @Parameter(description = "Username to get all policies for")
            @RequestParam String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("username", username);
        result.put("userType", user.getUserType() != null ? user.getUserType().getTypeCode() : null);
        result.put("roles", user.getRoles().stream().map(Role::getName).collect(Collectors.toList()));

        // Collect policies from all sources (ordered by priority: WORK_GROUP > USER > USER_TYPE > GLOBAL)
        Map<String, List<Map<String, Object>>> policiesBySource = new LinkedHashMap<>();

        // 1. WORK_GROUP policies (priority 300 - HIGHEST)
        List<WorkGroup> workGroups = workGroupRepository.findByUserId(user.getId());
        List<Map<String, Object>> workGroupPolicies = new ArrayList<>();
        for (WorkGroup wg : workGroups) {
            for (EquipmentPolicy p : wg.getPolicies()) {
                if (p.isEnabled()) {
                    workGroupPolicies.add(Map.of(
                            "id", p.getId(),
                            "name", p.getPolicyName(),
                            "type", p.getPolicyType().getTypeCode(),
                            "priority", p.getPriority(),
                            "workGroup", wg.getWorkGroupName()
                    ));
                }
            }
        }
        policiesBySource.put("WORK_GROUP (priority 300)", workGroupPolicies);

        // 2. USER-specific policies (priority 200)
        List<EquipmentPolicy> userPolicies = policyRepository.findAssignedToUser(user.getId());
        policiesBySource.put("USER (priority 200)", userPolicies.stream()
                .filter(EquipmentPolicy::isEnabled)
                .map(p -> Map.<String, Object>of(
                        "id", p.getId(),
                        "name", p.getPolicyName(),
                        "type", p.getPolicyType().getTypeCode(),
                        "priority", p.getPriority()
                ))
                .collect(Collectors.toList()));

        // 3. USER_TYPE policies (priority 100)
        if (user.getUserType() != null) {
            List<EquipmentPolicy> userTypePolicies = policyRepository.findAssignedToUserType(user.getUserType().getId());
            policiesBySource.put("USER_TYPE:" + user.getUserType().getTypeCode() + " (priority 100)", 
                    userTypePolicies.stream()
                            .filter(EquipmentPolicy::isEnabled)
                            .map(p -> Map.<String, Object>of(
                                    "id", p.getId(),
                                    "name", p.getPolicyName(),
                                    "type", p.getPolicyType().getTypeCode(),
                                    "priority", p.getPriority()
                            ))
                            .collect(Collectors.toList()));
        }

        // 4. GLOBAL policies (priority 0)
        List<EquipmentPolicy> globalPolicies = policyRepository.findByEnabledTrueOrderByPriorityDesc()
                .stream()
                .filter(p -> p.getPolicyName().startsWith("Global"))
                .collect(Collectors.toList());
        policiesBySource.put("GLOBAL (priority 0)", globalPolicies.stream()
                .map(p -> Map.<String, Object>of(
                        "id", p.getId(),
                        "name", p.getPolicyName(),
                        "type", p.getPolicyType().getTypeCode(),
                        "priority", p.getPriority()
                ))
                .collect(Collectors.toList()));

        result.put("policiesBySource", policiesBySource);

        return ResponseEntity.ok(result);
    }

    // Helper class for policy matching
    private static class PolicyMatch {
        String source;
        int priority;
        EquipmentPolicy policy;

        PolicyMatch(String source, int priority, EquipmentPolicy policy) {
            this.source = source;
            this.priority = priority;
            this.policy = policy;
        }
    }
}
