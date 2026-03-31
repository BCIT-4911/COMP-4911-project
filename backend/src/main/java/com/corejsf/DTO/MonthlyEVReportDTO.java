package com.corejsf.DTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for the Monthly Earned Value Report.
 *
 * Structure:
 *   - Top-level project identifiers and project-level EV totals.
 *   - A list of WP-level entries, each carrying its own BCWS, BCWP, and ACWP.
 *
 * BCWS  = Budgeted Cost of Work Scheduled  
 * BCWP  = Budgeted Cost of Work Performed  
 * ACWP  = Actual Cost of Work Performed    
 *
 * Derived metrics included for convenience:
 *   SV = BCWP - BCWS   (Schedule Variance)
 *   CV = BCWP - ACWP   (Cost Variance)
 */
public class MonthlyEVReportDTO {

    
    /** The project ID this report covers. */
    private String projectId;

    /** Human-readable project name. */
    private String projectName;

    /**
     * The date used when computing BCWS and BCWP.]
     */
    private java.time.LocalDate asOfDate;

    // -----------------------------------------------------------------------
    // Project-level EV totals (sum across all work packages)
    // -----------------------------------------------------------------------

    private BigDecimal projectBcws;
    private BigDecimal projectBcwp;
    private BigDecimal projectAcwp;

    /** Schedule Variance at project level: BCWP - BCWS */
    private BigDecimal projectSv;

    /** Cost Variance at project level: BCWP - ACWP */
    private BigDecimal projectCv;

    // -----------------------------------------------------------------------
    // Work-package-level breakdown (AC2)
    // -----------------------------------------------------------------------

    private List<WorkPackageEVEntry> workPackages;

    // -----------------------------------------------------------------------
    // Inner class: one entry per work package
    // -----------------------------------------------------------------------

    public static class WorkPackageEVEntry {

        private String wpId;
        private String wpName;

        /** wp.bac * (elapsed active weeks / total active weeks) up to asOfDate */
        private BigDecimal bcws;

        /** wp.bac * wp.percentComplete / 100 */
        private BigDecimal bcwp;

        /**
         * Sum of (approved timesheet hours for this WP) * (labor grade chargeRate)
         * for all approved timesheets whose weekEnding <= asOfDate.
         */
        private BigDecimal acwp;

        /** BCWP - BCWS */
        private BigDecimal sv;

        /** BCWP - ACWP */
        private BigDecimal cv;

        public WorkPackageEVEntry() { }

        public String getWpId() { return wpId; }
        public void setWpId(String wpId) { this.wpId = wpId; }

        public String getWpName() { return wpName; }
        public void setWpName(String wpName) { this.wpName = wpName; }

        public BigDecimal getBcws() { return bcws; }
        public void setBcws(BigDecimal bcws) { this.bcws = bcws; }

        public BigDecimal getBcwp() { return bcwp; }
        public void setBcwp(BigDecimal bcwp) { this.bcwp = bcwp; }

        public BigDecimal getAcwp() { return acwp; }
        public void setAcwp(BigDecimal acwp) { this.acwp = acwp; }

        public BigDecimal getSv() { return sv; }
        public void setSv(BigDecimal sv) { this.sv = sv; }

        public BigDecimal getCv() { return cv; }
        public void setCv(BigDecimal cv) { this.cv = cv; }
    }

    // -----------------------------------------------------------------------
    // Outer-class getters / setters
    // -----------------------------------------------------------------------

    public MonthlyEVReportDTO() { }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public java.time.LocalDate getAsOfDate() { return asOfDate; }
    public void setAsOfDate(java.time.LocalDate asOfDate) { this.asOfDate = asOfDate; }

    public BigDecimal getProjectBcws() { return projectBcws; }
    public void setProjectBcws(BigDecimal projectBcws) { this.projectBcws = projectBcws; }

    public BigDecimal getProjectBcwp() { return projectBcwp; }
    public void setProjectBcwp(BigDecimal projectBcwp) { this.projectBcwp = projectBcwp; }

    public BigDecimal getProjectAcwp() { return projectAcwp; }
    public void setProjectAcwp(BigDecimal projectAcwp) { this.projectAcwp = projectAcwp; }

    public BigDecimal getProjectSv() { return projectSv; }
    public void setProjectSv(BigDecimal projectSv) { this.projectSv = projectSv; }

    public BigDecimal getProjectCv() { return projectCv; }
    public void setProjectCv(BigDecimal projectCv) { this.projectCv = projectCv; }

    public List<WorkPackageEVEntry> getWorkPackages() { return workPackages; }
    public void setWorkPackages(List<WorkPackageEVEntry> workPackages) { this.workPackages = workPackages; }
}