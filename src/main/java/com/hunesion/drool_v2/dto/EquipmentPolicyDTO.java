package com.hunesion.drool_v2.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DTO for creating/updating Equipment Policy
 */
@JsonPropertyOrder({
        "policyName",
        "description",
        "policyClassification",
        "policyApplication",
        "enabled",
        "priority",
        "commonSettings",
        "allowedTime",
        "commandSettings",
        "loginControl"
})
public class EquipmentPolicyDTO {
    private Long id;
    private String policyName;
    private String description;
    private String policyClassification = "common"; // 'common', 'temporary', 'basic'
    private String policyApplication = "apply"; // 'apply', 'not_applicable'
    private Long equipmentBasicPolicyId;
    private boolean enabled = true;
    private Integer priority = 0;

    private PolicyCommonSettingsDTO commonSettings;
    private PolicyAllowedTimeDTO allowedTime;
    private List<PolicyCommandSettingsDTO> commandSettings;
    private PolicyLoginControlDTO loginControl;
    private List<Map<String, Object>> policies;

    // Raw policyConfig - allows sending config directly as JSON object
    // If provided, this takes precedence over individual settings (commonSettings, allowedTime, etc.)
    private Map<String, Object> policyConfig;

    // NEW: Custom conditions (like AccessPolicy) - flexible fields from frontend
    private Map<String, ConditionDTO> customConditions;
    
    // NEW: Frontend-only metadata (not used in DRL generation)
    private Map<String, Object> customMetadata;

    // Assignment IDs (for creating assignments)
    private Set<Long> userIds;
    private Set<Long> groupIds;
    private Set<Long> equipmentIds;
    private Set<Long> roleIds;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPolicyClassification() {
        return policyClassification;
    }

    public void setPolicyClassification(String policyClassification) {
        this.policyClassification = policyClassification;
    }

    public String getPolicyApplication() {
        return policyApplication;
    }

    public void setPolicyApplication(String policyApplication) {
        this.policyApplication = policyApplication;
    }

    public Long getEquipmentBasicPolicyId() {
        return equipmentBasicPolicyId;
    }

    public void setEquipmentBasicPolicyId(Long equipmentBasicPolicyId) {
        this.equipmentBasicPolicyId = equipmentBasicPolicyId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public PolicyCommonSettingsDTO getCommonSettings() {
        return commonSettings;
    }

    public void setCommonSettings(PolicyCommonSettingsDTO commonSettings) {
        this.commonSettings = commonSettings;
    }

    public PolicyAllowedTimeDTO getAllowedTime() {
        return allowedTime;
    }

    public void setAllowedTime(PolicyAllowedTimeDTO allowedTime) {
        this.allowedTime = allowedTime;
    }

    public List<PolicyCommandSettingsDTO> getCommandSettings() {
        return commandSettings;
    }

    public void setCommandSettings(List<PolicyCommandSettingsDTO> commandSettings) {
        this.commandSettings = commandSettings;
    }

    public PolicyLoginControlDTO getLoginControl() {
        return loginControl;
    }

    public void setLoginControl(PolicyLoginControlDTO loginControl) {
        this.loginControl = loginControl;
    }

    public Map<String, Object> getPolicyConfig() {
        return policyConfig;
    }

    public void setPolicyConfig(Map<String, Object> policyConfig) {
        this.policyConfig = policyConfig;
    }

    public Set<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(Set<Long> userIds) {
        this.userIds = userIds;
    }

    public Set<Long> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(Set<Long> groupIds) {
        this.groupIds = groupIds;
    }

    public Set<Long> getEquipmentIds() {
        return equipmentIds;
    }

    public void setEquipmentIds(Set<Long> equipmentIds) {
        this.equipmentIds = equipmentIds;
    }

    public Set<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(Set<Long> roleIds) {
        this.roleIds = roleIds;
    }

    public Map<String, ConditionDTO> getCustomConditions() {
        return customConditions;
    }

    public void setCustomConditions(Map<String, ConditionDTO> customConditions) {
        this.customConditions = customConditions;
    }

    public Map<String, Object> getCustomMetadata() {
        return customMetadata;
    }

    public void setCustomMetadata(Map<String, Object> customMetadata) {
        this.customMetadata = customMetadata;
    }
}