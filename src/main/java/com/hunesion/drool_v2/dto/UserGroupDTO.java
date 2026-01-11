package com.hunesion.drool_v2.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Set;

@JsonPropertyOrder({
        "id",
        "groupName",
        "groupDescription",
        "memberCount",
        "memberIds"
})
public class UserGroupDTO {
    private Long id;
    private String groupName;
    private String groupDescription;
    private Integer memberCount;
    private Set<Long> memberIds;

    public UserGroupDTO() {
    }

    // Getters and Setters
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

    public Set<Long> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(Set<Long> memberIds) {
        this.memberIds = memberIds;
    }
}