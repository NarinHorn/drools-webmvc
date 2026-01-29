package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * WorkGroup - Groups users, equipment, accounts, and policies together.
 * A work group represents a project/workspace concept where:
 * - Users in the work group can access equipment in the same work group
 * - Policies in the work group's catalog apply to all members
 */
@Entity
@Table(name = "work_groups")
@JsonPropertyOrder({
        "id",
        "workGroupName",
        "description",
        "enabled",
        "createdAt",
        "updatedAt"
})
public class WorkGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_group_name", nullable = false, unique = true)
    private String workGroupName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "last_updated_by")
    private UUID lastUpdatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Users in this work group
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "work_group_users",
            joinColumns = @JoinColumn(name = "work_group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnore
    private Set<User> users = new HashSet<>();

    // Equipment in this work group
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "work_group_equipment",
            joinColumns = @JoinColumn(name = "work_group_id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_id")
    )
    @JsonIgnore
    private Set<Equipment> equipment = new HashSet<>();

    // Accounts in this work group
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "work_group_accounts",
            joinColumns = @JoinColumn(name = "work_group_id"),
            inverseJoinColumns = @JoinColumn(name = "account_id")
    )
    @JsonIgnore
    private Set<Account> accounts = new HashSet<>();

    // Policies in this work group's catalog
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "work_group_policies",
            joinColumns = @JoinColumn(name = "work_group_id"),
            inverseJoinColumns = @JoinColumn(name = "policy_id")
    )
    @JsonIgnore
    private Set<EquipmentPolicy> policies = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public WorkGroup() {
    }

    public WorkGroup(String workGroupName, String description) {
        this.workGroupName = workGroupName;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWorkGroupName() {
        return workGroupName;
    }

    public void setWorkGroupName(String workGroupName) {
        this.workGroupName = workGroupName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(UUID lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public Set<Equipment> getEquipment() {
        return equipment;
    }

    public void setEquipment(Set<Equipment> equipment) {
        this.equipment = equipment;
    }

    public Set<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(Set<Account> accounts) {
        this.accounts = accounts;
    }

    public Set<EquipmentPolicy> getPolicies() {
        return policies;
    }

    public void setPolicies(Set<EquipmentPolicy> policies) {
        this.policies = policies;
    }

    @Override
    public String toString() {
        return "WorkGroup{" +
                "id=" + id +
                ", workGroupName='" + workGroupName + '\'' +
                ", enabled=" + enabled +
                ", userCount=" + (users != null ? users.size() : 0) +
                ", equipmentCount=" + (equipment != null ? equipment.size() : 0) +
                ", policyCount=" + (policies != null ? policies.size() : 0) +
                '}';
    }
}
