package com.hunesion.drool_v2.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "policy_time_slots")
public class PolicyTimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    @JsonIgnore // Break circular reference with PolicyAllowedTime
    private PolicyAllowedTime policy;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek; // 1=Monday, 7=Sunday

    @Column(name = "hour_start", nullable = false)
    private Integer hourStart; // 0-23

    @Column(name = "hour_end", nullable = false)
    private Integer hourEnd; // 0-23

    public PolicyTimeSlot() {
    }

    public PolicyTimeSlot(PolicyAllowedTime policy, Integer dayOfWeek, Integer hourStart, Integer hourEnd) {
        this.policy = policy;
        this.dayOfWeek = dayOfWeek;
        this.hourStart = hourStart;
        this.hourEnd = hourEnd;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PolicyAllowedTime getPolicy() {
        return policy;
    }

    public void setPolicy(PolicyAllowedTime policy) {
        this.policy = policy;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Integer getHourStart() {
        return hourStart;
    }

    public void setHourStart(Integer hourStart) {
        this.hourStart = hourStart;
    }

    public Integer getHourEnd() {
        return hourEnd;
    }

    public void setHourEnd(Integer hourEnd) {
        this.hourEnd = hourEnd;
    }
}