package com.hunesion.drool_v2.dto;

import java.util.List;

public class PolicyCommonSettingsDTO {
    private Integer servicePort;
    private List<String> allowedProtocols; // ['TELNET', 'FTP', 'SSH', etc.]
    private List<String> allowedDbms; // ['Oracle', 'MySQL', etc.]
    private Integer idleTimeMinutes;
    private Integer timeoutMinutes;
    private String blockingPolicyType; // 'session_blocking', 'more_than_once', 'warning'
    private Integer sessionBlockingCount;
    private Integer maxTelnetSessions;
    private boolean telnetBorderless = false;
    private Integer maxSshSessions;
    private boolean sshBorderless = false;
    private Integer maxRdpSessions;
    private boolean rdpBorderless = false;

    // Getters and Setters
    public Integer getServicePort() {
        return servicePort;
    }

    public void setServicePort(Integer servicePort) {
        this.servicePort = servicePort;
    }

    public List<String> getAllowedProtocols() {
        return allowedProtocols;
    }

    public void setAllowedProtocols(List<String> allowedProtocols) {
        this.allowedProtocols = allowedProtocols;
    }

    public List<String> getAllowedDbms() {
        return allowedDbms;
    }

    public void setAllowedDbms(List<String> allowedDbms) {
        this.allowedDbms = allowedDbms;
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
}