package com.corejsf.Service;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.corejsf.DTO.WorkPackageWeeklyDTO;
import com.corejsf.Entity.TimesheetStatus;
import com.corejsf.Entity.WorkPackage;

/**
 * Computes BCWS, BCWP, and ACWP for each child work package of a
 * control-account parent WP.
 *
 * ACWP calculation rules:
 *   - Only rows from APPROVED timesheets are counted.
 *   - DRAFT, SUBMITTED, and RETURNED timesheets are excluded entirely.
 *   - Cost per row = (Mon + Tue + Wed + Thu + Fri + COALESCE(Sat,0) + Sun)
 *                    * laborGrade.chargeRate
 *   - Rows are grouped by (wpId, weekEnding) and placed into the matching
 *     week-index slot of the report's week-ending list.
 *
 * ACWP at WP level:
 *   - acwpByWeek map on WorkPackageWeeklyDTO: weekIndex -> cost for that week
 *   - totalAcwp on WorkPackageWeeklyDTO: sum across all weeks
 *
 * ACWP at project level:
 *   - totalAcwpByWeek on EarnedValueReportDTO: weekIndex -> sum across all WPs
 *   - returned from EarnedValueResource alongside the per-WP breakdown
 */
@Stateless
public class EarnedValueCalculationService {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    // -----------------------------------------------------------------------
    // Public API — returns per-WP DTOs; project-level rollup done in resource
    // -----------------------------------------------------------------------

    /**
     * Calculates BCWS, BCWP, and ACWP for every child WP in the given input.
     *
     * @param input aggregated input (parent WP id, asOf date, week endings, children)
     * @return list of WorkPackageWeeklyDTO, one per child WP, in the same order
     *         as input.getChildWorkPackages()
     */
    public List<WorkPackageWeeklyDTO> calculate(final EarnedValueAggregateInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input is required");
        }

        final List<LocalDate> weekEndings = input.getWeekEndings();
        final LocalDate asOf             = input.getAsOfDate();
        final List<WorkPackage> children = input.getChildWorkPackages();

        // ------------------------------------------------------------------
        // Step 1: fetch real ACWP for all child WPs in a single DB query.
        //
        // The query groups by (wpId, weekEnding) and sums
        //   (daily hours total) * laborGrade.chargeRate
        // filtering to APPROVED timesheets only (AC1, AC2).
        //
        // We scope to weekEndings within the report window to avoid pulling
        // data from outside the report period.
        // ------------------------------------------------------------------
        final Map<String, Map<LocalDate, BigDecimal>> acwpByWpAndWeek =
                fetchApprovedAcwp(input.getParentWpId(), weekEndings);

        // ------------------------------------------------------------------
        // Step 2: build one DTO per child WP
        // ------------------------------------------------------------------
        final List<WorkPackageWeeklyDTO> out = new ArrayList<>();
        int fallbackIndex = 1;

        for (final WorkPackage wp : children) {
            final Map<LocalDate, BigDecimal> wpAcwpPerWeek =
                    acwpByWpAndWeek.getOrDefault(wp.getWpId(), new LinkedHashMap<>());

            out.add(computeForChild(wp, weekEndings, asOf, fallbackIndex, wpAcwpPerWeek));
            fallbackIndex++;
        }

        return out;
    }

    // -----------------------------------------------------------------------
    // ACWP query — approved timesheets only (AC1, AC2)
    // -----------------------------------------------------------------------

    /**
     * Queries approved timesheet row costs for all direct children of the
     * given parent WP, grouped by WP id and week-ending date.
     *
     * Only rows whose parent timesheet has status = APPROVED are included.
     * DRAFT, SUBMITTED, and RETURNED rows contribute nothing (AC1, AC2).
     *
     * saturday is nullable in the schema; COALESCE handles nulls in the sum.
     *
     * @param parentWpId  the parent (control-account) WP id
     * @param weekEndings the ordered list of week-ending Sundays for the report
     * @return nested map: wpId -> ( weekEndingDate -> cost )
     */
    private Map<String, Map<LocalDate, BigDecimal>> fetchApprovedAcwp(
            final String parentWpId,
            final List<LocalDate> weekEndings) {

        if (weekEndings.isEmpty()) {
            return new LinkedHashMap<>();
        }

        final LocalDate minWeek = weekEndings.get(0);
        final LocalDate maxWeek = weekEndings.get(weekEndings.size() - 1);

        // Result columns: [wpId (String), weekEnding (LocalDate), cost (BigDecimal)]
        final List<Object[]> rows = em.createQuery(
                "SELECT tr.workPackage.wpId," +
                "       tr.timesheet.weekEnding," +
                "       SUM((tr.monday + tr.tuesday + tr.wednesday" +
                "            + tr.thursday + tr.friday" +
                "            + COALESCE(tr.saturday, 0) + tr.sunday)" +
                "           * tr.laborGrade.chargeRate)" +
                "  FROM TimesheetRow tr" +
                " WHERE tr.workPackage.parentWorkPackage.wpId = :parentWpId" +
                "   AND tr.timesheet.timesheetStatus = :approved" +
                "   AND tr.timesheet.weekEnding BETWEEN :minWeek AND :maxWeek" +
                " GROUP BY tr.workPackage.wpId, tr.timesheet.weekEnding",
                Object[].class)
                .setParameter("parentWpId", parentWpId)
                .setParameter("approved", TimesheetStatus.APPROVED)
                .setParameter("minWeek", minWeek)
                .setParameter("maxWeek", maxWeek)
                .getResultList();

        // Build nested lookup map
        final Map<String, Map<LocalDate, BigDecimal>> result = new LinkedHashMap<>();
        for (final Object[] row : rows) {
            final String     wpId    = (String)     row[0];
            final LocalDate  weekEnd = (LocalDate)  row[1];
            final BigDecimal cost    = row[2] == null
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : ((BigDecimal) row[2]).setScale(2, RoundingMode.HALF_UP);

            result.computeIfAbsent(wpId, k -> new LinkedHashMap<>())
                  .put(weekEnd, cost);
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Per-WP computation
    // -----------------------------------------------------------------------

    private WorkPackageWeeklyDTO computeForChild(
            final WorkPackage wp,
            final List<LocalDate> reportWeekEndings,
            final LocalDate asOfDate,
            final int fallbackIndex,
            final Map<LocalDate, BigDecimal> wpAcwpPerWeek) {

        final BigDecimal bac = nz(wp.getBac());
        final BigDecimal pct = nz(wp.getPercentComplete()); // stored as 0..100

        final LocalDate wpStart = wp.getPlanStartDate() != null
                ? wp.getPlanStartDate() : reportWeekEndings.get(0);
        final LocalDate wpEnd   = wp.getPlanEndDate() != null
                ? wp.getPlanEndDate() : reportWeekEndings.get(reportWeekEndings.size() - 1);

        final LocalDate activeStartWe = toWeekEndingSunday(wpStart);
        final LocalDate activeEndWe   = toWeekEndingSunday(wpEnd);

        final Map<Integer, BigDecimal> bcws = initZeroMap(reportWeekEndings.size());
        final Map<Integer, BigDecimal> bcwp = initZeroMap(reportWeekEndings.size());
        // ACWP: populated from approved timesheet data (AC1, AC2, AC3)
        final Map<Integer, BigDecimal> acwp = initZeroMap(reportWeekEndings.size());

        // Fill ACWP week-index map from the DB lookup
        for (int i = 0; i < reportWeekEndings.size(); i++) {
            final LocalDate we = reportWeekEndings.get(i);
            final BigDecimal cost = wpAcwpPerWeek.getOrDefault(
                    we, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            acwp.put(i + 1, cost);
        }

        // Determine active weeks for BCWS straight-line allocation
        final boolean[] active    = new boolean[reportWeekEndings.size()];
        int             activeWeeks = 0;
        for (int i = 0; i < reportWeekEndings.size(); i++) {
            final LocalDate we = reportWeekEndings.get(i);
            final boolean isActive = !we.isBefore(activeStartWe) && !we.isAfter(activeEndWe);
            active[i] = isActive;
            if (isActive) activeWeeks++;
        }

        allocateBcwsStraightLine(bac, activeWeeks, active, bcws);
        allocateBcwpFromPercentComplete(bac, pct, asOfDate, reportWeekEndings, bcws, bcwp);

        final WorkPackageWeeklyDTO dto = new WorkPackageWeeklyDTO();
        dto.setWpId(wp.getWpId());
        dto.setNumber(extractNumber(wp.getWpId(), fallbackIndex));
        dto.setDescription(wp.getDescription());
        dto.setEvMethod("% Complete");
        dto.setType("WP");

        dto.setBcwsByWeek(bcws);
        dto.setBcwpByWeek(bcwp);
        dto.setAcwpByWeek(acwp);                   // real values, not zeros

        dto.setTotalBcws(sumMap(bcws));
        dto.setTotalBcwp(sumMap(bcwp));
        dto.setTotalAcwp(sumMap(acwp));             // real total, not zero

        return dto;
    }

    // -----------------------------------------------------------------------
    // BCWS straight-line allocation
    // -----------------------------------------------------------------------

    private void allocateBcwsStraightLine(final BigDecimal bac,
                                          final int activeWeeks,
                                          final boolean[] active,
                                          final Map<Integer, BigDecimal> bcwsByWeek) {
        if (activeWeeks <= 0 || bac.compareTo(BigDecimal.ZERO) <= 0) return;

        final BigDecimal weekly  = bac.divide(new BigDecimal(activeWeeks), 2, RoundingMode.HALF_UP);
        BigDecimal       assigned = BigDecimal.ZERO;
        int              seen     = 0;

        for (int idx = 0; idx < active.length; idx++) {
            if (!active[idx]) continue;
            seen++;
            BigDecimal val = weekly;
            if (seen == activeWeeks) {
                val = bac.subtract(assigned);
            }
            assigned = assigned.add(val);
            bcwsByWeek.put(idx + 1, val);
        }
    }

    // -----------------------------------------------------------------------
    // BCWP allocation 
    // -----------------------------------------------------------------------

    private void allocateBcwpFromPercentComplete(final BigDecimal bac,
                                                  final BigDecimal pct,
                                                  final LocalDate asOfDate,
                                                  final List<LocalDate> reportWeekEndings,
                                                  final Map<Integer, BigDecimal> bcwsByWeek,
                                                  final Map<Integer, BigDecimal> bcwpByWeek) {
        if (bac.compareTo(BigDecimal.ZERO) <= 0) return;

        final BigDecimal earnedTotal = bac.multiply(pct)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        final LocalDate asOfWe = toWeekEndingSunday(asOfDate);

        BigDecimal bcwsToDate = BigDecimal.ZERO;
        for (int i = 1; i <= reportWeekEndings.size(); i++) {
            if (!reportWeekEndings.get(i - 1).isAfter(asOfWe)) {
                bcwsToDate = bcwsToDate.add(bcwsByWeek.get(i));
            }
        }

        if (bcwsToDate.compareTo(BigDecimal.ZERO) == 0) {
            for (int i = 1; i <= reportWeekEndings.size(); i++) {
                if (bcwsByWeek.get(i).compareTo(BigDecimal.ZERO) > 0) {
                    bcwpByWeek.put(i, earnedTotal);
                    break;
                }
            }
            return;
        }

        BigDecimal assigned    = BigDecimal.ZERO;
        int        lastEarnWeek = -1;

        for (int i = 1; i <= reportWeekEndings.size(); i++) {
            if (reportWeekEndings.get(i - 1).isAfter(asOfWe)) continue;
            final BigDecimal plan = bcwsByWeek.get(i);
            if (plan.compareTo(BigDecimal.ZERO) == 0) continue;

            lastEarnWeek = i;
            final BigDecimal portion = plan.divide(bcwsToDate, 8, RoundingMode.HALF_UP);
            final BigDecimal earned  = earnedTotal.multiply(portion).setScale(2, RoundingMode.HALF_UP);
            assigned = assigned.add(earned);
            bcwpByWeek.put(i, earned);
        }

        if (lastEarnWeek != -1) {
            final BigDecimal diff = earnedTotal.subtract(assigned);
            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                bcwpByWeek.put(lastEarnWeek, bcwpByWeek.get(lastEarnWeek).add(diff));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private LocalDate toWeekEndingSunday(final LocalDate date) {
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    private Map<Integer, BigDecimal> initZeroMap(final int size) {
        final Map<Integer, BigDecimal> m = new LinkedHashMap<>();
        for (int i = 1; i <= size; i++) {
            m.put(i, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
        return m;
    }

    private BigDecimal sumMap(final Map<Integer, BigDecimal> map) {
        BigDecimal total = BigDecimal.ZERO;
        for (final BigDecimal v : map.values()) total = total.add(v);
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private int extractNumber(final String wpId, final int fallback) {
        if (wpId == null) return fallback;
        try {
            final String digits = wpId.replaceAll("\\D", "");
            if (digits.isBlank()) return fallback;
            return Integer.parseInt(digits);
        } catch (final Exception e) {
            return fallback;
        }
    }

    private BigDecimal nz(final BigDecimal v) {
        return (v == null) ? BigDecimal.ZERO : v;
    }
}