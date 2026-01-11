package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "policy_command_settings")
public class PolicyCommandSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    @JsonIgnore // Break circular reference with EquipmentPolicy
    private EquipmentPolicy policy;

    @Column(name = "protocol_type", nullable = false)
    private String protocolType; // 'TELNET_SSH', 'DB'

    @Column(name = "control_method", nullable = false)
    private String controlMethod; // 'blacklist', 'whitelist'

    @Column(name = "control_target", nullable = false)
    private String controlTarget; // 'entire_string', 'command'

    @ManyToMany
    @JoinTable(
            name = "policy_command_lists",
            joinColumns = @JoinColumn(name = "policy_id"),
            inverseJoinColumns = @JoinColumn(name = "command_list_id")
    )
    private Set<CommandList> commandLists = new HashSet<>();

    public PolicyCommandSettings() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EquipmentPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(EquipmentPolicy policy) {
        this.policy = policy;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    public String getControlMethod() {
        return controlMethod;
    }

    public void setControlMethod(String controlMethod) {
        this.controlMethod = controlMethod;
    }

    public String getControlTarget() {
        return controlTarget;
    }

    public void setControlTarget(String controlTarget) {
        this.controlTarget = controlTarget;
    }

    public Set<CommandList> getCommandLists() {
        return commandLists;
    }

    public void setCommandLists(Set<CommandList> commandLists) {
        this.commandLists = commandLists;
    }
}