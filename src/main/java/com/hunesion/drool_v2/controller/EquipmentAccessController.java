package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.dto.EquipmentAccessRequestDTO;
import com.hunesion.drool_v2.dto.EquipmentAccessResponseDTO;
import com.hunesion.drool_v2.service.EquipmentAccessControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/equipment-access")
@Tag(name = "Equipment Access Control", description = "API for checking equipment access using Drools")
public class EquipmentAccessController {

    private final EquipmentAccessControlService accessControlService;

    @Autowired
    public EquipmentAccessController(EquipmentAccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @Operation(
            summary = "User ssh on their equipment (Linux Server) that has been assigned by admin",
            description = "Evaluates if a user has SSH access to a specific equipment based on equipment policies and assignments"
    )
    @GetMapping("/ssh")
    public ResponseEntity<EquipmentAccessResponseDTO> sshToTargetEquipment(
            @RequestParam("username") String username,
            @RequestParam("equipmentId") Long equipmentId,
            @RequestParam(value = "clientIp", required = false) String clientIp
    ) {
        // Build minimal SSH access request DTO
        EquipmentAccessRequestDTO request = new EquipmentAccessRequestDTO();
        request.setUsername(username);
        request.setEquipmentId(equipmentId);
        request.setProtocol("SSH");
        request.setClientIp(clientIp);

        EquipmentAccessResponseDTO response = accessControlService.checkAccess(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Check equipment access",
            description = "Evaluates if a user has access to equipment based on policies"
    )
    @PostMapping("/check")
    public ResponseEntity<EquipmentAccessResponseDTO> checkAccess(
            @RequestBody EquipmentAccessRequestDTO request) {
        EquipmentAccessResponseDTO response = accessControlService.checkAccess(request);
        return ResponseEntity.ok(response);
    }
}