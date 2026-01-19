package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * PolicyGroupUserGroupAssignment - Assigns a PolicyGroup to a UserGroup.
 * All users in the UserGroup inherit all policies within the assigned PolicyGroup.
 */
@Entity
@Table(name = "policy_group_user_group_assignments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"policy_group_id", "user_group_id"}))
public class PolicyGroupUserGroupAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_group_id", nullable = false)
    @JsonIgnore
    private PolicyGroup policyGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_group_id", nullable = false)
    private UserGroup userGroup;

    public PolicyGroupUserGroupAssignment() {
    }

    public PolicyGroupUserGroupAssignment(PolicyGroup policyGroup, UserGroup userGroup) {
        this.policyGroup = policyGroup;
        this.userGroup = userGroup;
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

    public UserGroup getUserGroup() {
        return userGroup;
    }

    public void setUserGroup(UserGroup userGroup) {
        this.userGroup = userGroup;
    }
}
