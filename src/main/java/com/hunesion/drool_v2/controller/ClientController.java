package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.dto.EquipmentAccessRequestDTO;
import com.hunesion.drool_v2.dto.EquipmentAccessResponseDTO;
import com.hunesion.drool_v2.service.EquipmentAccessControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/clients")
@Tag(name = "Client Action Control", description = "API for client simulator instead of real frontend in NextJS")
public class ClientController {

    private final EquipmentAccessControlService equipmentAccessControlService;

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
}
