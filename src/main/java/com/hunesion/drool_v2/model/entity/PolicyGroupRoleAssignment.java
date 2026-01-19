package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * PolicyGroupRoleAssignment - Assigns a PolicyGroup to a Role.
 * All users with the Role inherit all policies within the assigned PolicyGroup.
 */
@Entity
@Table(name = "policy_group_role_assignments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"policy_group_id", "role_id"}))
public class PolicyGroupRoleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_group_id", nullable = false)
    @JsonIgnore
    private PolicyGroup policyGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    public PolicyGroupRoleAssignment() {
    }

    public PolicyGroupRoleAssignment(PolicyGroup policyGroup, Role role) {
        this.policyGroup = policyGroup;
        this.role = role;
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
