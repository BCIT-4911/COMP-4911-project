package com.corejsf.DTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Top-level response for the weekly EV report.
 *
 * Contains:
 *  - Per-WP breakdown via workPackages 
 *  - Per-week project totals for BCWS, BCWP, ACWP, SV, CV (timeline data).
 *  - Scalar project-level totals for all six EVM metrics.
 *
 * Formulas (standard EVM, consistent with WorkPackageWeeklyDTO):
 *   SV  = BCWP - BCWS
 *   CV  = BCWP - ACWP
 *   CPI = BCWP / ACWP   (default 1.0 when ACWP == 0)
 *   EAC = BAC / CPI
 *   VAC = BAC - EAC
 */
public class EarnedValueReportDTO {

    // -----------------------------------------------------------------------
    // Report data
    // -----------------------------------------------------------------------

    private int weekCount;

    private String selectedProjectId;
    private String parentWpId;

    private List<OptionDTO> projects;
    private List<OptionDTO> controlAccounts;

    private String controlAccountTitle;
    private String controlAccountManager;

    /** Total BAC across all child WPs. */
    private BigDecimal bac;

    // -----------------------------------------------------------------------
    // Per-WP breakdown (WP level)
    // Each WorkPackageWeeklyDTO carries its own sv/cv/eac/vac.
    // -----------------------------------------------------------------------

    private List<WorkPackageWeeklyDTO> workPackages;

    // -----------------------------------------------------------------------
    // Per-week project totals (weekIndex -> value) — for timeline charts
    // -----------------------------------------------------------------------

    private Map<Integer, BigDecimal> totalBcwsByWeek;
    private Map<Integer, BigDecimal> totalBcwpByWeek;
    private Map<Integer, BigDecimal> totalAcwpByWeek;

    /** SV by week: BCWP - BCWS per week index */
    private Map<Integer, BigDecimal> svByWeek;

    /** CV by week: BCWP - ACWP per week index */
    private Map<Integer, BigDecimal> cvByWeek;

    // -----------------------------------------------------------------------
    // Scalar project-level totals (project level)
    // These are the sums/derived values across all WPs and all weeks.
    // -----------------------------------------------------------------------

    /** Sum of totalBcws across all WPs. */
    private BigDecimal projectTotalBcws;

    /** Sum of totalBcwp across all WPs. */
    private BigDecimal projectTotalBcwp;

    /** Sum of totalAcwp across all WPs. */
    private BigDecimal projectTotalAcwp;

    /**
     * Project-level SV = projectTotalBcwp - projectTotalBcws  
     */
    private BigDecimal projectSv;

    /**
     * Project-level CV = projectTotalBcwp - projectTotalAcwp 
     */
    private BigDecimal projectCv;

    /**
     * Project-level EAC = BAC / CPI  where CPI = projectTotalBcwp / projectTotalAcwp
     */
    private BigDecimal projectEac;

    /**
     * Project-level VAC = BAC - projectEac 
     */
    private BigDecimal projectVac;

    public EarnedValueReportDTO() { }

    // --- getters / setters ---

    public int getWeekCount() { return weekCount; }
    public void setWeekCount(int weekCount) { this.weekCount = weekCount; }

    public String getSelectedProjectId() { return selectedProjectId; }
    public void setSelectedProjectId(String selectedProjectId) { this.selectedProjectId = selectedProjectId; }

    public String getParentWpId() { return parentWpId; }
    public void setParentWpId(String parentWpId) { this.parentWpId = parentWpId; }

    public List<OptionDTO> getProjects() { return projects; }
    public void setProjects(List<OptionDTO> projects) { this.projects = projects; }

    public List<OptionDTO> getControlAccounts() { return controlAccounts; }
    public void setControlAccounts(List<OptionDTO> controlAccounts) { this.controlAccounts = controlAccounts; }

    public String getControlAccountTitle() { return controlAccountTitle; }
    public void setControlAccountTitle(String controlAccountTitle) { this.controlAccountTitle = controlAccountTitle; }

    public String getControlAccountManager() { return controlAccountManager; }
    public void setControlAccountManager(String controlAccountManager) { this.controlAccountManager = controlAccountManager; }

    public BigDecimal getBac() { return bac; }
    public void setBac(BigDecimal bac) { this.bac = bac; }

    public List<WorkPackageWeeklyDTO> getWorkPackages() { return workPackages; }
    public void setWorkPackages(List<WorkPackageWeeklyDTO> workPackages) { this.workPackages = workPackages; }

    public Map<Integer, BigDecimal> getTotalBcwsByWeek() { return totalBcwsByWeek; }
    public void setTotalBcwsByWeek(Map<Integer, BigDecimal> totalBcwsByWeek) { this.totalBcwsByWeek = totalBcwsByWeek; }

    public Map<Integer, BigDecimal> getTotalBcwpByWeek() { return totalBcwpByWeek; }
    public void setTotalBcwpByWeek(Map<Integer, BigDecimal> totalBcwpByWeek) { this.totalBcwpByWeek = totalBcwpByWeek; }

    public Map<Integer, BigDecimal> getTotalAcwpByWeek() { return totalAcwpByWeek; }
    public void setTotalAcwpByWeek(Map<Integer, BigDecimal> totalAcwpByWeek) { this.totalAcwpByWeek = totalAcwpByWeek; }

    public Map<Integer, BigDecimal> getSvByWeek() { return svByWeek; }
    public void setSvByWeek(Map<Integer, BigDecimal> svByWeek) { this.svByWeek = svByWeek; }

    public Map<Integer, BigDecimal> getCvByWeek() { return cvByWeek; }
    public void setCvByWeek(Map<Integer, BigDecimal> cvByWeek) { this.cvByWeek = cvByWeek; }

    public BigDecimal getProjectTotalBcws() { return projectTotalBcws; }
    public void setProjectTotalBcws(BigDecimal projectTotalBcws) { this.projectTotalBcws = projectTotalBcws; }

    public BigDecimal getProjectTotalBcwp() { return projectTotalBcwp; }
    public void setProjectTotalBcwp(BigDecimal projectTotalBcwp) { this.projectTotalBcwp = projectTotalBcwp; }

    public BigDecimal getProjectTotalAcwp() { return projectTotalAcwp; }
    public void setProjectTotalAcwp(BigDecimal projectTotalAcwp) { this.projectTotalAcwp = projectTotalAcwp; }

    public BigDecimal getProjectSv() { return projectSv; }
    public void setProjectSv(BigDecimal projectSv) { this.projectSv = projectSv; }

    public BigDecimal getProjectCv() { return projectCv; }
    public void setProjectCv(BigDecimal projectCv) { this.projectCv = projectCv; }

    public BigDecimal getProjectEac() { return projectEac; }
    public void setProjectEac(BigDecimal projectEac) { this.projectEac = projectEac; }

    public BigDecimal getProjectVac() { return projectVac; }
    public void setProjectVac(BigDecimal projectVac) { this.projectVac = projectVac; }
}