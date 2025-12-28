package com.hunesion.drool_v2.dto;

/**
 * ConditionDTO - Represents a condition in a policy
 * 
 * Supported operators:
 * - equals: attribute == value
 * - notEquals: attribute != value
 * - greaterThan: attribute > value
 * - lessThan: attribute < value
 * - greaterThanOrEqual: attribute >= value
 * - lessThanOrEqual: attribute <= value
 * - contains: collection contains value
 * - matches: string matches regex pattern
 */
public class ConditionDTO {

    private String operator;
    private String value;

    public ConditionDTO() {
    }

    public ConditionDTO(String operator, String value) {
        this.operator = operator;
        this.value = value;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "ConditionDTO{" +
                "operator='" + operator + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
