package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * PolicyGroupUserAssignment - Assigns a PolicyGroup to a User.
 * User inherits all policies within the assigned PolicyGroup.
 */
@Entity
@Table(name = "policy_group_user_assignments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"policy_group_id", "user_id"}))
public class PolicyGroupUserAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_group_id", nullable = false)
    @JsonIgnore
    private PolicyGroup policyGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public PolicyGroupUserAssignment() {
    }

    public PolicyGroupUserAssignment(PolicyGroup policyGroup, User user) {
        this.policyGroup = policyGroup;
        this.user = user;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
