package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "policy_allowed_ips")
public class PolicyAllowedIp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    @JsonIgnore // Break circular reference with PolicyLoginControl
    private PolicyLoginControl policy;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress; // IPv4 or IPv6

    @Column(name = "ip_range_start")
    private String ipRangeStart;

    @Column(name = "ip_range_end")
    private String ipRangeEnd;

    public PolicyAllowedIp() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PolicyLoginControl getPolicy() {
        return policy;
    }

    public void setPolicy(PolicyLoginControl policy) {
        this.policy = policy;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getIpRangeStart() {
        return ipRangeStart;
    }

    public void setIpRangeStart(String ipRangeStart) {
        this.ipRangeStart = ipRangeStart;
    }

    public String getIpRangeEnd() {
        return ipRangeEnd;
    }

    public void setIpRangeEnd(String ipRangeEnd) {
        this.ipRangeEnd = ipRangeEnd;
    }
}