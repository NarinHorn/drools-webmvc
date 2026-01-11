package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "equipment_policies")
@JsonPropertyOrder({
        "id",
        "policyName",
        "description",
        "policyClassification",
        "policyApplication",
        "enabled",
        "priority",
        "createdAt",
        "updatedAt"
})
public class EquipmentPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_name", nullable = false, unique = true)
    private String policyName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "policy_classification", nullable = false)
    private String policyClassification; // 'common', 'temporary', 'basic'

    @Column(name = "policy_application", nullable = false)
    private String policyApplication; // 'apply', 'not_applicable'

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_basic_policy_id")
    private EquipmentPolicy equipmentBasicPolicy;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private Integer priority = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PolicyUserAssignment> userAssignments = new HashSet<>();

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PolicyGroupAssignment> groupAssignments = new HashSet<>();

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PolicyEquipmentAssignment> equipmentAssignments = new HashSet<>();

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PolicyRoleAssignment> roleAssignments = new HashSet<>();

    @OneToOne(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private PolicyCommonSettings commonSettings;

    @OneToOne(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private PolicyAllowedTime allowedTime;

    @OneToOne(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private PolicyLoginControl loginControl;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PolicyCommandSettings> commandSettings = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public EquipmentPolicy() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPolicyClassification() {
        return policyClassification;
    }

    public void setPolicyClassification(String policyClassification) {
        this.policyClassification = policyClassification;
    }

    public String getPolicyApplication() {
        return policyApplication;
    }

    public void setPolicyApplication(String policyApplication) {
        this.policyApplication = policyApplication;
    }

    public EquipmentPolicy getEquipmentBasicPolicy() {
        return equipmentBasicPolicy;
    }

    public void setEquipmentBasicPolicy(EquipmentPolicy equipmentBasicPolicy) {
        this.equipmentBasicPolicy = equipmentBasicPolicy;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
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

    public Set<PolicyUserAssignment> getUserAssignments() {
        return userAssignments;
    }

    public void setUserAssignments(Set<PolicyUserAssignment> userAssignments) {
        this.userAssignments = userAssignments;
    }

    public Set<PolicyGroupAssignment> getGroupAssignments() {
        return groupAssignments;
    }

    public void setGroupAssignments(Set<PolicyGroupAssignment> groupAssignments) {
        this.groupAssignments = groupAssignments;
    }

    public Set<PolicyEquipmentAssignment> getEquipmentAssignments() {
        return equipmentAssignments;
    }

    public void setEquipmentAssignments(Set<PolicyEquipmentAssignment> equipmentAssignments) {
        this.equipmentAssignments = equipmentAssignments;
    }

    public Set<PolicyRoleAssignment> getRoleAssignments() {
        return roleAssignments;
    }

    public void setRoleAssignments(Set<PolicyRoleAssignment> roleAssignments) {
        this.roleAssignments = roleAssignments;
    }

    public PolicyCommonSettings getCommonSettings() {
        return commonSettings;
    }

    public void setCommonSettings(PolicyCommonSettings commonSettings) {
        this.commonSettings = commonSettings;
    }

    public PolicyAllowedTime getAllowedTime() {
        return allowedTime;
    }

    public void setAllowedTime(PolicyAllowedTime allowedTime) {
        this.allowedTime = allowedTime;
    }

    public PolicyLoginControl getLoginControl() {
        return loginControl;
    }

    public void setLoginControl(PolicyLoginControl loginControl) {
        this.loginControl = loginControl;
    }

    public Set<PolicyCommandSettings> getCommandSettings() {
        return commandSettings;
    }

    public void setCommandSettings(Set<PolicyCommandSettings> commandSettings) {
        this.commandSettings = commandSettings;
    }
}