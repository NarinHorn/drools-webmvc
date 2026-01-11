package com.hunesion.drool_v2.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "command_lists")
public class CommandList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "list_name", nullable = false)
    private String listName;

    @Column(name = "list_type", nullable = false)
    private String listType; // 'blacklist', 'whitelist', 'watchlist'

    @Column(name = "protocol_type", nullable = false)
    private String protocolType; // 'TELNET_SSH', 'DB'

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "commandList", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CommandListItem> items = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public CommandList() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public String getListType() {
        return listType;
    }

    public void setListType(String listType) {
        this.listType = listType;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<CommandListItem> getItems() {
        return items;
    }

    public void setItems(Set<CommandListItem> items) {
        this.items = items;
    }
}