package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "policy_user_group_assignments")
public class PolicyUserGroupAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    @JsonIgnore // Break circular reference with EquipmentPolicy
    private EquipmentPolicy policy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private UserGroup group;

    public PolicyUserGroupAssignment() {
    }

    public PolicyUserGroupAssignment(EquipmentPolicy policy, UserGroup group) {
        this.policy = policy;
        this.group = group;
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

    public UserGroup getGroup() {
        return group;
    }

    public void setGroup(UserGroup group) {
        this.group = group;
    }
}