package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "policy_types")
@JsonPropertyOrder({
        "id",
        "typeCode",
        "typeName",
        "description",
        "active",
        "createdAt",
        "updatedAt"
})
public class PolicyType {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "type_code", nullable = false, unique = true, length = 50)
    private String typeCode; // 'commonSettings', 'allowedTime', 'loginControl', 'commandSettings'
    
    @Column(name = "type_name", nullable = false, length = 100)
    private String typeName;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public PolicyType() {
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTypeCode() {
        return typeCode;
    }
    
    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }
    
    public String getTypeName() {
        return typeName;
    }
    
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
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
}
