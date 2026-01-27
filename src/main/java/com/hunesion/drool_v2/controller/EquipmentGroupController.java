package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.dto.EquipmentGroupDTO;
import com.hunesion.drool_v2.model.entity.Equipment;
import com.hunesion.drool_v2.model.entity.EquipmentGroup;
import com.hunesion.drool_v2.repository.EquipmentGroupRepository;
import com.hunesion.drool_v2.repository.EquipmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/equipment-groups")
@Tag(name = "Equipment Group Management", description = "API for managing equipment groups")
public class EquipmentGroupController {

    private final EquipmentGroupRepository groupRepository;
    private final EquipmentRepository equipmentRepository;

    @Autowired
    public EquipmentGroupController(EquipmentGroupRepository groupRepository,
                                    EquipmentRepository equipmentRepository) {
        this.groupRepository = groupRepository;
        this.equipmentRepository = equipmentRepository;
    }

    @Operation(summary = "Get all equipment groups", description = "Retrieves all equipment groups")
    @GetMapping
    public ResponseEntity<List<EquipmentGroupDTO>> getAllGroups() {
        List<EquipmentGroup> groups = groupRepository.findAll();
        List<EquipmentGroupDTO> dtos = groups.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Get equipment group by ID", description = "Retrieves a specific equipment group by ID")
    @GetMapping("/{id}")
    public ResponseEntity<EquipmentGroupDTO> getGroupById(@PathVariable Long id) {
        EquipmentGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment group not found: " + id));
        return ResponseEntity.ok(toDto(group));
    }

    @Operation(summary = "Create new equipment group", description = "Creates a new equipment group")
    @PostMapping
    public ResponseEntity<EquipmentGroupDTO> createGroup(@RequestBody EquipmentGroupDTO dto) {
        if (groupRepository.existsByGroupName(dto.getGroupName())) {
            throw new RuntimeException("Equipment group already exists: " + dto.getGroupName());
        }

        EquipmentGroup group = new EquipmentGroup(dto.getGroupName(), dto.getGroupDescription());
        EquipmentGroup created = groupRepository.save(group);

        if (dto.getEquipmentIds() != null && !dto.getEquipmentIds().isEmpty()) {
            addEquipmentToGroup(created.getId(), dto.getEquipmentIds());
            created = groupRepository.findById(created.getId())
                    .orElseThrow(() -> new RuntimeException("Equipment group not found after create"));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
    }

    @Operation(summary = "Update equipment group", description = "Updates an existing equipment group")
    @PutMapping("/{id}")
    public ResponseEntity<EquipmentGroupDTO> updateGroup(
            @PathVariable Long id,
            @RequestBody EquipmentGroupDTO dto) {

        EquipmentGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment group not found: " + id));

        group.setGroupName(dto.getGroupName());
        group.setGroupDescription(dto.getGroupDescription());
        EquipmentGroup updated = groupRepository.save(group);

        return ResponseEntity.ok(toDto(updated));
    }

    @Operation(summary = "Delete equipment group", description = "Deletes an equipment group and its memberships")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> deleteGroup(@PathVariable Long id) {
        EquipmentGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment group not found: " + id));

        Set<Equipment> members = new HashSet<>(group.getEquipment());
        for (Equipment equipment : members) {
            equipment.getEquipmentGroups().remove(group);
            group.getEquipment().remove(equipment);
        }
        if (!members.isEmpty()) {
            equipmentRepository.saveAll(members);
        }

        groupRepository.delete(group);

        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Equipment group deleted successfully");
        resp.put("id", id.toString());
        resp.put("membersRemoved", String.valueOf(members.size()));
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Get equipment in group", description = "Retrieves all equipment in a given group")
    @GetMapping("/{id}/equipment")
    public ResponseEntity<List<Equipment>> getGroupEquipment(@PathVariable Long id) {
        EquipmentGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment group not found: " + id));
        return ResponseEntity.ok(new ArrayList<>(group.getEquipment()));
    }

    @Operation(summary = "Add equipment to group", description = "Adds equipment items to a group")
    @PostMapping("/{id}/equipment")
    @Transactional
    public ResponseEntity<Map<String, String>> addEquipmentToGroup(
            @PathVariable Long id,
            @RequestBody Set<Long> equipmentIds) {

        EquipmentGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment group not found: " + id));

        equipmentIds.forEach(equipmentId -> {
            Equipment equipment = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new RuntimeException("Equipment not found: " + equipmentId));
            group.getEquipment().add(equipment);
            // Ensure bidirectional update if Equipment has equipmentGroups field
            try {
                equipment.getEquipmentGroups().add(group);
            } catch (Exception ignored) {
                // If equipmentGroups field is not yet added on Equipment, this will be a no-op
            }
        });

        groupRepository.save(group);

        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Equipment added to group successfully");
        resp.put("count", String.valueOf(equipmentIds.size()));
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Remove equipment from group", description = "Removes equipment items from a group")
    @DeleteMapping("/{id}/equipment")
    @Transactional
    public ResponseEntity<Map<String, String>> removeEquipmentFromGroup(
            @PathVariable Long id,
            @RequestBody Set<Long> equipmentIds) {

        EquipmentGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment group not found: " + id));

        equipmentIds.forEach(equipmentId -> {
            Equipment equipment = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new RuntimeException("Equipment not found: " + equipmentId));
            group.getEquipment().remove(equipment);
            try {
                equipment.getEquipmentGroups().remove(group);
            } catch (Exception ignored) {
            }
        });

        groupRepository.save(group);

        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Equipment removed from group successfully");
        resp.put("count", String.valueOf(equipmentIds.size()));
        return ResponseEntity.ok(resp);
    }

    private EquipmentGroupDTO toDto(EquipmentGroup group) {
        EquipmentGroupDTO dto = new EquipmentGroupDTO();
        dto.setId(group.getId());
        dto.setGroupName(group.getGroupName());
        dto.setGroupDescription(group.getGroupDescription());
        dto.setMemberCount(group.getEquipment() != null ? group.getEquipment().size() : 0);
        if (group.getEquipment() != null) {
            dto.setEquipmentIds(
                    group.getEquipment().stream()
                            .map(Equipment::getId)
                            .collect(Collectors.toSet())
            );
        }
        return dto;
    }
}

