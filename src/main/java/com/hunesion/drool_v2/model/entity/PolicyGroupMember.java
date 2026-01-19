package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * PolicyGroupMember - Links a PolicyGroup to an EquipmentPolicy.
 * Represents which policies belong to a policy group.
 */
@Entity
@Table(name = "policy_group_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"policy_group_id", "policy_id"}))
public class PolicyGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_group_id", nullable = false)
    @JsonIgnore
    private PolicyGroup policyGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private EquipmentPolicy policy;

    public PolicyGroupMember() {
    }

    public PolicyGroupMember(PolicyGroup policyGroup, EquipmentPolicy policy) {
        this.policyGroup = policyGroup;
        this.policy = policy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PolicyGroup getPolicyGroup() {
        return policyGroup;
    }

    public void setPolicyGroup(PolicyGroup policyGroup) {
        this.policyGroup = policyGroup;
    }

    public EquipmentPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(EquipmentPolicy policy) {
        this.policy = policy;
    }
}
