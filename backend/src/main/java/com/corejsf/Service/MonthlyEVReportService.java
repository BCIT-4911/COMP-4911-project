package com.corejsf.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.NotFoundException;

import com.corejsf.DTO.MonthlyEVReportDTO;
import com.corejsf.DTO.MonthlyEVReportDTO.WorkPackageEVEntry;
import com.corejsf.DTO.WorkPackageMonthlyPerformanceDTO;
import com.corejsf.Entity.Project;
import com.corejsf.Entity.TimesheetStatus;
import com.corejsf.Entity.WorkPackage;
import com.corejsf.Entity.WorkPackageType;
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

    @Inject
    private RateHistoryService rateHistoryService;



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
        final List<com.corejsf.Entity.TimesheetRow> approvedRows = em.createQuery(
                "SELECT tr " +
                "FROM TimesheetRow tr " +
                "WHERE tr.workPackage.project.projId = :projId " +
                "  AND tr.timesheet.timesheetStatus = :status " +
                "  AND tr.timesheet.weekEnding <= :asOf",
                com.corejsf.Entity.TimesheetRow.class)
                .setParameter("projId", projectId)
                .setParameter("status", TimesheetStatus.APPROVED)
                .setParameter("asOf", asOfWeSunday)
                .getResultList();

        // Build a wpId -> acwp lookup so we don't loop the DB result repeatedly
        final Map<String, BigDecimal> acwpByWp = new HashMap<>();
        for (final com.corejsf.Entity.TimesheetRow row : approvedRows) {
            final String wpId = row.getWorkPackage().getWpId();
            final LocalDate weekEnding = row.getTimesheet().getWeekEnding();
            final BigDecimal effectiveRate = rateHistoryService.getEffectiveRate(row.getLaborGrade(), weekEnding);
            final BigDecimal cost = rowHours(row).multiply(effectiveRate).setScale(2, RoundingMode.HALF_UP);
            acwpByWp.merge(wpId, cost, BigDecimal::add);
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
            entry.setBac(bac);
            entry.setEtc(nz(wp.getEtc()).setScale(2, RoundingMode.HALF_UP));
            entry.setBcws(bcws);
            entry.setBcwp(bcwp);
            entry.setAcwp(acwp);
            entry.setSv(bcwp.subtract(bcws).setScale(2, RoundingMode.HALF_UP));
            entry.setCv(bcwp.subtract(acwp).setScale(2, RoundingMode.HALF_UP));
            final BigDecimal eac = computeEac(bac, bcwp, acwp);
            entry.setEac(eac);
            entry.setVac(bac.subtract(eac).setScale(2, RoundingMode.HALF_UP));

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
        final BigDecimal projectBac = wps.stream()
                .map(WorkPackage::getBac)
                .map(this::nz)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        dto.setProjectBac(projectBac);
        final BigDecimal projectEac = computeEac(projectBac, totalBcwp, totalAcwp);
        dto.setProjectEac(projectEac);
        dto.setProjectVac(projectBac.subtract(projectEac).setScale(2, RoundingMode.HALF_UP));

        return dto;
    }

    public WorkPackageMonthlyPerformanceDTO generateWorkPackageMonthlyPerformance(
            final String wpId,
            final LocalDate asOfDate) {

        if (wpId == null || wpId.isBlank()) {
            throw new IllegalArgumentException("wpId is required");
        }
        if (asOfDate == null) {
            throw new IllegalArgumentException("asOfDate is required");
        }

        final WorkPackage wp = em.find(WorkPackage.class, wpId);
        if (wp == null) {
            throw new NotFoundException("Work package not found: " + wpId);
        }
        if (wp.getWpType() != WorkPackageType.LOWEST_LEVEL) {
            throw new IllegalArgumentException("Monthly performance is only available for lowest-level work packages.");
        }

        final BigDecimal bac = nz(wp.getBac()).setScale(2, RoundingMode.HALF_UP);
        final BigDecimal percentComplete = nz(wp.getPercentComplete());
        final BigDecimal earnedTotal = bac.multiply(percentComplete)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        final LocalDate activeStartWe = toWeekEndingSunday(
                wp.getPlanStartDate() != null ? wp.getPlanStartDate() : asOfDate);
        final LocalDate activeEndWe = toWeekEndingSunday(
                wp.getPlanEndDate() != null ? wp.getPlanEndDate() : asOfDate);
        final LocalDate asOfWeSunday = toWeekEndingSunday(asOfDate);
        final LocalDate reportEndWe = asOfWeSunday.isBefore(activeEndWe) ? asOfWeSunday : activeEndWe;

        final List<LocalDate> weekEndings = new ArrayList<>();
        LocalDate cursor = activeStartWe;
        while (!cursor.isAfter(reportEndWe)) {
            weekEndings.add(cursor);
            cursor = cursor.plusWeeks(1);
        }

        final Map<LocalDate, BigDecimal> bcwsByWeek = new LinkedHashMap<>();
        final Map<LocalDate, BigDecimal> bcwpByWeek = new LinkedHashMap<>();
        final Map<LocalDate, BigDecimal> acwpByWeek = initWeeklyMap(weekEndings);

        final int activeWeeks = weekEndings.size();
        if (activeWeeks > 0 && bac.compareTo(BigDecimal.ZERO) > 0) {
            final BigDecimal weeklyPlan = bac.divide(new BigDecimal(activeWeeks), 2, RoundingMode.HALF_UP);
            BigDecimal assigned = BigDecimal.ZERO;
            for (int i = 0; i < weekEndings.size(); i++) {
                BigDecimal value = weeklyPlan;
                if (i == weekEndings.size() - 1) {
                    value = bac.subtract(assigned);
                }
                assigned = assigned.add(value);
                bcwsByWeek.put(weekEndings.get(i), value);
            }
            allocateWeeklyBcwp(weekEndings, asOfWeSunday, bcwsByWeek, bcwpByWeek, earnedTotal);
        }

        final List<com.corejsf.Entity.TimesheetRow> approvedRows = em.createQuery(
                "SELECT tr " +
                "FROM TimesheetRow tr " +
                "WHERE tr.workPackage.wpId = :wpId " +
                "  AND tr.timesheet.timesheetStatus = :status " +
                "  AND tr.timesheet.weekEnding <= :asOf",
                com.corejsf.Entity.TimesheetRow.class)
                .setParameter("wpId", wpId)
                .setParameter("status", TimesheetStatus.APPROVED)
                .setParameter("asOf", asOfWeSunday)
                .getResultList();

        for (final com.corejsf.Entity.TimesheetRow row : approvedRows) {
            final LocalDate weekEnding = row.getTimesheet().getWeekEnding();
            final BigDecimal effectiveRate = rateHistoryService.getEffectiveRate(row.getLaborGrade(), weekEnding);
            final BigDecimal cost = rowHours(row).multiply(effectiveRate).setScale(2, RoundingMode.HALF_UP);
            acwpByWeek.merge(weekEnding, cost, BigDecimal::add);
        }

        final LinkedHashMap<YearMonth, BigDecimal> bcwsMonthly = initMonthlyMap(weekEndings);
        final LinkedHashMap<YearMonth, BigDecimal> bcwpMonthly = initMonthlyMap(weekEndings);
        final LinkedHashMap<YearMonth, BigDecimal> acwpMonthly = initMonthlyMap(weekEndings);

        for (final LocalDate weekEnding : weekEndings) {
            final YearMonth month = YearMonth.from(weekEnding);
            bcwsMonthly.merge(month, bcwsByWeek.getOrDefault(weekEnding, BigDecimal.ZERO), BigDecimal::add);
            bcwpMonthly.merge(month, bcwpByWeek.getOrDefault(weekEnding, BigDecimal.ZERO), BigDecimal::add);
            acwpMonthly.merge(month, acwpByWeek.getOrDefault(weekEnding, BigDecimal.ZERO), BigDecimal::add);
        }

        final List<String> months = new ArrayList<>();
        final List<BigDecimal> bcwsByMonth = new ArrayList<>();
        final List<BigDecimal> bcwpByMonth = new ArrayList<>();
        final List<BigDecimal> acwpByMonth = new ArrayList<>();
        final List<BigDecimal> svByMonth = new ArrayList<>();
        final List<BigDecimal> cvByMonth = new ArrayList<>();

        for (final YearMonth month : bcwsMonthly.keySet()) {
            final BigDecimal bcws = scale2(bcwsMonthly.get(month));
            final BigDecimal bcwp = scale2(bcwpMonthly.get(month));
            final BigDecimal acwp = scale2(acwpMonthly.get(month));
            months.add(month.toString());
            bcwsByMonth.add(bcws);
            bcwpByMonth.add(bcwp);
            acwpByMonth.add(acwp);
            svByMonth.add(scale2(bcwp.subtract(bcws)));
            cvByMonth.add(scale2(bcwp.subtract(acwp)));
        }

        final BigDecimal totalAcwp = acwpByMonth.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        final BigDecimal eac = computeEac(bac, earnedTotal, totalAcwp);

        final WorkPackageMonthlyPerformanceDTO dto = new WorkPackageMonthlyPerformanceDTO();
        dto.setWpId(wp.getWpId());
        dto.setWpName(wp.getWpName());
        dto.setProjectId(wp.getProject().getProjId());
        dto.setAsOfDate(asOfDate);
        dto.setBac(bac);
        dto.setEtc(scale2(wp.getEtc()));
        dto.setEac(eac);
        dto.setVac(scale2(bac.subtract(eac)));
        dto.setMonths(months);
        dto.setBcwsByMonth(bcwsByMonth);
        dto.setBcwpByMonth(bcwpByMonth);
        dto.setAcwpByMonth(acwpByMonth);
        dto.setSvByMonth(svByMonth);
        dto.setCvByMonth(cvByMonth);
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

    private BigDecimal computeEac(final BigDecimal bac,
                                  final BigDecimal bcwp,
                                  final BigDecimal acwp) {
        if (bac.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (acwp.compareTo(BigDecimal.ZERO) == 0 ||
                bcwp.compareTo(BigDecimal.ZERO) == 0) {
            return bac.setScale(2, RoundingMode.HALF_UP);
        }
        return bac.multiply(acwp)
                .divide(bcwp, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal rowHours(final com.corejsf.Entity.TimesheetRow row) {
        return nz(row.getMonday())
                .add(nz(row.getTuesday()))
                .add(nz(row.getWednesday()))
                .add(nz(row.getThursday()))
                .add(nz(row.getFriday()))
                .add(nz(row.getSaturday()))
                .add(nz(row.getSunday()));
    }

    private Map<LocalDate, BigDecimal> initWeeklyMap(final List<LocalDate> weekEndings) {
        final Map<LocalDate, BigDecimal> map = new LinkedHashMap<>();
        for (final LocalDate weekEnding : weekEndings) {
            map.put(weekEnding, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
        return map;
    }

    private LinkedHashMap<YearMonth, BigDecimal> initMonthlyMap(final List<LocalDate> weekEndings) {
        final LinkedHashMap<YearMonth, BigDecimal> map = new LinkedHashMap<>();
        for (final LocalDate weekEnding : weekEndings) {
            map.putIfAbsent(YearMonth.from(weekEnding), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
        return map;
    }

    private void allocateWeeklyBcwp(final List<LocalDate> weekEndings,
                                    final LocalDate asOfWeSunday,
                                    final Map<LocalDate, BigDecimal> bcwsByWeek,
                                    final Map<LocalDate, BigDecimal> bcwpByWeek,
                                    final BigDecimal earnedTotal) {
        BigDecimal bcwsToDate = BigDecimal.ZERO;
        for (final LocalDate weekEnding : weekEndings) {
            if (!weekEnding.isAfter(asOfWeSunday)) {
                bcwsToDate = bcwsToDate.add(bcwsByWeek.getOrDefault(weekEnding, BigDecimal.ZERO));
            }
        }

        if (bcwsToDate.compareTo(BigDecimal.ZERO) == 0) {
            for (final LocalDate weekEnding : weekEndings) {
                final BigDecimal plan = bcwsByWeek.getOrDefault(weekEnding, BigDecimal.ZERO);
                if (plan.compareTo(BigDecimal.ZERO) > 0) {
                    bcwpByWeek.put(weekEnding, earnedTotal);
                    return;
                }
            }
            return;
        }

        BigDecimal assigned = BigDecimal.ZERO;
        LocalDate lastEarnWeek = null;
        for (final LocalDate weekEnding : weekEndings) {
            if (weekEnding.isAfter(asOfWeSunday)) {
                continue;
            }
            final BigDecimal plan = bcwsByWeek.getOrDefault(weekEnding, BigDecimal.ZERO);
            if (plan.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            lastEarnWeek = weekEnding;
            final BigDecimal portion = plan.divide(bcwsToDate, 8, RoundingMode.HALF_UP);
            final BigDecimal earned = earnedTotal.multiply(portion).setScale(2, RoundingMode.HALF_UP);
            assigned = assigned.add(earned);
            bcwpByWeek.put(weekEnding, earned);
        }

        if (lastEarnWeek != null) {
            final BigDecimal diff = earnedTotal.subtract(assigned);
            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                bcwpByWeek.put(lastEarnWeek, scale2(bcwpByWeek.getOrDefault(lastEarnWeek, BigDecimal.ZERO).add(diff)));
            }
        }
    }

    private BigDecimal scale2(final BigDecimal value) {
        return nz(value).setScale(2, RoundingMode.HALF_UP);
    }
}
