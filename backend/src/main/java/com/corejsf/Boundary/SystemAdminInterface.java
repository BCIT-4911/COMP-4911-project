package com.corejsf.Boundary;

import com.corejsf.Entity.Timesheet;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import java.io.Serializable;
import java.util.List;

/*
 * This bean supports the auditing capabilities required by the system administrator. 
 * Since admins do not approve or modify timesheets directly, this class does not heavily interact 
 * with the TimesheetController's modification logic. Instead, it captures search parameters from 
 * the UI, queries the database for matching timesheets, and holds the results so the admin can 
 * view the detailed history of any employee's logged hours.
 */
@Named
@ViewScoped
public class SystemAdminInterface implements Serializable {

    private List<Timesheet> timesheetList;
    private String searchCriteria;
    
    private Timesheet viewedTimesheetDetail;

    public void captureSearchCriteria() {
        // Logic to read searchCriteria and execute the search query
        displayTimesheetResults(null); // Passing the simulated result list
    }

    public void displayTimesheetResults(List<Timesheet> results) {
        this.timesheetList = results;
    }

    public void viewTimesheetDetail(Integer timesheetId) {
        // Logic to fetch and set viewedTimesheetDetail for the UI to display
    }

    // Getters and Setters for JSF binding
    public List<Timesheet> getTimesheetList() { return timesheetList; }
    public void setTimesheetList(List<Timesheet> timesheetList) { this.timesheetList = timesheetList; }
    public String getSearchCriteria() { return searchCriteria; }
    public void setSearchCriteria(String searchCriteria) { this.searchCriteria = searchCriteria; }
    public Timesheet getViewedTimesheetDetail() { return viewedTimesheetDetail; }
    public void setViewedTimesheetDetail(Timesheet viewedTimesheetDetail) { this.viewedTimesheetDetail = viewedTimesheetDetail; }
}