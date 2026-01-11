package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "policy_allowed_protocols")
public class PolicyAllowedProtocol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    @JsonIgnore // Break circular reference
    private PolicyCommonSettings policy;

    @Column(nullable = false)
    private String protocol; // 'TELNET', 'FTP', 'SSH', 'SFTP', 'HTTP', 'RDP'

    public PolicyAllowedProtocol() {
    }

    public PolicyAllowedProtocol(PolicyCommonSettings policy, String protocol) {
        this.policy = policy;
        this.protocol = protocol;
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

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}