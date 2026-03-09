package com.corejsf.DTO;

/**
 * Inbound DTO for returning a timesheet.
 * The approver must supply a non-empty comment explaining why.
 */
public class ReturnTimesheetRequestDTO {

    private String returnComment;

    public String getReturnComment() {
        return returnComment;
    }

    public void setReturnComment(String returnComment) {
        this.returnComment = returnComment;
    }
}
