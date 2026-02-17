package com.corejsf.DTO;

import java.time.LocalDate;
import java.util.List;

/**
 * Inbound DTO for creating/updating a timesheet.
 * Contains only the fields the client needs to send.
 */
public class TimesheetRequestDTO {

    private Integer empId;
    private LocalDate weekEnding;
    private Integer approverId;
    private List<TimesheetRowRequestDTO> rows;

    public Integer getEmpId() {
        return empId;
    }

    public void setEmpId(Integer empId) {
        this.empId = empId;
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

    public List<TimesheetRowRequestDTO> getRows() {
        return rows;
    }

    public void setRows(List<TimesheetRowRequestDTO> rows) {
        this.rows = rows;
    }
}
