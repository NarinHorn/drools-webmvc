package com.hunesion.drool_v2.dto;

import java.time.LocalDate;
import java.util.List;

public class PolicyAllowedTimeDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean borderless = false;
    private String timeZone;
    private List<TimeSlotDTO> timeSlots;

    public static class TimeSlotDTO {
        private Integer dayOfWeek; // 1=Monday, 7=Sunday
        private Integer hourStart; // 0-23
        private Integer hourEnd; // 0-23

        public TimeSlotDTO() {
        }

        public TimeSlotDTO(Integer dayOfWeek, Integer hourStart, Integer hourEnd) {
            this.dayOfWeek = dayOfWeek;
            this.hourStart = hourStart;
            this.hourEnd = hourEnd;
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

    // Getters and Setters
    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isBorderless() {
        return borderless;
    }

    public void setBorderless(boolean borderless) {
        this.borderless = borderless;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public List<TimeSlotDTO> getTimeSlots() {
        return timeSlots;
    }

    public void setTimeSlots(List<TimeSlotDTO> timeSlots) {
        this.timeSlots = timeSlots;
    }
}