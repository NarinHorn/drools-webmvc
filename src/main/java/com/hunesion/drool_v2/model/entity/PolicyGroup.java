package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * PolicyGroup - Groups multiple EquipmentPolicies together for easier assignment.
 * Instead of assigning 10 individual policies to a user, assign 1 PolicyGroup.
 */
@Entity
@Table(name = "policy_groups")
@JsonPropertyOrder({
        "id",
        "groupName",
        "description",
        "enabled",
        "createdAt",
        "updatedAt"
})
public class PolicyGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_name", nullable = false, unique = true)
    private String groupName;

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

    // Policies in this group
    @OneToMany(mappedBy = "policyGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PolicyGroupMember> policyMembers = new HashSet<>();

    // Assignments to users
    @OneToMany(mappedBy = "policyGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PolicyGroupUserAssignment> userAssignments = new HashSet<>();

    // Assignments to user groups
    @OneToMany(mappedBy = "policyGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PolicyGroupUserGroupAssignment> userGroupAssignments = new HashSet<>();

    // Assignments to roles
    @OneToMany(mappedBy = "policyGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PolicyGroupRoleAssignment> roleAssignments = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public PolicyGroup() {
    }

    public PolicyGroup(String groupName, String description) {
        this.groupName = groupName;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
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

    public Set<PolicyGroupMember> getPolicyMembers() {
        return policyMembers;
    }

    public void setPolicyMembers(Set<PolicyGroupMember> policyMembers) {
        this.policyMembers = policyMembers;
    }

    public Set<PolicyGroupUserAssignment> getUserAssignments() {
        return userAssignments;
    }

    public void setUserAssignments(Set<PolicyGroupUserAssignment> userAssignments) {
        this.userAssignments = userAssignments;
    }

    public Set<PolicyGroupUserGroupAssignment> getUserGroupAssignments() {
        return userGroupAssignments;
    }

    public void setUserGroupAssignments(Set<PolicyGroupUserGroupAssignment> userGroupAssignments) {
        this.userGroupAssignments = userGroupAssignments;
    }

    public Set<PolicyGroupRoleAssignment> getRoleAssignments() {
        return roleAssignments;
    }

    public void setRoleAssignments(Set<PolicyGroupRoleAssignment> roleAssignments) {
        this.roleAssignments = roleAssignments;
    }

    @Override
    public String toString() {
        return String.format(
                "%nPolicyGroup{%n  id=%d%n  groupName='%s'%n  enabled=%s%n  memberCount=%d%n}",
                id,
                groupName,
                enabled,
                policyMembers != null ? policyMembers.size() : 0
        );
    }
}
