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

import com.corejsf.DTO.EarnedValueReportDTO;
import com.corejsf.DTO.WorkPackageWeeklyDTO;
import com.corejsf.Entity.TimesheetStatus;
import com.corejsf.Entity.WorkPackage;

/**
 * Computes all EV metrics for a control-account and its child work packages.
 *
 * Metrics computed (standard EVM formulas):
 *
 *   BCWS  = Budgeted Cost of Work Scheduled
 *           Straight-line spread of BAC over the WP's active weeks,
 *           accumulated up to the report week-ending.
 *
 *   BCWP  = Budgeted Cost of Work Performed
 *           BAC × (percentComplete / 100)
 *           Distributed proportionally across BCWS-active weeks up to asOf.
 *
 *   ACWP  = Actual Cost of Work Performed
 *           Sum of (approved timesheet hours for this WP in each week)
 *           × laborGrade.chargeRate.
 *           Queried from the database per WP per week-ending.
 *
 *   SV    = BCWP − BCWS       
 *   CV    = BCWP − ACWP         
 *   CPI   = BCWP / ACWP          (1.0 when ACWP = 0)
 *   EAC   = BAC / CPI            
 *   VAC   = BAC − EAC            
 *
 * Project-level values are the sums of WP-level values (AC5).
 */
@Stateless
public class EarnedValueCalculationService {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

   

    /**
     * Calculates all EV metrics and returns a fully populated EarnedValueReportDTO.
     *
     * @param input aggregated input containing parent WP ID, asOf date,
     *              week-ending list, and child work packages
     * @return report with per-WP and project-level EV metrics
     */
    public EarnedValueReportDTO calculate(final EarnedValueAggregateInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input is required");
        }

        final List<LocalDate> weekEndings = input.getWeekEndings();
        final LocalDate asOf = input.getAsOfDate();
        final List<WorkPackage> children = input.getChildWorkPackages();
        final int weekCount = weekEndings.size();

        // ----------------------------------------------------------------
        // Step 1: fetch ACWP from approved timesheets for ALL child WPs
        // in one query, grouped by (wpId, weekEnding).
        // Result columns: [wpId (String), weekEnding (LocalDate), cost (BigDecimal)]
        // ----------------------------------------------------------------
        final List<Object[]> acwpRows = fetchAcwpRows(input.getParentWpId(), weekEndings);

        // Build lookup: wpId -> (weekEnding -> cost)
        final Map<String, Map<LocalDate, BigDecimal>> acwpByWpAndWeek =
                buildAcwpLookup(acwpRows);

        // ----------------------------------------------------------------
        // Step 2: initialise project-level per-week accumulators
        // ----------------------------------------------------------------
        final Map<Integer, BigDecimal> projBcwsByWeek  = initZeroMap(weekCount);
        final Map<Integer, BigDecimal> projBcwpByWeek  = initZeroMap(weekCount);
        final Map<Integer, BigDecimal> projAcwpByWeek  = initZeroMap(weekCount);

        // ----------------------------------------------------------------
        // Step 3: compute per-WP metrics
        // ----------------------------------------------------------------
        int fallbackIndex = 1;
        final List<WorkPackageWeeklyDTO> wpDtos = new ArrayList<>();

        for (final WorkPackage wp : children) {
            final WorkPackageWeeklyDTO dto = computeForChild(
                    wp, weekEndings, asOf, fallbackIndex,
                    acwpByWpAndWeek.getOrDefault(wp.getWpId(), new LinkedHashMap<>()));

            wpDtos.add(dto);
            fallbackIndex++;

            // Accumulate into project-level per-week maps
            for (int i = 1; i <= weekCount; i++) {
                projBcwsByWeek.merge(i, dto.getBcwsByWeek().get(i), BigDecimal::add);
                projBcwpByWeek.merge(i, dto.getBcwpByWeek().get(i), BigDecimal::add);
                projAcwpByWeek.merge(i, dto.getAcwpByWeek().get(i), BigDecimal::add);
            }
        }

        // ----------------------------------------------------------------
        // Step 4: derive project-level per-week SV and CV maps
        // ----------------------------------------------------------------
        final Map<Integer, BigDecimal> projSvByWeek = new LinkedHashMap<>();
        final Map<Integer, BigDecimal> projCvByWeek = new LinkedHashMap<>();
        for (int i = 1; i <= weekCount; i++) {
            final BigDecimal bcwp = projBcwpByWeek.get(i);
            final BigDecimal bcws = projBcwsByWeek.get(i);
            final BigDecimal acwp = projAcwpByWeek.get(i);
            projSvByWeek.put(i, bcwp.subtract(bcws).setScale(2, RoundingMode.HALF_UP));
            projCvByWeek.put(i, bcwp.subtract(acwp).setScale(2, RoundingMode.HALF_UP));
        }

        // ----------------------------------------------------------------
        // Step 5: project-level scalar totals and derived metrics
        // ----------------------------------------------------------------
        final BigDecimal projTotalBcws = sumMap(projBcwsByWeek);
        final BigDecimal projTotalBcwp = sumMap(projBcwpByWeek);
        final BigDecimal projTotalAcwp = sumMap(projAcwpByWeek);

        // Project BAC = sum of child WP BACs
        BigDecimal projBac = BigDecimal.ZERO;
        for (final WorkPackage wp : children) {
            projBac = projBac.add(nz(wp.getBac()));
        }
        projBac = projBac.setScale(2, RoundingMode.HALF_UP);

        final BigDecimal projSv  = projTotalBcwp.subtract(projTotalBcws)
                                                 .setScale(2, RoundingMode.HALF_UP);
        final BigDecimal projCv  = projTotalBcwp.subtract(projTotalAcwp)
                                                 .setScale(2, RoundingMode.HALF_UP);
        final BigDecimal projEac = computeEac(projBac, projTotalBcwp, projTotalAcwp);
        final BigDecimal projVac = projBac.subtract(projEac).setScale(2, RoundingMode.HALF_UP);

        // ----------------------------------------------------------------
        // Step 6: assemble the report DTO
        // ----------------------------------------------------------------
        final EarnedValueReportDTO report = new EarnedValueReportDTO();
        report.setParentWpId(input.getParentWpId());
        report.setWeekCount(weekCount);
        report.setBac(projBac);
        report.setWorkPackages(wpDtos);

        report.setTotalBcwsByWeek(projBcwsByWeek);
        report.setTotalBcwpByWeek(projBcwpByWeek);
        report.setTotalAcwpByWeek(projAcwpByWeek);
        report.setSvByWeek(projSvByWeek);
        report.setCvByWeek(projCvByWeek);

        report.setProjectTotalBcws(projTotalBcws);
        report.setProjectTotalBcwp(projTotalBcwp);
        report.setProjectTotalAcwp(projTotalAcwp);
        report.setProjectSv(projSv);
        report.setProjectCv(projCv);
        report.setProjectEac(projEac);
        report.setProjectVac(projVac);

        return report;
    }

    // -----------------------------------------------------------------------
    // Per-WP computation
    // -----------------------------------------------------------------------

    private WorkPackageWeeklyDTO computeForChild(
            final WorkPackage wp,
            final List<LocalDate> reportWeekEndings,
            final LocalDate asOfDate,
            final int fallbackIndex,
            final Map<LocalDate, BigDecimal> acwpPerWeek) {

        final BigDecimal bac = nz(wp.getBac());
        final BigDecimal pct = nz(wp.getPercentComplete()); // 0..100

        final LocalDate wpStart = (wp.getPlanStartDate() == null)
                ? reportWeekEndings.get(0) : wp.getPlanStartDate();
        final LocalDate wpEnd = (wp.getPlanEndDate() == null)
                ? reportWeekEndings.get(reportWeekEndings.size() - 1) : wp.getPlanEndDate();

        final LocalDate activeStartWe = toWeekEndingSunday(wpStart);
        final LocalDate activeEndWe   = toWeekEndingSunday(wpEnd);

        final Map<Integer, BigDecimal> bcws = initZeroMap(reportWeekEndings.size());
        final Map<Integer, BigDecimal> bcwp = initZeroMap(reportWeekEndings.size());
        final Map<Integer, BigDecimal> acwp = initZeroMap(reportWeekEndings.size());

        // Map ACWP fetched from DB into the week-index map
        for (int i = 0; i < reportWeekEndings.size(); i++) {
            final LocalDate we = reportWeekEndings.get(i);
            final BigDecimal cost = acwpPerWeek.getOrDefault(we,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            acwp.put(i + 1, cost);
        }

        // Determine which week indices are within the WP's active window
        final boolean[] active = new boolean[reportWeekEndings.size()];
        int activeWeeks = 0;
        for (int i = 0; i < reportWeekEndings.size(); i++) {
            final LocalDate we = reportWeekEndings.get(i);
            final boolean isActive = !we.isBefore(activeStartWe) && !we.isAfter(activeEndWe);
            active[i] = isActive;
            if (isActive) activeWeeks++;
        }

        allocateBcwsStraightLine(bac, activeWeeks, active, bcws);
        allocateBcwpFromPercentComplete(bac, pct, asOfDate, reportWeekEndings, bcws, bcwp);

        // Compute scalar totals
        final BigDecimal totalBcws = sumMap(bcws);
        final BigDecimal totalBcwp = sumMap(bcwp);
        final BigDecimal totalAcwp = sumMap(acwp);

        // SV = BCWP - BCWS 
        final BigDecimal sv = totalBcwp.subtract(totalBcws).setScale(2, RoundingMode.HALF_UP);

        // CV = BCWP - ACWP 
        final BigDecimal cv = totalBcwp.subtract(totalAcwp).setScale(2, RoundingMode.HALF_UP);

        // EAC = BAC / CPI  
        final BigDecimal eac = computeEac(bac, totalBcwp, totalAcwp);

        // VAC = BAC - EAC 
        final BigDecimal vac = bac.subtract(eac).setScale(2, RoundingMode.HALF_UP);

        // Build DTO
        final WorkPackageWeeklyDTO dto = new WorkPackageWeeklyDTO();
        dto.setWpId(wp.getWpId());
        dto.setNumber(extractNumber(wp.getWpId(), fallbackIndex));
        dto.setDescription(wp.getDescription());
        dto.setEvMethod("% Complete");
        dto.setType("WP");
        dto.setBac(bac);

        dto.setBcwsByWeek(bcws);
        dto.setBcwpByWeek(bcwp);
        dto.setAcwpByWeek(acwp);

        dto.setTotalBcws(totalBcws);
        dto.setTotalBcwp(totalBcwp);
        dto.setTotalAcwp(totalAcwp);

        dto.setSv(sv);
        dto.setCv(cv);
        dto.setEac(eac);
        dto.setVac(vac);

        return dto;
    }

    // -----------------------------------------------------------------------
    // ACWP — database query
    // -----------------------------------------------------------------------

    /**
     * Fetches actual cost of work performed for all child WPs of the given
     * parent, grouped by WP and week-ending.
     *
     * Only APPROVED timesheets are counted.
     *
     * Result row columns: [wpId (String), weekEnding (LocalDate), cost (BigDecimal)]
     */
    private List<Object[]> fetchAcwpRows(final String parentWpId,
                                          final List<LocalDate> weekEndings) {
        if (weekEndings.isEmpty()) {
            return new ArrayList<>();
        }

        final LocalDate minWeek = weekEndings.get(0);
        final LocalDate maxWeek = weekEndings.get(weekEndings.size() - 1);

        return em.createQuery(
                "SELECT tr.workPackage.wpId, " +
                "       tr.timesheet.weekEnding, " +
                "       SUM((tr.monday + tr.tuesday + tr.wednesday + tr.thursday + tr.friday " +
                "            + COALESCE(tr.saturday, 0) + tr.sunday) " +
                "           * tr.laborGrade.chargeRate) " +
                "FROM TimesheetRow tr " +
                "WHERE tr.workPackage.parentWorkPackage.wpId = :parentWpId " +
                "  AND tr.timesheet.timesheetStatus = :status " +
                "  AND tr.timesheet.weekEnding BETWEEN :minWeek AND :maxWeek " +
                "GROUP BY tr.workPackage.wpId, tr.timesheet.weekEnding",
                Object[].class)
                .setParameter("parentWpId", parentWpId)
                .setParameter("status", TimesheetStatus.APPROVED)
                .setParameter("minWeek", minWeek)
                .setParameter("maxWeek", maxWeek)
                .getResultList();
    }

    /**
     * Converts the flat ACWP query result into a nested map:
     *   wpId -> ( weekEnding -> cost )
     */
    private Map<String, Map<LocalDate, BigDecimal>> buildAcwpLookup(
            final List<Object[]> rows) {

        final Map<String, Map<LocalDate, BigDecimal>> lookup = new LinkedHashMap<>();
        for (final Object[] row : rows) {
            final String    wpId      = (String)    row[0];
            final LocalDate weekEnd   = (LocalDate) row[1];
            final BigDecimal cost     = row[2] == null
                    ? BigDecimal.ZERO
                    : ((BigDecimal) row[2]).setScale(2, RoundingMode.HALF_UP);

            lookup.computeIfAbsent(wpId, k -> new LinkedHashMap<>()).put(weekEnd, cost);
        }
        return lookup;
    }

    // -----------------------------------------------------------------------
    // BCWS straight-line allocation
    // -----------------------------------------------------------------------

    private void allocateBcwsStraightLine(final BigDecimal bac,
                                          final int activeWeeks,
                                          final boolean[] active,
                                          final Map<Integer, BigDecimal> bcwsByWeek) {

        if (activeWeeks <= 0 || bac.compareTo(BigDecimal.ZERO) <= 0) return;

        final BigDecimal weekly = bac.divide(new BigDecimal(activeWeeks), 2, RoundingMode.HALF_UP);

        BigDecimal assigned = BigDecimal.ZERO;
        int seen = 0;

        for (int idx = 0; idx < active.length; idx++) {
            if (!active[idx]) continue;

            seen++;
            BigDecimal val = weekly;

            if (seen == activeWeeks) {
                // Last active week absorbs any rounding residual
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
            // No scheduled work yet — drop the full earned value into the first non-zero week
            for (int i = 1; i <= reportWeekEndings.size(); i++) {
                if (bcwsByWeek.get(i).compareTo(BigDecimal.ZERO) > 0) {
                    bcwpByWeek.put(i, earnedTotal);
                    break;
                }
            }
            return;
        }

        BigDecimal assigned = BigDecimal.ZERO;
        int lastEarnWeek = -1;

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

        // Fix rounding residual in last earned week
        if (lastEarnWeek != -1) {
            final BigDecimal diff = earnedTotal.subtract(assigned);
            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                bcwpByWeek.put(lastEarnWeek,
                        bcwpByWeek.get(lastEarnWeek).add(diff));
            }
        }
    }

    // -----------------------------------------------------------------------
    // EVM formula helpers
    // -----------------------------------------------------------------------

    /**
     * EAC using CPI method: EAC = BAC / CPI where CPI = BCWP / ACWP.
     * When ACWP == 0 (no actuals charged yet), CPI defaults to 1.0,
     * making EAC == BAC.
     */
    private BigDecimal computeEac(final BigDecimal bac,
                                   final BigDecimal bcwp,
                                   final BigDecimal acwp) {

        if (bac.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (acwp.compareTo(BigDecimal.ZERO) == 0 ||
            bcwp.compareTo(BigDecimal.ZERO) == 0) {
            // No actuals or no earned value yet — EAC = BAC
            return bac.setScale(2, RoundingMode.HALF_UP);
        }
        // CPI = BCWP / ACWP;  EAC = BAC / CPI = BAC * ACWP / BCWP
        return bac.multiply(acwp)
                  .divide(bcwp, 2, RoundingMode.HALF_UP);
    }

    // -----------------------------------------------------------------------
    // Generic helpers
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
        } catch (Exception e) {
            return fallback;
        }
    }

    private BigDecimal nz(final BigDecimal v) {
        return (v == null) ? BigDecimal.ZERO : v;
    }
}