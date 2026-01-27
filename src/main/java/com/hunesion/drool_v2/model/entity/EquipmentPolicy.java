package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

    // Policy Type: Each policy handles only one type of configuration
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "policy_type_id", nullable = false)
    private PolicyType policyType;

    // JSONB field for policy configuration (contains ONLY the section for this policy type)
    // For 'commonSettings' type: contains only commonSettings object
    // For 'allowedTime' type: contains only allowedTime object
    // For 'loginControl' type: contains only loginControl object
    // For 'commandSettings' type: contains only commandSettings array
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB", name = "policy_config")
    private String policyConfig;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private Integer priority = 0;

    // Versioning & Audit
    @Column(name = "version_no")
    private Integer versionNo = 1;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "last_updated_by")
    private UUID lastUpdatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PolicyUserAssignment> userAssignments = new HashSet<>();

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PolicyUserGroupAssignment> groupAssignments = new HashSet<>();

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PolicyEquipmentAssignment> equipmentAssignments = new HashSet<>();

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PolicyRoleAssignment> roleAssignments = new HashSet<>();

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

    public PolicyType getPolicyType() {
        return policyType;
    }

    public void setPolicyType(PolicyType policyType) {
        this.policyType = policyType;
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

    @JsonRawValue
    public String getPolicyConfig() {
        return policyConfig;
    }

    public void setPolicyConfig(String policyConfig) {
        this.policyConfig = policyConfig;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
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

    public Set<PolicyUserAssignment> getUserAssignments() {
        return userAssignments;
    }

    public void setUserAssignments(Set<PolicyUserAssignment> userAssignments) {
        this.userAssignments = userAssignments;
    }

    public Set<PolicyUserGroupAssignment> getGroupAssignments() {
        return groupAssignments;
    }

    public void setGroupAssignments(Set<PolicyUserGroupAssignment> groupAssignments) {
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

    /**
     * Helper method to get parsed config as Map
     * Note: This should use PolicyConfigCache for better performance
     */
    public Map<String, Object> getPolicyConfigAsMap() {
        if (policyConfig == null || policyConfig.isEmpty()) {
            return new HashMap<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(policyConfig, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse policy config for policy: " + id, e);
        }
    }

    @Override
    public String toString() {
        return String.format(
                "%nPolicy{%n  id=%d%n  policyName='%s'%n  enabled=%s%n  priority=%d%n}",
                id,
                policyName,
                enabled,
                priority
        );
    }
}