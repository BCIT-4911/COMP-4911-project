package com.corejsf.DTO;

import java.time.LocalDate;

public class EarnedValueReportRequestDTO {

    private Integer projectId;
    private Integer controlAccountId;

    private LocalDate fromWeekEnding; // optional
    private LocalDate toWeekEnding;   // optional

    private Boolean approvedOnly;     // optional (usually true for ACWP)

    public EarnedValueReportRequestDTO() { }

    // getters/setters...
    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }

    public Integer getControlAccountId() { return controlAccountId; }
    public void setControlAccountId(Integer controlAccountId) { this.controlAccountId = controlAccountId; }

    public LocalDate getFromWeekEnding() { return fromWeekEnding; }
    public void setFromWeekEnding(LocalDate fromWeekEnding) { this.fromWeekEnding = fromWeekEnding; }

    public LocalDate getToWeekEnding() { return toWeekEnding; }
    public void setToWeekEnding(LocalDate toWeekEnding) { this.toWeekEnding = toWeekEnding; }

    public Boolean getApprovedOnly() { return approvedOnly; }
    public void setApprovedOnly(Boolean approvedOnly) { this.approvedOnly = approvedOnly; }
}