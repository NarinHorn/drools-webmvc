package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.model.entity.UserType;
import com.hunesion.drool_v2.repository.UserTypeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/user-types")
@Tag(name = "User Type Management", description = "Manage user types (SUPER_ADMIN, MIDDLE_MANAGER, REGULAR_USER, OCCASIONAL_USER)")
public class UserTypeController {

    private final UserTypeRepository userTypeRepository;

    @Autowired
    public UserTypeController(UserTypeRepository userTypeRepository) {
        this.userTypeRepository = userTypeRepository;
    }

    @Operation(summary = "Get all user types", description = "Retrieve all user types")
    @GetMapping
    public ResponseEntity<List<UserType>> getAllUserTypes() {
        List<UserType> userTypes = userTypeRepository.findAll();
        return ResponseEntity.ok(userTypes);
    }

    @Operation(summary = "Get active user types", description = "Retrieve only active user types")
    @GetMapping("/active")
    public ResponseEntity<List<UserType>> getActiveUserTypes() {
        List<UserType> userTypes = userTypeRepository.findByActiveTrue();
        return ResponseEntity.ok(userTypes);
    }

    @Operation(summary = "Get user type by ID", description = "Retrieve a specific user type by ID")
    @GetMapping("/{id}")
    public ResponseEntity<UserType> getUserTypeById(@PathVariable Long id) {
        Optional<UserType> userType = userTypeRepository.findById(id);
        return userType.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get user type by code", description = "Retrieve a user type by type code (e.g., SUPER_ADMIN)")
    @GetMapping("/code/{typeCode}")
    public ResponseEntity<UserType> getUserTypeByCode(@PathVariable String typeCode) {
        Optional<UserType> userType = userTypeRepository.findByTypeCode(typeCode);
        return userType.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
