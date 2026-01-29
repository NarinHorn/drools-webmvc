package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * PolicyAccountTypeAssignment - Links policies to account types.
 * When a policy is assigned to an AccountType (e.g., PRIVILEGED),
 * access to accounts of that type will have this policy applied.
 */
@Entity
@Table(name = "policy_account_type_assignments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"policy_id", "account_type_id"}))
public class PolicyAccountTypeAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    @JsonIgnore
    private EquipmentPolicy policy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_type_id", nullable = false)
    private AccountType accountType;

    public PolicyAccountTypeAssignment() {
    }

    public PolicyAccountTypeAssignment(EquipmentPolicy policy, AccountType accountType) {
        this.policy = policy;
        this.accountType = accountType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EquipmentPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(EquipmentPolicy policy) {
        this.policy = policy;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }
}
