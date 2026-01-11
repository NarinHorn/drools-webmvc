package com.hunesion.drool_v2.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * EquipmentAccessRequest - Drools fact for equipment policy evaluation
 * Contains all context needed for equipment access policy evaluation
 */
public class EquipmentAccessRequest {

    private String username;
    private Set<String> userRoles = new HashSet<>();
    private Set<String> userGroups = new HashSet<>();
    private Long userId;
    private Long equipmentId;
    private String equipmentName;
    private String protocol; // 'TELNET', 'FTP', 'SSH', 'SFTP', 'HTTP', 'RDP'
    private String dbmsType; // 'Oracle', 'MSSQL', 'MySQL', 'PostgreSQL', etc.
    private String command; // Command being executed
    private LocalDateTime requestTime;
    private String clientIp;
    private Integer currentHour; // 0-23
    private Integer currentDayOfWeek; // 1=Monday, 7=Sunday
    private Integer currentSshSessions;
    private Integer currentRdpSessions;
    private Integer currentTelnetSessions;

    // Policy assignment flags (set by PolicyFactLoader)
    private Set<Long> assignedPolicyIds = new HashSet<>();

    // Protocol/DBMS flags (set by PolicyFactLoader)
    private Set<String> allowedProtocols = new HashSet<>();
    private Set<String> allowedDbms = new HashSet<>();

    // Command lists (set by PolicyFactLoader)
    private Set<String> blacklistedCommands = new HashSet<>();
    private Set<String> whitelistedCommands = new HashSet<>();

    // Time slots (set by PolicyFactLoader)
    private Set<TimeSlot> allowedTimeSlots = new HashSet<>();

    // IP filtering (set by PolicyFactLoader)
    private Set<String> allowedIps = new HashSet<>();
    private String ipFilteringType; // 'allow_specified_ips', 'ip_band_allowed', 'no_restrictions'

    // Helper class for time slots
    public static class TimeSlot {
        private Integer dayOfWeek;
        private Integer hourStart;
        private Integer hourEnd;

        public TimeSlot(Integer dayOfWeek, Integer hourStart, Integer hourEnd) {
            this.dayOfWeek = dayOfWeek;
            this.hourStart = hourStart;
            this.hourEnd = hourEnd;
        }

        public boolean isWithinTime(int day, int hour) {
            return dayOfWeek.equals(day) && hour >= hourStart && hour <= hourEnd;
        }

        // Getters
        public Integer getDayOfWeek() { return dayOfWeek; }
        public Integer getHourStart() { return hourStart; }
        public Integer getHourEnd() { return hourEnd; }
    }

    public EquipmentAccessRequest() {
        this.requestTime = LocalDateTime.now();
    }

    // Helper methods for Drools (makes rules simpler)
    public boolean hasProtocol(String protocol) {
        return this.protocol != null && this.protocol.equalsIgnoreCase(protocol);
    }

    public boolean hasDbmsType(String dbmsType) {
        return this.dbmsType != null && this.dbmsType.equalsIgnoreCase(dbmsType);
    }

    public boolean isInGroup(String groupName) {
        return userGroups != null && userGroups.contains(groupName);
    }

    public boolean hasRole(String roleName) {
        return userRoles != null && userRoles.contains(roleName);
    }

    public boolean isAssignedToPolicy(Long policyId) {
        return assignedPolicyIds != null && assignedPolicyIds.contains(policyId);
    }

    public boolean isProtocolAllowed(String protocol) {
        return allowedProtocols != null && allowedProtocols.contains(protocol);
    }

    public boolean isDbmsAllowed(String dbmsType) {
        return allowedDbms != null && allowedDbms.contains(dbmsType);
    }

    public boolean isWithinAllowedTime() {
        if (allowedTimeSlots == null || allowedTimeSlots.isEmpty()) {
            return true; // No time restrictions
        }
        if (currentDayOfWeek == null || currentHour == null) {
            return false;
        }
        return allowedTimeSlots.stream()
                .anyMatch(slot -> slot.isWithinTime(currentDayOfWeek, currentHour));
    }

    public boolean isCommandBlocked(String command) {
        if (command == null || command.isEmpty()) {
            return false;
        }
        // Check blacklist
        if (blacklistedCommands != null) {
            for (String blacklisted : blacklistedCommands) {
                if (command.contains(blacklisted) || command.equals(blacklisted)) {
                    return true;
                }
            }
        }
        // Check whitelist (if whitelist exists and command not in it, block)
        if (whitelistedCommands != null && !whitelistedCommands.isEmpty()) {
            return !whitelistedCommands.stream()
                    .anyMatch(whitelisted -> command.contains(whitelisted) || command.equals(whitelisted));
        }
        return false;
    }

    public boolean isIpAllowed(String ip) {
        if (ipFilteringType == null || "no_restrictions".equals(ipFilteringType)) {
            return true;
        }
        if (allowedIps == null || allowedIps.isEmpty()) {
            return false;
        }
        return allowedIps.contains(ip);
    }

    public boolean exceedsMaxSessions(String protocol, Integer maxSessions) {
        if (maxSessions == null) {
            return false; // No limit
        }
        switch (protocol.toUpperCase()) {
            case "SSH":
                return currentSshSessions != null && currentSshSessions >= maxSessions;
            case "RDP":
                return currentRdpSessions != null && currentRdpSessions >= maxSessions;
            case "TELNET":
                return currentTelnetSessions != null && currentTelnetSessions >= maxSessions;
            default:
                return false;
        }
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Set<String> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(Set<String> userRoles) {
        this.userRoles = userRoles;
    }

    public Set<String> getUserGroups() {
        return userGroups;
    }

    public void setUserGroups(Set<String> userGroups) {
        this.userGroups = userGroups;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getDbmsType() {
        return dbmsType;
    }

    public void setDbmsType(String dbmsType) {
        this.dbmsType = dbmsType;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
        if (requestTime != null) {
            this.currentHour = requestTime.getHour();
            this.currentDayOfWeek = requestTime.getDayOfWeek().getValue();
        }
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public Integer getCurrentHour() {
        return currentHour;
    }

    public void setCurrentHour(Integer currentHour) {
        this.currentHour = currentHour;
    }

    public Integer getCurrentDayOfWeek() {
        return currentDayOfWeek;
    }

    public void setCurrentDayOfWeek(Integer currentDayOfWeek) {
        this.currentDayOfWeek = currentDayOfWeek;
    }

    public Integer getCurrentSshSessions() {
        return currentSshSessions;
    }

    public void setCurrentSshSessions(Integer currentSshSessions) {
        this.currentSshSessions = currentSshSessions;
    }

    public Integer getCurrentRdpSessions() {
        return currentRdpSessions;
    }

    public void setCurrentRdpSessions(Integer currentRdpSessions) {
        this.currentRdpSessions = currentRdpSessions;
    }

    public Integer getCurrentTelnetSessions() {
        return currentTelnetSessions;
    }

    public void setCurrentTelnetSessions(Integer currentTelnetSessions) {
        this.currentTelnetSessions = currentTelnetSessions;
    }

    public Set<Long> getAssignedPolicyIds() {
        return assignedPolicyIds;
    }

    public void setAssignedPolicyIds(Set<Long> assignedPolicyIds) {
        this.assignedPolicyIds = assignedPolicyIds;
    }

    public Set<String> getAllowedProtocols() {
        return allowedProtocols;
    }

    public void setAllowedProtocols(Set<String> allowedProtocols) {
        this.allowedProtocols = allowedProtocols;
    }

    public Set<String> getAllowedDbms() {
        return allowedDbms;
    }

    public void setAllowedDbms(Set<String> allowedDbms) {
        this.allowedDbms = allowedDbms;
    }

    public Set<String> getBlacklistedCommands() {
        return blacklistedCommands;
    }

    public void setBlacklistedCommands(Set<String> blacklistedCommands) {
        this.blacklistedCommands = blacklistedCommands;
    }

    public Set<String> getWhitelistedCommands() {
        return whitelistedCommands;
    }

    public void setWhitelistedCommands(Set<String> whitelistedCommands) {
        this.whitelistedCommands = whitelistedCommands;
    }

    public Set<TimeSlot> getAllowedTimeSlots() {
        return allowedTimeSlots;
    }

    public void setAllowedTimeSlots(Set<TimeSlot> allowedTimeSlots) {
        this.allowedTimeSlots = allowedTimeSlots;
    }

    public Set<String> getAllowedIps() {
        return allowedIps;
    }

    public void setAllowedIps(Set<String> allowedIps) {
        this.allowedIps = allowedIps;
    }

    public String getIpFilteringType() {
        return ipFilteringType;
    }

    public void setIpFilteringType(String ipFilteringType) {
        this.ipFilteringType = ipFilteringType;
    }
}