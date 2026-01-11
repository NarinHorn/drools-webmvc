package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.model.entity.Role;
import com.hunesion.drool_v2.repository.RoleRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RoleController - REST API for role management
 */
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleRepository roleRepository;

    @Autowired
    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Operation(
        summary = "Get all roles",
        description = "Retrieves a list of all roles defined in the system, including their descriptions."
    )
    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleRepository.findAll());
    }

    @Operation(
        summary = "Get role by ID",
        description = "Retrieves a specific role by its unique identifier. Returns 404 if the role does not exist."
    )
    @GetMapping("/{id}")
    public ResponseEntity<Role> getRoleById(@PathVariable Long id) {
        return roleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Get role by name",
        description = "Retrieves a role by its name. Useful for looking up roles by their exact name. Returns 404 if the role does not exist."
    )
    @GetMapping("/name/{name}")
    public ResponseEntity<Role> getRoleByName(@PathVariable String name) {
        return roleRepository.findByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Create a new role",
        description = "Creates a new role in the system. The role name must be unique. Returns 400 Bad Request if the role name already exists."
    )
    @PostMapping
    public ResponseEntity<Role> createRole(@RequestBody Role role) {
        if (roleRepository.existsByName(role.getName())) {
            return ResponseEntity.badRequest().build();
        }
        Role saved = roleRepository.save(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(
        summary = "Update role description",
        description = "Updates the description of an existing role. Only the description can be modified. The role name cannot be changed. Returns 404 if the role does not exist."
    )
    @PutMapping("/{id}")
    public ResponseEntity<Role> updateRole(@PathVariable Long id, @RequestBody Role role) {
        return roleRepository.findById(id)
                .map(existing -> {
                    existing.setDescription(role.getDescription());
                    return ResponseEntity.ok(roleRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Delete role",
        description = "Permanently deletes a role from the system. This action cannot be undone. Note: Users who had this role will no longer have it assigned."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteRole(@PathVariable Long id) {
        roleRepository.deleteById(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Role deleted successfully");
        return ResponseEntity.ok(response);
    }
}
