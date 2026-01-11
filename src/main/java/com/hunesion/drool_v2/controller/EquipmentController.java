package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.model.entity.Equipment;
import com.hunesion.drool_v2.model.entity.User;
import com.hunesion.drool_v2.repository.EquipmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EquipmentController - REST API for equipment/device management
 * Equipment and Device are the same entity
 */
@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentRepository equipmentRepository;

    @Autowired
    public EquipmentController(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    @Operation(
        summary = "Get all equipment",
        description = "Retrieves a list of all non-deleted equipment in the system."
    )
    @GetMapping
    public ResponseEntity<List<Equipment>> getAllEquipment() {
        List<Equipment> equipment = equipmentRepository.findByIsDeletedFalse();
        return ResponseEntity.ok(equipment);
    }

    @Operation(
        summary = "Get equipment by ID",
        description = "Retrieves a specific equipment by its unique identifier. Returns 404 if the equipment does not exist or is deleted."
    )
    @GetMapping("/{id}")
    public ResponseEntity<Equipment> getEquipmentById(@PathVariable Long id) {
        return equipmentRepository.findById(id)
                .filter(equipment -> !equipment.isDeleted())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Get equipment by device name",
        description = "Retrieves equipment by its device name. Returns 404 if not found or deleted."
    )
    @GetMapping("/name/{deviceName}")
    public ResponseEntity<Equipment> getEquipmentByName(@PathVariable String deviceName) {
        return equipmentRepository.findByDeviceNameAndIsDeletedFalse(deviceName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Create a new equipment",
        description = "Creates a new equipment/device in the system. The device name should be unique. Returns 400 Bad Request if the device name already exists."
    )
    @PostMapping
    public ResponseEntity<Equipment> createEquipment(@RequestBody Equipment equipment) {
        // Check if device name already exists (non-deleted)
        if (equipment.getDeviceName() != null && 
            equipmentRepository.existsByDeviceNameAndIsDeletedFalse(equipment.getDeviceName())) {
            return ResponseEntity.badRequest().build();
        }
        
        Equipment saved = equipmentRepository.save(equipment);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(
        summary = "Update equipment information",
        description = "Updates an existing equipment's information. The equipment ID must exist and not be deleted. Returns 404 if the equipment does not exist."
    )
    @PutMapping("/{id}")
    public ResponseEntity<Equipment> updateEquipment(@PathVariable Long id, @RequestBody Equipment equipment) {
        return equipmentRepository.findById(id)
                .filter(existing -> !existing.isDeleted())
                .map(existing -> {
                    // Update fields
                    existing.setDeviceName(equipment.getDeviceName());
                    existing.setHostName(equipment.getHostName());
                    existing.setIpAddress(equipment.getIpAddress());
                    existing.setProtocol(equipment.getProtocol());
                    existing.setPort(equipment.getPort());
                    existing.setUsername(equipment.getUsername());
                    existing.setPassword(equipment.getPassword()); // Password can be updated
                    existing.setDeviceType(equipment.getDeviceType());
                    // Note: isDeleted, createdAt are not updated here
                    return ResponseEntity.ok(equipmentRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Soft delete equipment",
        description = "Performs a soft delete on the equipment by marking it as deleted. The equipment record remains in the database but is hidden from normal queries. Returns 404 if the equipment does not exist."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteEquipment(@PathVariable Long id) {
        return equipmentRepository.findById(id)
                .filter(equipment -> !equipment.isDeleted())
                .map(equipment -> {
                    equipmentRepository.softDelete(id);
                    Map<String, String> response = new HashMap<>();
                    response.put("message", "Equipment soft deleted successfully");
                    response.put("id", id.toString());
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Get equipment by device type",
        description = "Retrieves all equipment of a specific device type (e.g., LINUX_SERVER, DATABASE). Only returns non-deleted equipment."
    )
    @GetMapping("/type/{deviceType}")
    public ResponseEntity<List<Equipment>> getEquipmentByType(@PathVariable String deviceType) {
        List<Equipment> equipment = equipmentRepository.findByDeviceTypeAndIsDeletedFalse(deviceType);
        return ResponseEntity.ok(equipment);
    }

    @Operation(
        summary = "Get all users assigned to equipment",
        description = "Retrieves all users assigned to a specific equipment/device. Only returns active users. Returns 404 if the equipment does not exist or is deleted."
    )
    @GetMapping("/{equipmentId}/users")
    public ResponseEntity<List<User>> getEquipmentUsers(@PathVariable Long equipmentId) {
        return equipmentRepository.findById(equipmentId)
                .filter(equipment -> !equipment.isDeleted())
                .map(equipment -> {
                    List<User> users = equipment.getUsers().stream()
                            .filter(User::isActive)
                            .toList();
                    return ResponseEntity.ok(users);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
