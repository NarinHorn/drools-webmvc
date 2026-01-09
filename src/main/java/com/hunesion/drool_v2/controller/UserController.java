package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.entity.Equipment;
import com.hunesion.drool_v2.entity.Role;
import com.hunesion.drool_v2.entity.User;
import com.hunesion.drool_v2.repository.EquipmentRepository;
import com.hunesion.drool_v2.repository.RoleRepository;
import com.hunesion.drool_v2.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UserController - REST API for user management
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EquipmentRepository equipmentRepository;

    @Autowired
    public UserController(UserRepository userRepository, RoleRepository roleRepository, EquipmentRepository equipmentRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.equipmentRepository = equipmentRepository;
    }

    @Operation(
        summary = "Get all users",
        description = "Retrieves a list of all users in the system, including their roles, department, level, and attributes."
    )
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @Operation(
        summary = "Get user by ID",
        description = "Retrieves a specific user by their unique identifier. Returns 404 if the user does not exist."
    )
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Get user by username",
        description = "Retrieves a user by their username. Useful for looking up users by their login name. Returns 404 if the user does not exist."
    )
    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Create a new user",
        description = "Creates a new user account in the system. The username must be unique. Returns 400 Bad Request if the username already exists."
    )
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            return ResponseEntity.badRequest().build();
        }
        User saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(
        summary = "Update user information",
        description = "Updates an existing user's information including email, department, level, active status, and custom attributes. The username cannot be changed. Returns 404 if the user does not exist."
    )
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        return userRepository.findById(id)
                .map(existing -> {
                    existing.setEmail(user.getEmail());
                    existing.setDepartment(user.getDepartment());
                    existing.setLevel(user.getLevel());
                    existing.setActive(user.isActive());
                    existing.setAttributes(user.getAttributes());
                    return ResponseEntity.ok(userRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Add role to user",
        description = "Assigns a role to a user. The role must exist in the system. If the user already has the role, it will not be duplicated."
    )
    @PostMapping("/{userId}/roles/{roleName}")
    public ResponseEntity<User> addRoleToUser(@PathVariable Long userId, 
                                               @PathVariable String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        
        user.addRole(role);
        return ResponseEntity.ok(userRepository.save(user));
    }

    @Operation(
        summary = "Remove role from user",
        description = "Removes a role from a user. The user and role must exist. If the user does not have the role, the operation still succeeds."
    )
    @DeleteMapping("/{userId}/roles/{roleName}")
    public ResponseEntity<User> removeRoleFromUser(@PathVariable Long userId, 
                                                    @PathVariable String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        
        user.removeRole(role);
        return ResponseEntity.ok(userRepository.save(user));
    }

    @Operation(
        summary = "Delete user",
        description = "Permanently deletes a user from the system. This action cannot be undone. All role associations will be removed."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User deleted successfully");
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get all equipment assigned to a user",
        description = "Retrieves all equipment/devices assigned to a specific user. Only returns non-deleted equipment. Returns 404 if the user does not exist."
    )
    @GetMapping("/{userId}/equipment")
    public ResponseEntity<List<Equipment>> getUserEquipment(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    List<Equipment> equipment = user.getEquipment().stream()
                            .filter(eq -> !eq.isDeleted())
                            .toList();
                    return ResponseEntity.ok(equipment);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Assign equipment to user",
        description = "Assigns an equipment/device to a user. The user and equipment must exist and the equipment must not be deleted. If the equipment is already assigned, the operation still succeeds."
    )
    @PostMapping("/{userId}/equipment/{equipmentId}")
    public ResponseEntity<User> assignEquipmentToUser(@PathVariable Long userId, 
                                                      @PathVariable Long equipmentId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .filter(eq -> !eq.isDeleted())
                .orElseThrow(() -> new RuntimeException("Equipment not found or deleted"));
        
        user.addEquipment(equipment);
        return ResponseEntity.ok(userRepository.save(user));
    }

    @Operation(
        summary = "Unassign equipment from user",
        description = "Removes an equipment/device assignment from a user. The user and equipment must exist. If the equipment is not assigned to the user, the operation still succeeds."
    )
    @DeleteMapping("/{userId}/equipment/{equipmentId}")
    public ResponseEntity<User> unassignEquipmentFromUser(@PathVariable Long userId, 
                                                           @PathVariable Long equipmentId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Equipment not found"));
        
        user.removeEquipment(equipment);
        return ResponseEntity.ok(userRepository.save(user));
    }
}
