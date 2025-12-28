package com.hunesion.drool_v2.model;

/**
 * AccessResult - Drools fact that holds the result of policy evaluation
 * Rules will modify this object to indicate whether access is allowed or denied
 */
public class AccessResult {

    private boolean allowed = false;
    private boolean evaluated = false;
    private String matchedPolicyName;
    private String denialReason;

    public AccessResult() {
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
        this.evaluated = true;
    }

    public boolean isEvaluated() {
        return evaluated;
    }

    public void setEvaluated(boolean evaluated) {
        this.evaluated = evaluated;
    }

    public String getMatchedPolicyName() {
        return matchedPolicyName;
    }

    public void setMatchedPolicyName(String matchedPolicyName) {
        this.matchedPolicyName = matchedPolicyName;
    }

    public String getDenialReason() {
        return denialReason;
    }

    public void setDenialReason(String denialReason) {
        this.denialReason = denialReason;
    }

    public void allow(String policyName) {
        this.allowed = true;
        this.evaluated = true;
        this.matchedPolicyName = policyName;
    }

    public void deny(String policyName, String reason) {
        this.allowed = false;
        this.evaluated = true;
        this.matchedPolicyName = policyName;
        this.denialReason = reason;
    }

    @Override
    public String toString() {
        return "AccessResult{" +
                "allowed=" + allowed +
                ", evaluated=" + evaluated +
                ", matchedPolicyName='" + matchedPolicyName + '\'' +
                ", denialReason='" + denialReason + '\'' +
                '}';
    }
}
