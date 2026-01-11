package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "policy_allowed_dbms")
public class PolicyAllowedDbms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    @JsonIgnore // Break circular reference
    private PolicyCommonSettings policy;

    @Column(name = "dbms_type", nullable = false)
    private String dbmsType; // 'Oracle', 'MSSQL', 'MySQL', 'PostgreSQL', etc.

    public PolicyAllowedDbms() {
    }

    public PolicyAllowedDbms(PolicyCommonSettings policy, String dbmsType) {
        this.policy = policy;
        this.dbmsType = dbmsType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PolicyCommonSettings getPolicy() {
        return policy;
    }

    public void setPolicy(PolicyCommonSettings policy) {
        this.policy = policy;
    }

    public String getDbmsType() {
        return dbmsType;
    }

    public void setDbmsType(String dbmsType) {
        this.dbmsType = dbmsType;
    }
}