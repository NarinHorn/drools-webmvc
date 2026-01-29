package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * PolicyUserTypeAssignment - Links policies to user types.
 * When a policy is assigned to a UserType (e.g., SUPER_ADMIN),
 * all users of that type will have this policy applied.
 */
@Entity
@Table(name = "policy_user_type_assignments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"policy_id", "user_type_id"}))
public class PolicyUserTypeAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    @JsonIgnore
    private EquipmentPolicy policy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_type_id", nullable = false)
    private UserType userType;

    public PolicyUserTypeAssignment() {
    }

    public PolicyUserTypeAssignment(EquipmentPolicy policy, UserType userType) {
        this.policy = policy;
        this.userType = userType;
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

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }
}
