package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "equipment")
@JsonPropertyOrder({
    "id",
    "deviceName",
    "hostName",
    "ipAddress",
    "protocol",
    "port",
    "username",
    "deviceType",
    "isDeleted",
    "createdAt",
    "updatedAt"
})
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_name", nullable = false, length = 200)
    private String deviceName;

    @Column(name = "host_name", length = 255)
    private String hostName;

    @Column(name = "ip_address", length = 45) // IPv6 max length
    private String ipAddress;

    @Column(name = "protocol", length = 50)
    private String protocol; // e.g., "ssh", "rdp", "http", "https", "vnc"

    @Column(name = "port")
    private Integer port; // e.g., 22 (SSH), 3389 (RDP), 80 (HTTP), 443 (HTTPS)

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "password", length = 500)
    @JsonIgnore // Hide password in JSON responses for security
    private String password;

    @Column(name = "device_type", length = 50)
    private String deviceType; // e.g., "LINUX_SERVER", "DATABASE", "WINDOWS_SERVER"

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany(mappedBy = "equipment", fetch = FetchType.LAZY)
    @JsonIgnore // Avoid circular reference in JSON responses
    private Set<User> users = new HashSet<>();

    @ManyToMany(mappedBy = "equipment", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<EquipmentGroup> equipmentGroups = new HashSet<>();

    @OneToMany(mappedBy = "equipment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<Account> accounts = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Equipment() {
    }

    public Equipment(String deviceName, String hostName, String ipAddress, String deviceType) {
        this.deviceName = deviceName;
        this.hostName = hostName;
        this.ipAddress = ipAddress;
        this.deviceType = deviceType;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // Password getter - hidden in JSON responses
    @JsonIgnore
    public String getPassword() {
        return password;
    }

    // Password setter - allows setting password from JSON requests
    @JsonProperty("password")
    public void setPassword(String password) {
        this.password = password;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public Set<EquipmentGroup> getEquipmentGroups() {
        return equipmentGroups;
    }

    public void setEquipmentGroups(Set<EquipmentGroup> equipmentGroups) {
        this.equipmentGroups = equipmentGroups;
    }

    public Set<Account> getAccounts() {
        return accounts;
    }
    
    public void setAccounts(Set<Account> accounts) {
        this.accounts = accounts;
    }

    @Override
    public String toString() {
        return "Equipment{" +
                "id=" + id +
                ", deviceName='" + deviceName + '\'' +
                ", hostName='" + hostName + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", protocol='" + protocol + '\'' +
                ", port=" + port +
                ", username='" + username + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", isDeleted=" + isDeleted +
                '}';
    }
}
