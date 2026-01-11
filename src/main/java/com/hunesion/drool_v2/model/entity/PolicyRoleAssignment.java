package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "policy_role_assignments")
public class PolicyRoleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    @JsonIgnore // Break circular reference with EquipmentPolicy
    private EquipmentPolicy policy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    public PolicyRoleAssignment() {
    }

    public PolicyRoleAssignment(EquipmentPolicy policy, Role role) {
        this.policy = policy;
        this.role = role;
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}