package com.hunesion.drool_v2.dto;

import java.util.List;

public class PolicyCommandSettingsDTO {
    private Long id;
    private String protocolType; // 'TELNET_SSH', 'DB'
    private String controlMethod; // 'blacklist', 'whitelist'
    private String controlTarget; // 'entire_string', 'command'
    private List<Long> commandListIds; // IDs of command lists to link

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    public String getControlMethod() {
        return controlMethod;
    }

    public void setControlMethod(String controlMethod) {
        this.controlMethod = controlMethod;
    }

    public String getControlTarget() {
        return controlTarget;
    }

    public void setControlTarget(String controlTarget) {
        this.controlTarget = controlTarget;
    }

    public List<Long> getCommandListIds() {
        return commandListIds;
    }

    public void setCommandListIds(List<Long> commandListIds) {
        this.commandListIds = commandListIds;
    }
}