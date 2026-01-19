package com.hunesion.drool_v2.dto;

import java.util.Set;

public class PolicyGroupDTO {

    private String groupName;
    private String description;
    private boolean enabled = true;
    private Set<Long> policyIds;

    public PolicyGroupDTO() {
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<Long> getPolicyIds() {
        return policyIds;
    }

    public void setPolicyIds(Set<Long> policyIds) {
        this.policyIds = policyIds;
    }
}
