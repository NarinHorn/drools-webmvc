package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.dto.WorkGroupDTO;
import com.hunesion.drool_v2.model.entity.*;
import com.hunesion.drool_v2.repository.*;
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
@RequestMapping("/api/work-groups")
@Tag(name = "Work Group Management", description = "Manage work groups (users, equipment, accounts, policies)")
public class WorkGroupController {

    private final WorkGroupRepository workGroupRepository;
    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final AccountRepository accountRepository;
    private final EquipmentPolicyRepository policyRepository;

    @Autowired
    public WorkGroupController(
            WorkGroupRepository workGroupRepository,
            UserRepository userRepository,
            EquipmentRepository equipmentRepository,
            AccountRepository accountRepository,
            EquipmentPolicyRepository policyRepository) {
        this.workGroupRepository = workGroupRepository;
        this.userRepository = userRepository;
        this.equipmentRepository = equipmentRepository;
        this.accountRepository = accountRepository;
        this.policyRepository = policyRepository;
    }

    // ========== CRUD ==========

    @Operation(summary = "Get all work groups", description = "Retrieve all work groups")
    @GetMapping
    public ResponseEntity<List<WorkGroupDTO>> getAllWorkGroups() {
        List<WorkGroup> workGroups = workGroupRepository.findAll();
        List<WorkGroupDTO> dtos = workGroups.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Get active work groups", description = "Retrieve only active work groups")
    @GetMapping("/active")
    public ResponseEntity<List<WorkGroupDTO>> getActiveWorkGroups() {
        List<WorkGroup> workGroups = workGroupRepository.findByEnabledTrue();
        List<WorkGroupDTO> dtos = workGroups.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Get work group by ID", description = "Retrieve a specific work group by ID")
    @GetMapping("/{id}")
    public ResponseEntity<WorkGroupDTO> getWorkGroupById(@PathVariable Long id) {
        WorkGroup workGroup = workGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work group not found: " + id));
        return ResponseEntity.ok(toDto(workGroup));
    }

    @Operation(summary = "Create work group", description = "Create a new work group")
    @PostMapping
    @Transactional
    public ResponseEntity<WorkGroupDTO> createWorkGroup(@RequestBody WorkGroupDTO dto) {
        if (workGroupRepository.existsByWorkGroupName(dto.getWorkGroupName())) {
            throw new RuntimeException("Work group already exists: " + dto.getWorkGroupName());
        }

        WorkGroup workGroup = new WorkGroup(dto.getWorkGroupName(), dto.getDescription());
        workGroup.setEnabled(dto.isEnabled());
        WorkGroup saved = workGroupRepository.save(workGroup);

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @Operation(summary = "Update work group", description = "Update an existing work group")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<WorkGroupDTO> updateWorkGroup(@PathVariable Long id, @RequestBody WorkGroupDTO dto) {
        WorkGroup workGroup = workGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work group not found: " + id));

        workGroup.setWorkGroupName(dto.getWorkGroupName());
        workGroup.setDescription(dto.getDescription());
        workGroup.setEnabled(dto.isEnabled());
        WorkGroup saved = workGroupRepository.save(workGroup);

        return ResponseEntity.ok(toDto(saved));
    }

    @Operation(summary = "Delete work group", description = "Delete a work group")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> deleteWorkGroup(@PathVariable Long id) {
        WorkGroup workGroup = workGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work group not found: " + id));

        workGroupRepository.delete(workGroup);

        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Work group deleted successfully");
        resp.put("id", id.toString());
        return ResponseEntity.ok(resp);
    }

    // ========== USER MEMBERSHIP ==========

    @Operation(summary = "Get users in work group", description = "Retrieve all users in a work group")
    @GetMapping("/{id}/users")
    public ResponseEntity<List<User>> getWorkGroupUsers(@PathVariable Long id) {
        WorkGroup workGroup = workGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work group not found: " + id));
        return ResponseEntity.ok(new ArrayList<>(workGroup.getUsers()));
    }

    @Operation(summary = "Add users to work group", description = "Add users to a work group")
    @PostMapping("/{id}/users")
    @Transactional
    public ResponseEntity<Map<String, String>> addUsersToWorkGroup(
            @PathVariable Long id,
            @RequestBody Set<Long> userIds) {

        WorkGroup workGroup = workGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work group not found: " + id));

        userIds.forEach(userId -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            workGroup.getUsers().add(user);
        });

        workGroupRepository.save(workGroup);

        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Users added to work group successfully");
        resp.put("count", String.valueOf(userIds.size()));
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Remove users from work group", description = "Remove users from a work group")
    @DeleteMapping("/{id}/users")
    @Transactional
    public ResponseEntity<Map<String, String>> removeUsersFromWorkGroup(
            @PathVariable Long id,
            @RequestBody Set<Long> userIds) {

        WorkGroup workGroup = workGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work group not found: " + id));

        workGroup.getUsers().removeIf(user -> userIds.contains(user.getId()));
        workGroupRepository.save(workGroup);

        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Users removed from work group successfully");
        resp.put("count", String.valueOf(userIds.size()));
        return ResponseEntity.ok(resp);
    }

    // ========== EQUIPMENT MEMBERSHIP ==========

    @Operation(summary = "Get equipment in work group", description = "Retrieve all equipment in a work group")
    @GetMapping("/{id}/equipment")
    public ResponseEntity<List<Equipment>> getWorkGroupEquipment(@PathVariable Long id) {
        WorkGroup workGroup = workGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work group not found: " + id));
        return ResponseEntity.ok(new ArrayList<>(workGroup.getEquipment()));
    }

    @Operation(summary = "Add equipment to work group", description = "Add equipment to a work group")
    @PostMapping("/{id}/equipment")
    @Transactional
    public ResponseEntity<Map<String, String>> addEquipmentToWorkGroup(
            @PathVariable Long id,
            @RequestBody Set<Long> equipmentIds) {

        WorkGroup workGroup = workGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work group not found: " + id));

        equipmentIds.forEach(equipmentId -> {
            Equipment equipment = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new RuntimeException("Equipment not found: " + equipmentId));
            workGroup.getEquipment().add(equipment);
        });

        workGroupRepository.save(workGroup);

        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Equipment added to work group successfully");
        resp.put("count", String.valueOf(equipmentIds.size()));
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Remove equipment from work group", description = "Remove equipment from a work group")
    @DeleteMapping("/{id}/equipment")
    @Transactional
    public ResponseEntity<Map<String, String>> removeEquipmentFromWorkGroup(
            @PathVariable Long id,
            @RequestBody Set<Long> equipmentIds) {

        WorkGroup workGroup = workGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work group not found: " + id));

        workGroup.getEquipment().removeIf(equipment -> equipmentIds.contains(equipment.getId()));
        workGroupRepository.save(workGroup);

        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Equipment removed from work group successfully");
        resp.put("count", String.valueOf(equipmentIds.size()));
        return ResponseEntity.ok(resp);
    }

    // ========== ACCOUNT MEMBERSHIP ==========

    @Operation(summary = "Get accounts in work group", description = "Retrieve all accounts in a work group")
    @GetMapping("/{id}/accounts")
    public ResponseEntity<List<Account>> getWorkGroupAccounts(@PathVariable Long id) {
        WorkGroup workGroup = workGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work group not found: " + id));
        return ResponseEntity.ok(new ArrayList<>(workGroup.getAccounts()));
    }

    @Operation(summary = "Add accounts to work group", description = "Add accounts to a work group")
    @PostMapping("/{id}/accounts")
    @Transactional
    public ResponseEntity<Map<String, String>> addAccountsToWorkGroup(
            @PathVariable Long id,
            @RequestBody Set<Long> accountIds) {

        WorkGroup workGroup = workGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work group not found: " + id));

        accountIds.forEach(accountId -> {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
            workGroup.getAccounts().add(account);
        });

        workGroupRepository.save(workGroup);

        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Accounts added to work group successfully");
        resp.put("count", String.valueOf(accountIds.size()));
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Remove accounts from work group", description = "Remove accounts from a work group")
    @DeleteMapping("/{id}/accounts")
    @Transactional
    public ResponseEntity<Map<String, String>> removeAccountsFromWorkGroup(
            @PathVariable Long id,
            @RequestBody Set<Long> accountIds) {

        WorkGroup workGroup = workGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work group not found: " + id));

        workGroup.getAccounts().removeIf(account -> accountIds.contains(account.getId()));
        workGroupRepository.save(workGroup);

        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Accounts removed from work group successfully");
        resp.put("count", String.valueOf(accountIds.size()));
        return ResponseEntity.ok(resp);
    }

    // ========== POLICY CATALOG ==========

    @Operation(summary = "Get policies in work group", description = "Retrieve all policies in a work group's catalog")
    @GetMapping("/{id}/policies")
    public ResponseEntity<List<EquipmentPolicy>> getWorkGroupPolicies(@PathVariable Long id) {
        WorkGroup workGroup = workGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work group not found: " + id));
        return ResponseEntity.ok(new ArrayList<>(workGroup.getPolicies()));
    }

    @Operation(summary = "Add policies to work group", description = "Add policies to a work group's catalog")
    @PostMapping("/{id}/policies")
    @Transactional
    public ResponseEntity<Map<String, String>> addPoliciesToWorkGroup(
            @PathVariable Long id,
            @RequestBody Set<Long> policyIds) {

        WorkGroup workGroup = workGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work group not found: " + id));

        policyIds.forEach(policyId -> {
            EquipmentPolicy policy = policyRepository.findById(policyId)
                    .orElseThrow(() -> new RuntimeException("Policy not found: " + policyId));
            workGroup.getPolicies().add(policy);
        });

        workGroupRepository.save(workGroup);

        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Policies added to work group successfully");
        resp.put("count", String.valueOf(policyIds.size()));
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Remove policies from work group", description = "Remove policies from a work group's catalog")
    @DeleteMapping("/{id}/policies")
    @Transactional
    public ResponseEntity<Map<String, String>> removePoliciesFromWorkGroup(
            @PathVariable Long id,
            @RequestBody Set<Long> policyIds) {

        WorkGroup workGroup = workGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work group not found: " + id));

        workGroup.getPolicies().removeIf(policy -> policyIds.contains(policy.getId()));
        workGroupRepository.save(workGroup);

        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Policies removed from work group successfully");
        resp.put("count", String.valueOf(policyIds.size()));
        return ResponseEntity.ok(resp);
    }

    // ========== HELPER ==========

    private WorkGroupDTO toDto(WorkGroup workGroup) {
        WorkGroupDTO dto = new WorkGroupDTO();
        dto.setId(workGroup.getId());
        dto.setWorkGroupName(workGroup.getWorkGroupName());
        dto.setDescription(workGroup.getDescription());
        dto.setEnabled(workGroup.isEnabled());
        dto.setUserCount(workGroup.getUsers() != null ? workGroup.getUsers().size() : 0);
        dto.setEquipmentCount(workGroup.getEquipment() != null ? workGroup.getEquipment().size() : 0);
        dto.setAccountCount(workGroup.getAccounts() != null ? workGroup.getAccounts().size() : 0);
        dto.setPolicyCount(workGroup.getPolicies() != null ? workGroup.getPolicies().size() : 0);

        if (workGroup.getUsers() != null) {
            dto.setUserIds(workGroup.getUsers().stream().map(User::getId).collect(Collectors.toSet()));
        }
        if (workGroup.getEquipment() != null) {
            dto.setEquipmentIds(workGroup.getEquipment().stream().map(Equipment::getId).collect(Collectors.toSet()));
        }
        if (workGroup.getAccounts() != null) {
            dto.setAccountIds(workGroup.getAccounts().stream().map(Account::getId).collect(Collectors.toSet()));
        }
        if (workGroup.getPolicies() != null) {
            dto.setPolicyIds(workGroup.getPolicies().stream().map(EquipmentPolicy::getId).collect(Collectors.toSet()));
        }

        return dto;
    }
}
