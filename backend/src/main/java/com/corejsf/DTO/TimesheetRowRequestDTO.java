package com.corejsf.DTO;

import java.math.BigDecimal;

/**
 * Inbound DTO for creating/updating a single timesheet row.
 * Contains only the fields the client needs to send.
 */
public class TimesheetRowRequestDTO {

    private String wpId;
    private Integer laborGradeId;
    private BigDecimal monday;
    private BigDecimal tuesday;
    private BigDecimal wednesday;
    private BigDecimal thursday;
    private BigDecimal friday;
    private BigDecimal saturday;
    private BigDecimal sunday;

    public String getWpId() {
        return wpId;
    }

    public void setWpId(String wpId) {
        this.wpId = wpId;
    }

    public Integer getLaborGradeId() {
        return laborGradeId;
    }

    public void setLaborGradeId(Integer laborGradeId) {
        this.laborGradeId = laborGradeId;
    }

    public BigDecimal getMonday() {
        return monday;
    }

    public void setMonday(BigDecimal monday) {
        this.monday = monday;
    }

    public BigDecimal getTuesday() {
        return tuesday;
    }

    public void setTuesday(BigDecimal tuesday) {
        this.tuesday = tuesday;
    }

    public BigDecimal getWednesday() {
        return wednesday;
    }

    public void setWednesday(BigDecimal wednesday) {
        this.wednesday = wednesday;
    }

    public BigDecimal getThursday() {
        return thursday;
    }

    public void setThursday(BigDecimal thursday) {
        this.thursday = thursday;
    }

    public BigDecimal getFriday() {
        return friday;
    }

    public void setFriday(BigDecimal friday) {
        this.friday = friday;
    }

    public BigDecimal getSaturday() {
        return saturday;
    }

    public void setSaturday(BigDecimal saturday) {
        this.saturday = saturday;
    }

    public BigDecimal getSunday() {
        return sunday;
    }

    public void setSunday(BigDecimal sunday) {
        this.sunday = sunday;
    }
}
