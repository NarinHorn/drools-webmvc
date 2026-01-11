package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "policy_common_settings")
public class PolicyCommonSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "policy_id", nullable = false, unique = true)
    @JsonIgnore // Break circular reference with EquipmentPolicy
    private EquipmentPolicy policy;

    @Column(name = "service_port")
    private Integer servicePort;

    @Column(name = "idle_time_minutes")
    private Integer idleTimeMinutes;

    @Column(name = "timeout_minutes")
    private Integer timeoutMinutes;

    @Column(name = "blocking_policy_type")
    private String blockingPolicyType; // 'session_blocking', 'more_than_once', 'warning'

    @Column(name = "session_blocking_count")
    private Integer sessionBlockingCount;

    @Column(name = "max_telnet_sessions")
    private Integer maxTelnetSessions;

    @Column(name = "telnet_borderless")
    private boolean telnetBorderless = false;

    @Column(name = "max_ssh_sessions")
    private Integer maxSshSessions;

    @Column(name = "ssh_borderless")
    private boolean sshBorderless = false;

    @Column(name = "max_rdp_sessions")
    private Integer maxRdpSessions;

    @Column(name = "rdp_borderless")
    private boolean rdpBorderless = false;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PolicyAllowedProtocol> allowedProtocols = new HashSet<>();

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PolicyAllowedDbms> allowedDbms = new HashSet<>();

    public PolicyCommonSettings() {
    }

    // Getters and Setters
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

    public Integer getServicePort() {
        return servicePort;
    }

    public void setServicePort(Integer servicePort) {
        this.servicePort = servicePort;
    }

    public Integer getIdleTimeMinutes() {
        return idleTimeMinutes;
    }

    public void setIdleTimeMinutes(Integer idleTimeMinutes) {
        this.idleTimeMinutes = idleTimeMinutes;
    }

    public Integer getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public void setTimeoutMinutes(Integer timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    public String getBlockingPolicyType() {
        return blockingPolicyType;
    }

    public void setBlockingPolicyType(String blockingPolicyType) {
        this.blockingPolicyType = blockingPolicyType;
    }

    public Integer getSessionBlockingCount() {
        return sessionBlockingCount;
    }

    public void setSessionBlockingCount(Integer sessionBlockingCount) {
        this.sessionBlockingCount = sessionBlockingCount;
    }

    public Integer getMaxTelnetSessions() {
        return maxTelnetSessions;
    }

    public void setMaxTelnetSessions(Integer maxTelnetSessions) {
        this.maxTelnetSessions = maxTelnetSessions;
    }

    public boolean isTelnetBorderless() {
        return telnetBorderless;
    }

    public void setTelnetBorderless(boolean telnetBorderless) {
        this.telnetBorderless = telnetBorderless;
    }

    public Integer getMaxSshSessions() {
        return maxSshSessions;
    }

    public void setMaxSshSessions(Integer maxSshSessions) {
        this.maxSshSessions = maxSshSessions;
    }

    public boolean isSshBorderless() {
        return sshBorderless;
    }

    public void setSshBorderless(boolean sshBorderless) {
        this.sshBorderless = sshBorderless;
    }

    public Integer getMaxRdpSessions() {
        return maxRdpSessions;
    }

    public void setMaxRdpSessions(Integer maxRdpSessions) {
        this.maxRdpSessions = maxRdpSessions;
    }

    public boolean isRdpBorderless() {
        return rdpBorderless;
    }

    public void setRdpBorderless(boolean rdpBorderless) {
        this.rdpBorderless = rdpBorderless;
    }

    public Set<PolicyAllowedProtocol> getAllowedProtocols() {
        return allowedProtocols;
    }

    public void setAllowedProtocols(Set<PolicyAllowedProtocol> allowedProtocols) {
        this.allowedProtocols = allowedProtocols;
    }

    public Set<PolicyAllowedDbms> getAllowedDbms() {
        return allowedDbms;
    }

    public void setAllowedDbms(Set<PolicyAllowedDbms> allowedDbms) {
        this.allowedDbms = allowedDbms;
    }
}