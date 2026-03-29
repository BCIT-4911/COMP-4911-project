package com.corejsf.DTO;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Per-work-package earned value data for the weekly EV report.
 *
 * BCWS, BCWP, ACWP are carried as per-week maps (weekIndex -> dollars)
 * so the frontend can render a timeline.
 *
 * SV, CV, EAC, VAC are single scalar values computed from the totals
 * (they do not have per-week breakdowns because they are point-in-time
 * metrics that only make sense relative to the asOf date, not per week).
 *
 * Formulas used (standard EVM):
 *   SV  = BCWP - BCWS
 *   CV  = BCWP - ACWP
 *   CPI = BCWP / ACWP  (if ACWP == 0, treat CPI as 1.0 to avoid div-by-zero)
 *   EAC = BAC / CPI    (cost-performance-index method)
 *   VAC = BAC - EAC
 */
public class WorkPackageWeeklyDTO {

    private String wpId;

    // For display in the table
    private int number;              // WP#
    private String description;      // Work Description
    private String evMethod;         // Earned Value Method (0/100, 50/50, % Complete, etc.)
    private String type;             // Type column (optional)

    // weekIndex -> dollars
    private Map<Integer, BigDecimal> bcwsByWeek;
    private Map<Integer, BigDecimal> bcwpByWeek;
    private Map<Integer, BigDecimal> acwpByWeek;

    // Totals (so frontend doesn't have to sum)
    private BigDecimal totalBcws;
    private BigDecimal totalBcwp;
    private BigDecimal totalAcwp;

    // -----------------------------------------------------------------------
    // AC1 — SV = BCWP - BCWS
    // AC2 — CV = BCWP - ACWP
    // AC3 — EAC = BAC / CPI  (CPI = BCWP / ACWP)
    // AC4 — VAC = BAC - EAC
    // AC5 — exposed at WP level here; project-level is in EarnedValueReportDTO
    // -----------------------------------------------------------------------

    /** Schedule Variance: totalBcwp - totalBcws */
    private BigDecimal sv;

    /** Cost Variance: totalBcwp - totalAcwp */
    private BigDecimal cv;

    /**
     * Estimate at Completion.
     * Method: BAC / CPI where CPI = BCWP / ACWP.
     * When ACWP == 0 (no actuals yet), EAC defaults to BAC (CPI assumed 1.0).
     */
    private BigDecimal eac;

    /** Variance at Completion: BAC - EAC */
    private BigDecimal vac;

    /** Budget at Completion — included so front-end can display BAC without a separate call. */
    private BigDecimal bac;

    public WorkPackageWeeklyDTO() { }

    // --- getters / setters ---

    public String getWpId() { return wpId; }
    public void setWpId(String wpId) { this.wpId = wpId; }

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEvMethod() { return evMethod; }
    public void setEvMethod(String evMethod) { this.evMethod = evMethod; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Map<Integer, BigDecimal> getBcwsByWeek() { return bcwsByWeek; }
    public void setBcwsByWeek(Map<Integer, BigDecimal> bcwsByWeek) { this.bcwsByWeek = bcwsByWeek; }

    public Map<Integer, BigDecimal> getBcwpByWeek() { return bcwpByWeek; }
    public void setBcwpByWeek(Map<Integer, BigDecimal> bcwpByWeek) { this.bcwpByWeek = bcwpByWeek; }

    public Map<Integer, BigDecimal> getAcwpByWeek() { return acwpByWeek; }
    public void setAcwpByWeek(Map<Integer, BigDecimal> acwpByWeek) { this.acwpByWeek = acwpByWeek; }

    public BigDecimal getTotalBcws() { return totalBcws; }
    public void setTotalBcws(BigDecimal totalBcws) { this.totalBcws = totalBcws; }

    public BigDecimal getTotalBcwp() { return totalBcwp; }
    public void setTotalBcwp(BigDecimal totalBcwp) { this.totalBcwp = totalBcwp; }

    public BigDecimal getTotalAcwp() { return totalAcwp; }
    public void setTotalAcwp(BigDecimal totalAcwp) { this.totalAcwp = totalAcwp; }

    public BigDecimal getSv() { return sv; }
    public void setSv(BigDecimal sv) { this.sv = sv; }

    public BigDecimal getCv() { return cv; }
    public void setCv(BigDecimal cv) { this.cv = cv; }

    public BigDecimal getEac() { return eac; }
    public void setEac(BigDecimal eac) { this.eac = eac; }

    public BigDecimal getVac() { return vac; }
    public void setVac(BigDecimal vac) { this.vac = vac; }

    public BigDecimal getBac() { return bac; }
    public void setBac(BigDecimal bac) { this.bac = bac; }
}