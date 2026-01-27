package com.hunesion.drool_v2.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Set;

@JsonPropertyOrder({
        "id",
        "groupName",
        "groupDescription",
        "memberCount",
        "equipmentIds"
})
public class EquipmentGroupDTO {

    private Long id;
    private String groupName;
    private String groupDescription;
    private Integer memberCount;
    private Set<Long> equipmentIds;

    public EquipmentGroupDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupDescription() {
        return groupDescription;
    }

    public void setGroupDescription(String groupDescription) {
        this.groupDescription = groupDescription;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public Set<Long> getEquipmentIds() {
        return equipmentIds;
    }

    public void setEquipmentIds(Set<Long> equipmentIds) {
        this.equipmentIds = equipmentIds;
    }
}

