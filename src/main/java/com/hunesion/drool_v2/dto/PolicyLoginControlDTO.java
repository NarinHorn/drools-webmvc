package com.hunesion.drool_v2.dto;

import java.util.List;

public class PolicyLoginControlDTO {
    private String ipFilteringType; // 'allow_specified_ips', 'ip_band_allowed', 'no_restrictions'
    private List<String> allowedIps; // List of IP addresses or CIDR ranges
    private boolean accountLockEnabled = false;
    private Integer maxFailureAttempts;
    private Integer lockoutDurationMinutes;
    private String twoFactorType; // 'none', 'SMS', 'email', 'admin_approval', 'OTP'

    // Getters and Setters
    public String getIpFilteringType() {
        return ipFilteringType;
    }

    public void setIpFilteringType(String ipFilteringType) {
        this.ipFilteringType = ipFilteringType;
    }

    public List<String> getAllowedIps() {
        return allowedIps;
    }

    public void setAllowedIps(List<String> allowedIps) {
        this.allowedIps = allowedIps;
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
}