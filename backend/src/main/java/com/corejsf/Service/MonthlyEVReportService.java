package com.corejsf.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.NotFoundException;

import com.corejsf.DTO.MonthlyEVReportDTO;
import com.corejsf.DTO.MonthlyEVReportDTO.WorkPackageEVEntry;
import com.corejsf.Entity.Project;
import com.corejsf.Entity.TimesheetStatus;
import com.corejsf.Entity.WorkPackage;
import com.corejsf.Entity.WorkPackageType;

/**
 * Computes the Monthly Earned Value Report for a given project.
 *
 * Project-level totals are the sums of the WP-level values.
 *
 * Only LOWEST_LEVEL work packages contribute to the report (SUMMARY WPs
 * are containers; charging happens at the lowest level).  If a project has
 * no LOWEST_LEVEL WPs the report is still returned but the lists are empty.
 */
@Stateless
public class MonthlyEVReportService {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;



    /**
     * Generates the monthly EV report for the given project as of today.
     *
     * @param projectId the project to report on
     * @return fully populated MonthlyEVReportDTO
     * @throws NotFoundException if the project does not exist
     */
    public MonthlyEVReportDTO generateReport(final String projectId) {
        return generateReport(projectId, LocalDate.now());
    }

    /**
     * Generates the monthly EV report for the given project as of a specific date.
     * Exposed separately so callers (and tests) can pass an explicit asOf date.
     *
     * @param projectId the project to report on
     * @param asOfDate  data is computed up to and including this date's week-ending Sunday
     * @return fully populated MonthlyEVReportDTO
     * @throws NotFoundException if the project does not exist
     * @throws IllegalArgumentException if projectId or asOfDate is null
     */
    public MonthlyEVReportDTO generateReport(final String projectId, final LocalDate asOfDate) {

        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("projectId is required");
        }
        if (asOfDate == null) {
            throw new IllegalArgumentException("asOfDate is required");
        }

        // 1. Load project
        final Project project = em.find(Project.class, projectId);
        if (project == null) {
            throw new NotFoundException("Project not found: " + projectId);
        }

        // 2. Load only LOWEST_LEVEL work packages for this project
        final List<WorkPackage> wps = em.createQuery(
                "SELECT w FROM WorkPackage w " +
                "WHERE w.project.projId = :projId " +
                "  AND w.wpType = :type " +
                "ORDER BY w.wpId",
                WorkPackage.class)
                .setParameter("projId", projectId)
                .setParameter("type", WorkPackageType.LOWEST_LEVEL)
                .getResultList();

        // 3. Normalise the asOf boundary to the containing week-ending Sunday
        final LocalDate asOfWeSunday = toWeekEndingSunday(asOfDate);

        // 4. Compute ACWP for all relevant WPs in one query
        //    Result columns: [wpId (String), totalCost (BigDecimal)]
        final List<Object[]> acwpRows = em.createQuery(
                "SELECT tr.workPackage.wpId, " +
                "       SUM((tr.monday + tr.tuesday + tr.wednesday + tr.thursday + tr.friday " +
                "            + COALESCE(tr.saturday, 0) + tr.sunday) " +
                "           * tr.laborGrade.chargeRate) " +
                "FROM TimesheetRow tr " +
                "WHERE tr.workPackage.project.projId = :projId " +
                "  AND tr.timesheet.timesheetStatus  = :status " +
                "  AND tr.timesheet.weekEnding       <= :asOf " +
                "GROUP BY tr.workPackage.wpId",
                Object[].class)
                .setParameter("projId", projectId)
                .setParameter("status", TimesheetStatus.APPROVED)
                .setParameter("asOf", asOfWeSunday)
                .getResultList();

        // Build a wpId -> acwp lookup so we don't loop the DB result repeatedly
        final java.util.Map<String, BigDecimal> acwpByWp = new java.util.HashMap<>();
        for (final Object[] row : acwpRows) {
            final String wpId = (String) row[0];
            final BigDecimal cost = row[1] == null
                    ? BigDecimal.ZERO
                    : ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP);
            acwpByWp.put(wpId, cost);
        }

        // 5. Compute per-WP BCWS and BCWP, build entries
        final List<WorkPackageEVEntry> entries = new ArrayList<>();

        BigDecimal totalBcws = BigDecimal.ZERO;
        BigDecimal totalBcwp = BigDecimal.ZERO;
        BigDecimal totalAcwp = BigDecimal.ZERO;

        for (final WorkPackage wp : wps) {
            final BigDecimal bac  = nz(wp.getBac());
            final BigDecimal pct  = nz(wp.getPercentComplete()); // stored as 0–100

            final BigDecimal bcws = computeBcws(wp, bac, asOfWeSunday);
            final BigDecimal bcwp = bac.multiply(pct)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            final BigDecimal acwp = acwpByWp.getOrDefault(wp.getWpId(),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

            final WorkPackageEVEntry entry = new WorkPackageEVEntry();
            entry.setWpId(wp.getWpId());
            entry.setWpName(wp.getWpName());
            entry.setBcws(bcws);
            entry.setBcwp(bcwp);
            entry.setAcwp(acwp);
            entry.setSv(bcwp.subtract(bcws).setScale(2, RoundingMode.HALF_UP));
            entry.setCv(bcwp.subtract(acwp).setScale(2, RoundingMode.HALF_UP));

            entries.add(entry);

            totalBcws = totalBcws.add(bcws);
            totalBcwp = totalBcwp.add(bcwp);
            totalAcwp = totalAcwp.add(acwp);
        }

        // 6. Assemble the top-level DTO
        final MonthlyEVReportDTO dto = new MonthlyEVReportDTO();
        dto.setProjectId(projectId);
        dto.setProjectName(project.getProjName());
        dto.setAsOfDate(asOfDate);
        dto.setWorkPackages(entries);

        dto.setProjectBcws(totalBcws.setScale(2, RoundingMode.HALF_UP));
        dto.setProjectBcwp(totalBcwp.setScale(2, RoundingMode.HALF_UP));
        dto.setProjectAcwp(totalAcwp.setScale(2, RoundingMode.HALF_UP));
        dto.setProjectSv(totalBcwp.subtract(totalBcws).setScale(2, RoundingMode.HALF_UP));
        dto.setProjectCv(totalBcwp.subtract(totalAcwp).setScale(2, RoundingMode.HALF_UP));

        return dto;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Computes BCWS for a WP as of the given week-ending Sunday, using straight-line allocation of BAC over the WP's active weeks.
     */
    private BigDecimal computeBcws(final WorkPackage wp,
                                   final BigDecimal bac,
                                   final LocalDate asOfWeSunday) {

        if (bac.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Determine the WP's active date range
        final LocalDate wpStart = wp.getPlanStartDate() != null ? wp.getPlanStartDate() : asOfWeSunday;
        final LocalDate wpEnd   = wp.getPlanEndDate()   != null ? wp.getPlanEndDate()   : asOfWeSunday;

        final LocalDate activeStartWe = toWeekEndingSunday(wpStart);
        final LocalDate activeEndWe   = toWeekEndingSunday(wpEnd);

        // Count total active weeks
        int totalActiveWeeks = 0;
        LocalDate cursor = activeStartWe;
        while (!cursor.isAfter(activeEndWe)) {
            totalActiveWeeks++;
            cursor = cursor.plusWeeks(1);
        }

        if (totalActiveWeeks == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Weekly straight-line allocation
        final BigDecimal weeklyRate = bac.divide(
                new BigDecimal(totalActiveWeeks), 8, RoundingMode.HALF_UP);

        // Accumulate weeks up to asOfWeSunday, with last-week rounding correction
        BigDecimal accumulated = BigDecimal.ZERO;
        int weeksProcessed = 0;

        cursor = activeStartWe;
        while (!cursor.isAfter(activeEndWe) && !cursor.isAfter(asOfWeSunday)) {
            weeksProcessed++;

            // On the last active week, assign remainder to avoid rounding drift
            if (weeksProcessed == totalActiveWeeks) {
                accumulated = bac; // entire BAC is scheduled by end
                break;
            }

            accumulated = accumulated.add(weeklyRate);
            cursor = cursor.plusWeeks(1);
        }

        // If Sunday falls before the last active week we just add weeksProcessed * weeklyRate
        // (the loop above already did that via accumulated.add(weeklyRate) each iteration)

        return accumulated.setScale(2, RoundingMode.HALF_UP);
    }

    /** Adjusts any date to the next-or-same Sunday (week-ending convention). */
    private LocalDate toWeekEndingSunday(final LocalDate date) {
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    /** Null-safe BigDecimal coercion — treats null as zero. */
    private BigDecimal nz(final BigDecimal v) {
        return (v == null) ? BigDecimal.ZERO : v;
    }
}