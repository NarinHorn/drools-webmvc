package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "policy_login_control")
public class PolicyLoginControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "policy_id", nullable = false, unique = true)
    @JsonIgnore // Break circular reference with EquipmentPolicy
    private EquipmentPolicy policy;

    @Column(name = "ip_filtering_type", nullable = false)
    private String ipFilteringType; // 'allow_specified_ips', 'ip_band_allowed', 'no_restrictions'

    @Column(name = "account_lock_enabled")
    private boolean accountLockEnabled = false;

    @Column(name = "max_failure_attempts")
    private Integer maxFailureAttempts;

    @Column(name = "lockout_duration_minutes")
    private Integer lockoutDurationMinutes;

    @Column(name = "two_factor_type", nullable = false)
    private String twoFactorType; // 'none', 'SMS', 'email', 'admin_approval', 'OTP'

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PolicyAllowedIp> allowedIps = new HashSet<>();

    public PolicyLoginControl() {
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

    public String getIpFilteringType() {
        return ipFilteringType;
    }

    public void setIpFilteringType(String ipFilteringType) {
        this.ipFilteringType = ipFilteringType;
    }

    public boolean isAccountLockEnabled() {
        return accountLockEnabled;
    }

    public void setAccountLockEnabled(boolean accountLockEnabled) {
        this.accountLockEnabled = accountLockEnabled;
    }

    public Integer getMaxFailureAttempts() {
        return maxFailureAttempts;
    }

    public void setMaxFailureAttempts(Integer maxFailureAttempts) {
        this.maxFailureAttempts = maxFailureAttempts;
    }

    public Integer getLockoutDurationMinutes() {
        return lockoutDurationMinutes;
    }

    public void setLockoutDurationMinutes(Integer lockoutDurationMinutes) {
        this.lockoutDurationMinutes = lockoutDurationMinutes;
    }

    public String getTwoFactorType() {
        return twoFactorType;
    }

    public void setTwoFactorType(String twoFactorType) {
        this.twoFactorType = twoFactorType;
    }

    public Set<PolicyAllowedIp> getAllowedIps() {
        return allowedIps;
    }

    public void setAllowedIps(Set<PolicyAllowedIp> allowedIps) {
        this.allowedIps = allowedIps;
    }
}