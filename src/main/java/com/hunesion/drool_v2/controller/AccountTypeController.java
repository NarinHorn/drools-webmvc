package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.model.entity.AccountType;
import com.hunesion.drool_v2.repository.AccountTypeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/account-types")
@Tag(name = "Account Type Management", description = "Manage account types (COLLECTION, PRIVILEGED, PERSONAL, SOLUTION, PUBLIC)")
public class AccountTypeController {

    private final AccountTypeRepository accountTypeRepository;

    @Autowired
    public AccountTypeController(AccountTypeRepository accountTypeRepository) {
        this.accountTypeRepository = accountTypeRepository;
    }

    @Operation(summary = "Get all account types", description = "Retrieve all account types")
    @GetMapping
    public ResponseEntity<List<AccountType>> getAllAccountTypes() {
        List<AccountType> accountTypes = accountTypeRepository.findAll();
        return ResponseEntity.ok(accountTypes);
    }

    @Operation(summary = "Get active account types", description = "Retrieve only active account types")
    @GetMapping("/active")
    public ResponseEntity<List<AccountType>> getActiveAccountTypes() {
        List<AccountType> accountTypes = accountTypeRepository.findByActiveTrue();
        return ResponseEntity.ok(accountTypes);
    }

    @Operation(summary = "Get account type by ID", description = "Retrieve a specific account type by ID")
    @GetMapping("/{id}")
    public ResponseEntity<AccountType> getAccountTypeById(@PathVariable Long id) {
        Optional<AccountType> accountType = accountTypeRepository.findById(id);
        return accountType.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get account type by code", description = "Retrieve an account type by type code (e.g., PRIVILEGED)")
    @GetMapping("/code/{typeCode}")
    public ResponseEntity<AccountType> getAccountTypeByCode(@PathVariable String typeCode) {
        Optional<AccountType> accountType = accountTypeRepository.findByTypeCode(typeCode);
        return accountType.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
