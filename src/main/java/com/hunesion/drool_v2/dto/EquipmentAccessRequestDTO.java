package com.hunesion.drool_v2.dto;

import java.time.LocalDateTime;

public class EquipmentAccessRequestDTO {
    private String username;
    private Long equipmentId;
    private String protocol; // 'TELNET', 'SSH', 'RDP', etc.
    private String dbmsType; // 'MySQL', 'PostgreSQL', etc.
    private String command; // Command being executed
    private String clientIp;
    private LocalDateTime requestTime;

    public EquipmentAccessRequestDTO() {
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
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

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }
}