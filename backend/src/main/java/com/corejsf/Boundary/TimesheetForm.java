package com.corejsf.boundary;

import com.corejsf.Control.TimesheetController;
import com.corejsf.entity.TimesheetRow;
import com.corejsf.entity.WorkPackage;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/*
 * This backing bean acts as the main interface for the employee entering their hours. 
 * It binds directly to the frontend inputs, holding the current state of the timesheet grid 
 * like the selected week, the rows being edited, and the signature data if they are ready to submit.
 * When the user clicks "Save Draft" or "Submit", this class grabs the UI data and passes it down 
 * to the TimesheetController. It also handles the validation feedback, so if the controller says 
 * the hours are short, this class updates the UI to show that error message to the employee.
 */
@Named
@ViewScoped
public class TimesheetForm implements Serializable {

    @Inject
    private TimesheetController timesheetController;

    private LocalDate weekEndingDate;
    private String selectedWorkPackageId;
    private LocalDate entryDate;
    private BigDecimal loggedHours;
    private byte[] capturedESignature;
    private List<TimesheetRow> existingEntries;
    
    private Integer currentTimesheetId; // Assuming we track the active timesheet

    public void captureTimeEntry() {
        // Logic to bind UI input to a new or existing TimesheetRow
    }

    public void requestSaveDraft() {
        if (currentTimesheetId != null && existingEntries != null) {
            timesheetController.saveAsDraft(currentTimesheetId, existingEntries);
        }
    }

    public void requestSubmission() {
        // Used 40.0 as the expected full-time hours for validation, later to be replaced 
        // with a dynamic value from Employee class's expected_weekly_hours column.
        BigDecimal expectedWeeklyHours = new BigDecimal("40.0"); 
        
        if (timesheetController.validateHours(currentTimesheetId, expectedWeeklyHours)) {
            timesheetController.finalizeSubmission(currentTimesheetId, capturedESignature);
        } else {
            displayValidationFeedback("Total hours do not meet the expected weekly requirement.");
        }
    }

    public void displayValidationFeedback(String errors) {
        // Logic to push error messages to the JSF FacesContext so the user sees them
        System.out.println("Validation Error: " + errors);
    }

    public void loadTimesheetData(Integer timesheetId) {
        this.currentTimesheetId = timesheetId;
        // Logic to fetch timesheet and populate existingEntries
    }

    public List<WorkPackage> presentWorkPackageList() {
        // Return a list of available work packages for the dropdown menu
        return null;
    }

    // Getters and Setters for JSF binding
    public LocalDate getWeekEndingDate() { return weekEndingDate; }
    public void setWeekEndingDate(LocalDate weekEndingDate) { this.weekEndingDate = weekEndingDate; }
    public String getSelectedWorkPackageId() { return selectedWorkPackageId; }
    public void setSelectedWorkPackageId(String selectedWorkPackageId) { this.selectedWorkPackageId = selectedWorkPackageId; }
    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
    public BigDecimal getLoggedHours() { return loggedHours; }
    public void setLoggedHours(BigDecimal loggedHours) { this.loggedHours = loggedHours; }
    public byte[] getCapturedESignature() { return capturedESignature; }
    public void setCapturedESignature(byte[] capturedESignature) { this.capturedESignature = capturedESignature; }
    public List<TimesheetRow> getExistingEntries() { return existingEntries; }
    public void setExistingEntries(List<TimesheetRow> existingEntries) { this.existingEntries = existingEntries; }
}