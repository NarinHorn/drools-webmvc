package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.dto.UserGroupDTO;
import com.hunesion.drool_v2.model.entity.User;
import com.hunesion.drool_v2.model.entity.UserGroup;
import com.hunesion.drool_v2.repository.UserGroupRepository;
import com.hunesion.drool_v2.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user-groups")
@Tag(name = "User Group Management", description = "API for managing user groups")
public class UserGroupController {

    private final UserGroupRepository groupRepository;
    private final UserRepository userRepository;

    @Autowired
    public UserGroupController(UserGroupRepository groupRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Get all groups", description = "Retrieves all user groups")
    @GetMapping
    public ResponseEntity<List<UserGroup>> getAllGroups() {
        return ResponseEntity.ok(groupRepository.findAll());
    }

    @Operation(summary = "Get group by ID", description = "Retrieves a specific group by its ID")
    @GetMapping("/{id}")
    public ResponseEntity<UserGroup> getGroupById(@PathVariable Long id) {
        return ResponseEntity.ok(groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found: " + id)));
    }

    @Operation(summary = "Create new group", description = "Creates a new user group")
    @PostMapping
    public ResponseEntity<UserGroup> createGroup(@RequestBody UserGroupDTO dto) {
        if (groupRepository.existsByGroupName(dto.getGroupName())) {
            throw new RuntimeException("Group already exists: " + dto.getGroupName());
        }

        UserGroup group = new UserGroup(dto.getGroupName(), dto.getGroupDescription());
        UserGroup created = groupRepository.save(group);

        // Add members if provided
        if (dto.getMemberIds() != null && !dto.getMemberIds().isEmpty()) {
            addMembersToGroup(created.getId(), dto.getMemberIds());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update group", description = "Updates an existing user group")
    @PutMapping("/{id}")
    public ResponseEntity<UserGroup> updateGroup(
            @PathVariable Long id,
            @RequestBody UserGroupDTO dto) {
        UserGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found: " + id));

        group.setGroupName(dto.getGroupName());
        group.setGroupDescription(dto.getGroupDescription());
        UserGroup updated = groupRepository.save(group);

        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete group", description = "Deletes a user group")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteGroup(@PathVariable Long id) {
        groupRepository.deleteById(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Group deleted successfully");
        response.put("id", id.toString());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get group members", description = "Retrieves all users in a group")
    @GetMapping("/{id}/members")
    public ResponseEntity<List<User>> getGroupMembers(@PathVariable Long id) {
        UserGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found: " + id));
        return ResponseEntity.ok(new ArrayList<>(group.getUsers()));
    }

    @Operation(summary = "Add members to group", description = "Adds users to a group")
    @PostMapping("/{id}/members")
    public ResponseEntity<Map<String, String>> addMembersToGroup(
            @PathVariable Long id,
            @RequestBody Set<Long> userIds) {
        UserGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found: " + id));

        userIds.forEach(userId -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            group.getUsers().add(user);
            user.getGroups().add(group);
        });

        groupRepository.save(group);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Members added successfully");
        response.put("count", String.valueOf(userIds.size()));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove members from group", description = "Removes users from a group")
    @DeleteMapping("/{id}/members")
    public ResponseEntity<Map<String, String>> removeMembersFromGroup(
            @PathVariable Long id,
            @RequestBody Set<Long> userIds) {
        UserGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found: " + id));

        userIds.forEach(userId -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            group.getUsers().remove(user);
            user.getGroups().remove(group);
        });

        groupRepository.save(group);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Members removed successfully");
        response.put("count", String.valueOf(userIds.size()));
        return ResponseEntity.ok(response);
    }
}