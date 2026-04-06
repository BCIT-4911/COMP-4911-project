package com.corejsf.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LaborReportRowDTO {

    private String projectId;
    private String projectName;
    private Integer empId;
    private String employeeName;
    private String wpId;
    private String workPackageName;
    private Integer laborGradeId;
    private String laborGradeCode;
    private BigDecimal chargeRate;
    private BigDecimal hours;
    private LocalDate weekEnding;
    private String status;
    private String statusLabel;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Integer getEmpId() {
        return empId;
    }

    public void setEmpId(Integer empId) {
        this.empId = empId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getWpId() {
        return wpId;
    }

    public void setWpId(String wpId) {
        this.wpId = wpId;
    }

    public String getWorkPackageName() {
        return workPackageName;
    }

    public void setWorkPackageName(String workPackageName) {
        this.workPackageName = workPackageName;
    }

    public Integer getLaborGradeId() {
        return laborGradeId;
    }

    public void setLaborGradeId(Integer laborGradeId) {
        this.laborGradeId = laborGradeId;
    }

    public String getLaborGradeCode() {
        return laborGradeCode;
    }

    public void setLaborGradeCode(String laborGradeCode) {
        this.laborGradeCode = laborGradeCode;
    }

    public BigDecimal getChargeRate() {
        return chargeRate;
    }

    public void setChargeRate(BigDecimal chargeRate) {
        this.chargeRate = chargeRate;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }

    public LocalDate getWeekEnding() {
        return weekEnding;
    }

    public void setWeekEnding(LocalDate weekEnding) {
        this.weekEnding = weekEnding;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }
}
