package com.corejsf.Boundary;

import com.corejsf.Control.TimesheetController;
import com.corejsf.Entity.Timesheet;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.List;

/*
 * This is the UI controller for the supervisor or manager. 
 * It manages the list of timesheets waiting for their review. When the approver selects a specific 
 * timesheet, this bean holds that detailed view. It provides the methods tied to the "Approve" 
 * and "Return" buttons on the frontend. If they return it, it passes their typed comments down 
 * to the TimesheetController so the record can be unlocked and the employee can fix it.
 */
@Named
@ViewScoped
public class TimesheetApproverInterface implements Serializable {

    @Inject
    private TimesheetController timesheetController;

    private List<Timesheet> pendingTimesheetList;
    private Timesheet detailedTimesheetView;
    private String approverComments;

    public void viewPendingTimesheets() {
        // Logic to query and populate pendingTimesheetList for the logged-in approver
    }

    public void reviewDetails(Integer timesheetId) {
        // Logic to fetch a specific timesheet and assign it to detailedTimesheetView
    }

    public void submitApproval(Integer timesheetId) {
        timesheetController.applyApprovalLock(timesheetId);
        viewPendingTimesheets(); // Refresh the list after approving
    }

    public void returnForCorrection(Integer timesheetId) {
        timesheetController.returnForCorrection(timesheetId, approverComments);
        this.approverComments = ""; // Clear comments after submission
        viewPendingTimesheets(); // Refresh the list
    }

    // Getters and Setters for JSF binding
    public List<Timesheet> getPendingTimesheetList() { return pendingTimesheetList; }
    public void setPendingTimesheetList(List<Timesheet> pendingTimesheetList) { this.pendingTimesheetList = pendingTimesheetList; }
    public Timesheet getDetailedTimesheetView() { return detailedTimesheetView; }
    public void setDetailedTimesheetView(Timesheet detailedTimesheetView) { this.detailedTimesheetView = detailedTimesheetView; }
    public String getApproverComments() { return approverComments; }
    public void setApproverComments(String approverComments) { this.approverComments = approverComments; }
}