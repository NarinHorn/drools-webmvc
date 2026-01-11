package com.hunesion.drool_v2.model;

/**
 * EquipmentAccessResult - Drools fact that holds the result of equipment policy evaluation
 */
public class EquipmentAccessResult {

    private boolean allowed = false;
    private boolean evaluated = false;
    private String matchedPolicyName;
    private String denialReason;
    private String denialCode;

    public EquipmentAccessResult() {
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

    public String getDenialCode() {
        return denialCode;
    }

    public void setDenialCode(String denialCode) {
        this.denialCode = denialCode;
    }

    public void allow(String policyName) {
        this.allowed = true;
        this.evaluated = true;
        this.matchedPolicyName = policyName;
    }

    public void deny(String policyName, String reason) {
        deny(policyName, reason, null);
    }

    public void deny(String policyName, String reason, String code) {
        this.allowed = false;
        this.evaluated = true;
        this.matchedPolicyName = policyName;
        this.denialReason = reason;
        this.denialCode = code;
    }

    @Override
    public String toString() {
        return "EquipmentAccessResult{" +
                "allowed=" + allowed +
                ", evaluated=" + evaluated +
                ", matchedPolicyName='" + matchedPolicyName + '\'' +
                ", denialReason='" + denialReason + '\'' +
                ", denialCode='" + denialCode + '\'' +
                '}';
    }
}