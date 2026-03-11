package com.corejsf.DTO;

import com.corejsf.Entity.TimesheetStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Outbound DTO returned by the API for a timesheet.
 * Includes server-generated fields, resolved display names, and nested rows.
 */
public class TimesheetResponseDTO {

    private Integer tsId;
    private Integer empId;
    private String empName;
    private LocalDate weekEnding;
    private Integer approverId;
    private String approverName;
    private String returnComment;
    private TimesheetStatus timesheetStatus;
    private List<TimesheetRowResponseDTO> rows;

    public Integer getTsId() {
        return tsId;
    }

    public void setTsId(Integer tsId) {
        this.tsId = tsId;
    }

    public Integer getEmpId() {
        return empId;
    }

    public void setEmpId(Integer empId) {
        this.empId = empId;
    }

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
    }

    public LocalDate getWeekEnding() {
        return weekEnding;
    }

    public void setWeekEnding(LocalDate weekEnding) {
        this.weekEnding = weekEnding;
    }

    public Integer getApproverId() {
        return approverId;
    }

    public void setApproverId(Integer approverId) {
        this.approverId = approverId;
    }

    public String getApproverName() {
        return approverName;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    public TimesheetStatus getStatus() { return timesheetStatus; }

    public void setTimesheetStatus(TimesheetStatus timesheetStatus) { this.timesheetStatus = timesheetStatus; }

    public String getReturnComment() {
        return returnComment;
    }

    public void setReturnComment(String returnComment) {
        this.returnComment = returnComment;
    }

    public List<TimesheetRowResponseDTO> getRows() {
        return rows;
    }

    public void setRows(List<TimesheetRowResponseDTO> rows) {
        this.rows = rows;
    }
}
