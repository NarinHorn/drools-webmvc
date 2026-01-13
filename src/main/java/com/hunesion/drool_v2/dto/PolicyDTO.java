package com.hunesion.drool_v2.dto;

import java.util.List;
import java.util.Map;

/**
 * PolicyDTO - Data Transfer Object for creating/updating access policies from frontend
 * This structured format is converted to DRL by the backend
 */
public class PolicyDTO {

    private Long id;
    private String policyName;
    private String description;
    private String endpoint;
    private String httpMethod;
    private List<String> allowedRoles;
    // Add this field in PolicyDTO class (after line 17)
    private List<Long> groupIds;
    private Map<String, ConditionDTO> conditions;
    private String effect;
    private Integer priority;
    private Boolean enabled;

    public PolicyDTO() {
    }

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

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public List<String> getAllowedRoles() {
        return allowedRoles;
    }

    public void setAllowedRoles(List<String> allowedRoles) {
        this.allowedRoles = allowedRoles;
    }

    public Map<String, ConditionDTO> getConditions() {
        return conditions;
    }

    public void setConditions(Map<String, ConditionDTO> conditions) {
        this.conditions = conditions;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    // Add getter and setter
    public List<Long> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(List<Long> groupIds) {
        this.groupIds = groupIds;
    }

    @Override
    public String toString() {
        return "PolicyDTO{" +
                "policyName='" + policyName + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                ", allowedRoles=" + allowedRoles +
                ", effect='" + effect + '\'' +
                '}';
    }
}
