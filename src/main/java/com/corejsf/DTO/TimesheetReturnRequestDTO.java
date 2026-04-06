package com.corejsf.DTO;

/**
 * Request DTO for returning a submitted timesheet to an employee.
 */
public class TimesheetReturnRequestDTO {

    private String returnComment;

    public String getReturnComment() {
        return returnComment;
    }

    public void setReturnComment(String returnComment) {
        this.returnComment = returnComment;
    }
}
