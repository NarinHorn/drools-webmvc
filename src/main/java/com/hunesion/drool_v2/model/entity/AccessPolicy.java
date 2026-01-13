package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.HashSet;
import java.util.Set;

import java.time.LocalDateTime;

@Entity
@Table(name = "access_policies")
@JsonPropertyOrder({
        "id",
        "policyName",
        "description",
        "endpoint",
        "httpMethod",
        "allowedRoles",
        "conditions",
        "effect",
        "priority",
        "enabled",
        "generatedDrl",
        "createdAt",
        "updatedAt"
})
public class AccessPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String policyName;

    private String description;

    @Column(nullable = false)
    private String endpoint;

    @Column(nullable = false)
    private String httpMethod;

    @Column(columnDefinition = "TEXT")
    private String allowedRoles;

    // In AccessPolicy.java
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private String conditions;

    @Column(nullable = false)
    private String effect;

    private Integer priority = 0;

    private boolean enabled = true;

    @Column(columnDefinition = "TEXT")
    private String generatedDrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Add this field in AccessPolicy class (after line 65, before @PrePersist)
    @OneToMany(mappedBy = "accessPolicy", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AccessPolicyGroupAssignment> groupAssignments = new HashSet<>();

    // Add getter and setter
    public Set<AccessPolicyGroupAssignment> getGroupAssignments() {
        return groupAssignments;
    }

    public void setGroupAssignments(Set<AccessPolicyGroupAssignment> groupAssignments) {
        this.groupAssignments = groupAssignments;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public AccessPolicy() {
    }

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

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getAllowedRoles() {
        return allowedRoles;
    }

    public void setAllowedRoles(String allowedRoles) {
        this.allowedRoles = allowedRoles;
    }

    @JsonRawValue
    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getGeneratedDrl() {
        return generatedDrl;
    }

    public void setGeneratedDrl(String generatedDrl) {
        this.generatedDrl = generatedDrl;
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

    @Override
    public String toString() {
        return "AccessPolicy{" +
                "id=" + id +
                ", policyName='" + policyName + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                ", effect='" + effect + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
