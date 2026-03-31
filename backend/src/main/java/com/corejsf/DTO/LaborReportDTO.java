package com.corejsf.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class LaborReportDTO {

    private String projectId;
    private String wpId;
    private Integer employeeId;
    private LocalDate weekEnding;
    private SummaryDTO summary;
    private List<LaborReportRowDTO> rows;
    private BigDecimal totalRowHours;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getWpId() {
        return wpId;
    }

    public void setWpId(String wpId) {
        this.wpId = wpId;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getWeekEnding() {
        return weekEnding;
    }

    public void setWeekEnding(LocalDate weekEnding) {
        this.weekEnding = weekEnding;
    }

    public SummaryDTO getSummary() {
        return summary;
    }

    public void setSummary(SummaryDTO summary) {
        this.summary = summary;
    }

    public List<LaborReportRowDTO> getRows() {
        return rows;
    }

    public void setRows(List<LaborReportRowDTO> rows) {
        this.rows = rows;
    }

    public BigDecimal getTotalRowHours() {
        return totalRowHours;
    }

    public void setTotalRowHours(BigDecimal totalRowHours) {
        this.totalRowHours = totalRowHours;
    }

    public static class SummaryDTO {
        private BigDecimal totalHours;
        private BigDecimal previousWeekHours;
        private BigDecimal hoursChangePercent;
        private BigDecimal overtimeHours;
        private BigDecimal pendingApprovalHours;
        private long activeWorkPackages;

        public BigDecimal getTotalHours() {
            return totalHours;
        }

        public void setTotalHours(BigDecimal totalHours) {
            this.totalHours = totalHours;
        }

        public BigDecimal getPreviousWeekHours() {
            return previousWeekHours;
        }

        public void setPreviousWeekHours(BigDecimal previousWeekHours) {
            this.previousWeekHours = previousWeekHours;
        }

        public BigDecimal getHoursChangePercent() {
            return hoursChangePercent;
        }

        public void setHoursChangePercent(BigDecimal hoursChangePercent) {
            this.hoursChangePercent = hoursChangePercent;
        }

        public BigDecimal getOvertimeHours() {
            return overtimeHours;
        }

        public void setOvertimeHours(BigDecimal overtimeHours) {
            this.overtimeHours = overtimeHours;
        }

        public BigDecimal getPendingApprovalHours() {
            return pendingApprovalHours;
        }

        public void setPendingApprovalHours(BigDecimal pendingApprovalHours) {
            this.pendingApprovalHours = pendingApprovalHours;
        }

        public long getActiveWorkPackages() {
            return activeWorkPackages;
        }

        public void setActiveWorkPackages(long activeWorkPackages) {
            this.activeWorkPackages = activeWorkPackages;
        }
    }
}
