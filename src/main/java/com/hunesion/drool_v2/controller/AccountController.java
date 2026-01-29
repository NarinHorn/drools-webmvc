package com.hunesion.drool_v2.controller;

import com.hunesion.drool_v2.model.entity.Account;
import com.hunesion.drool_v2.model.entity.AccountType;
import com.hunesion.drool_v2.model.entity.Equipment;
import com.hunesion.drool_v2.repository.AccountRepository;
import com.hunesion.drool_v2.repository.AccountTypeRepository;
import com.hunesion.drool_v2.repository.EquipmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Account Management", description = "Manage accounts for equipment")
public class AccountController {

    private final AccountRepository accountRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final EquipmentRepository equipmentRepository;

    @Autowired
    public AccountController(
            AccountRepository accountRepository,
            AccountTypeRepository accountTypeRepository,
            EquipmentRepository equipmentRepository) {
        this.accountRepository = accountRepository;
        this.accountTypeRepository = accountTypeRepository;
        this.equipmentRepository = equipmentRepository;
    }

    @Operation(summary = "Get all accounts", description = "Retrieve all accounts")
    @GetMapping
    public ResponseEntity<List<Account>> getAllAccounts() {
        List<Account> accounts = accountRepository.findAll();
        return ResponseEntity.ok(accounts);
    }

    @Operation(summary = "Get account by ID", description = "Retrieve a specific account by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccountById(@PathVariable Long id) {
        Optional<Account> account = accountRepository.findById(id);
        return account.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get accounts by equipment ID", description = "Retrieve all accounts for a specific equipment")
    @GetMapping("/equipment/{equipmentId}")
    public ResponseEntity<List<Account>> getAccountsByEquipment(@PathVariable Long equipmentId) {
        List<Account> accounts = accountRepository.findByEquipmentIdAndActiveTrue(equipmentId);
        return ResponseEntity.ok(accounts);
    }

    @Operation(summary = "Get accounts by account type", description = "Retrieve all accounts of a specific type")
    @GetMapping("/type/{accountTypeId}")
    public ResponseEntity<List<Account>> getAccountsByType(@PathVariable Long accountTypeId) {
        List<Account> accounts = accountRepository.findByAccountTypeIdAndActiveTrue(accountTypeId);
        return ResponseEntity.ok(accounts);
    }

    @Operation(summary = "Create new account", description = "Create a new account for an equipment")
    @PostMapping
    public ResponseEntity<?> createAccount(@RequestBody Map<String, Object> request) {
        try {
            String accountName = (String) request.get("accountName");
            Long accountTypeId = Long.valueOf(request.get("accountTypeId").toString());
            Long equipmentId = Long.valueOf(request.get("equipmentId").toString());
            String username = (String) request.get("username");
            String password = (String) request.get("password");
            String description = (String) request.get("description");

            if (accountRepository.existsByAccountNameAndEquipmentId(accountName, equipmentId)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Account with name '" + accountName + "' already exists for this equipment"));
            }

            AccountType accountType = accountTypeRepository.findById(accountTypeId)
                    .orElseThrow(() -> new RuntimeException("Account type not found: " + accountTypeId));

            Equipment equipment = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new RuntimeException("Equipment not found: " + equipmentId));

            Account account = new Account();
            account.setAccountName(accountName);
            account.setAccountType(accountType);
            account.setEquipment(equipment);
            account.setUsername(username);
            account.setPassword(password);
            account.setDescription(description);
            account.setActive(true);

            Account savedAccount = accountRepository.save(account);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedAccount);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Update account", description = "Update an existing account")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAccount(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Account account = accountRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Account not found: " + id));

            if (request.containsKey("accountName")) {
                account.setAccountName((String) request.get("accountName"));
            }
            if (request.containsKey("accountTypeId")) {
                Long accountTypeId = Long.valueOf(request.get("accountTypeId").toString());
                AccountType accountType = accountTypeRepository.findById(accountTypeId)
                        .orElseThrow(() -> new RuntimeException("Account type not found: " + accountTypeId));
                account.setAccountType(accountType);
            }
            if (request.containsKey("username")) {
                account.setUsername((String) request.get("username"));
            }
            if (request.containsKey("password")) {
                account.setPassword((String) request.get("password"));
            }
            if (request.containsKey("description")) {
                account.setDescription((String) request.get("description"));
            }
            if (request.containsKey("active")) {
                account.setActive(Boolean.valueOf(request.get("active").toString()));
            }

            Account updatedAccount = accountRepository.save(account);
            return ResponseEntity.ok(updatedAccount);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Delete account", description = "Soft delete an account (set active to false)")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAccount(@PathVariable Long id) {
        try {
            Account account = accountRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Account not found: " + id));
            account.setActive(false);
            accountRepository.save(account);
            return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
