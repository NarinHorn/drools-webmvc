package com.hunesion.drool_v2.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * AccessRequest - Drools fact representing an access request to be evaluated
 * This contains all context needed for policy evaluation
 */
public class AccessRequest {

    private String username;
    private Set<String> userRoles = new HashSet<>();
    private String endpoint;
    private String httpMethod;
    private String department;
    private Integer userLevel;
    private Map<String, Object> attributes = new HashMap<>();

    public AccessRequest() {
    }

    public AccessRequest(String username, Set<String> userRoles, String endpoint, String httpMethod) {
        this.username = username;
        this.userRoles = userRoles;
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
    }

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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getUserLevel() {
        return userLevel;
    }

    public void setUserLevel(Integer userLevel) {
        this.userLevel = userLevel;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public void setAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }

    public boolean hasRole(String role) {
        return userRoles != null && userRoles.contains(role);
    }

    public boolean hasAnyRole(String... roles) {
        if (userRoles == null) return false;
        for (String role : roles) {
            if (userRoles.contains(role)) return true;
        }
        return false;
    }

    public boolean endpointMatches(String pattern) {
        if (endpoint == null || pattern == null) return false;
        // Pattern is already a regex (converted by PolicyService.convertEndpointToRegex)
        return endpoint.matches(pattern);
    }

    @Override
    public String toString() {
        return "AccessRequest{" +
                "username='" + username + '\'' +
                ", userRoles=" + userRoles +
                ", endpoint='" + endpoint + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                ", department='" + department + '\'' +
                ", userLevel=" + userLevel +
                '}';
    }
}
