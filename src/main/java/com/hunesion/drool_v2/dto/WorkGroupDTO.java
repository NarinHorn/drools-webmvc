package com.hunesion.drool_v2.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Set;

@JsonPropertyOrder({
    "id",
    "workGroupName",
    "description",
    "enabled",
    "userCount",
    "equipmentCount",
    "accountCount",
    "policyCount",
    "userIds",
    "equipmentIds",
    "accountIds",
    "policyIds"
})
public class WorkGroupDTO {
    private Long id;
    private String workGroupName;
    private String description;
    private boolean enabled;
    private int userCount;
    private int equipmentCount;
    private int accountCount;
    private int policyCount;
    private Set<Long> userIds;
    private Set<Long> equipmentIds;
    private Set<Long> accountIds;
    private Set<Long> policyIds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWorkGroupName() {
        return workGroupName;
    }

    public void setWorkGroupName(String workGroupName) {
        this.workGroupName = workGroupName;
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

    public int getUserCount() {
        return userCount;
    }

    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }

    public int getEquipmentCount() {
        return equipmentCount;
    }

    public void setEquipmentCount(int equipmentCount) {
        this.equipmentCount = equipmentCount;
    }

    public int getAccountCount() {
        return accountCount;
    }

    public void setAccountCount(int accountCount) {
        this.accountCount = accountCount;
    }

    public int getPolicyCount() {
        return policyCount;
    }

    public void setPolicyCount(int policyCount) {
        this.policyCount = policyCount;
    }

    public Set<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(Set<Long> userIds) {
        this.userIds = userIds;
    }

    public Set<Long> getEquipmentIds() {
        return equipmentIds;
    }

    public void setEquipmentIds(Set<Long> equipmentIds) {
        this.equipmentIds = equipmentIds;
    }

    public Set<Long> getAccountIds() {
        return accountIds;
    }

    public void setAccountIds(Set<Long> accountIds) {
        this.accountIds = accountIds;
    }

    public Set<Long> getPolicyIds() {
        return policyIds;
    }

    public void setPolicyIds(Set<Long> policyIds) {
        this.policyIds = policyIds;
    }
}
