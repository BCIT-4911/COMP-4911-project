package com.corejsf.api;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.corejsf.DTO.EarnedValueReportDTO;
import com.corejsf.DTO.WorkPackageWeeklyDTO;
import com.corejsf.Service.EarnedValueAggregateInput;
import com.corejsf.Service.EarnedValueAggregationService;
import com.corejsf.Service.EarnedValueCalculationService;

/**
 * REST resource for earned value data.
 *
 * Returns an EarnedValueReportDTO that includes:
 *   - Per-WP ACWP per week and total (WP-level, AC3)
 *   - Project-level ACWP rolled up across all child WPs (project-level, AC3)
 *
 * ACWP in both levels reflects only APPROVED timesheet rows (AC1, AC2) —
 * that filtering is enforced inside EarnedValueCalculationService.
 */
@Path("/earned-value")
@Produces(MediaType.APPLICATION_JSON)
public class EarnedValueResource {

    @Inject
    private EarnedValueAggregationService aggregationService;

    @Inject
    private EarnedValueCalculationService calculationService;

    /**
     * Returns the full EV report for a control-account parent WP.
     *
     * The response shape is EarnedValueReportDTO which includes:
     *   workPackages         — per-WP list, each with acwpByWeek and totalAcwp
     *   totalAcwpByWeek      — project-level ACWP per week index (sum across all WPs)
     *   totalBcwsByWeek      — project-level BCWS per week index
     *   totalBcwpByWeek      — project-level BCWP per week index
     *   weekCount            — number of week columns in the report
     *
     * Example calls:
     *   GET /api/earned-value?parentWpId=CA-1
     *   GET /api/earned-value?parentWpId=CA-1&asOf=2026-02-15
     *
     * @param parentWpId the parent (control-account) WP id — required
     * @param asOf       optional ISO-8601 date string; defaults to today
     * @return 200 EarnedValueReportDTO, or 400 when parentWpId is missing
     */
    @GET
    public Response getEVReport(@QueryParam("parentWpId") final String parentWpId,
                                @QueryParam("asOf")       final String asOf) {

        if (parentWpId == null || parentWpId.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("parentWpId is required")
                    .build();
        }

        final LocalDate asOfDate = (asOf == null || asOf.isBlank())
                ? null
                : LocalDate.parse(asOf);

        // Build the aggregation input (date range, week-ending list, child WPs)
        final EarnedValueAggregateInput input =
                aggregationService.aggregateForParent(parentWpId, asOfDate);

        // Calculate per-WP BCWS, BCWP, and real ACWP from approved timesheets
        final List<WorkPackageWeeklyDTO> wpDtos = calculationService.calculate(input);

        final int weekCount = input.getWeekEndings().size();

        // ------------------------------------------------------------------
        // Roll up per-WP values to project level
        // ------------------------------------------------------------------
        final Map<Integer, BigDecimal> projBcwsByWeek  = initZeroMap(weekCount);
        final Map<Integer, BigDecimal> projBcwpByWeek  = initZeroMap(weekCount);
        final Map<Integer, BigDecimal> projAcwpByWeek  = initZeroMap(weekCount);

        for (final WorkPackageWeeklyDTO dto : wpDtos) {
            for (int i = 1; i <= weekCount; i++) {
                projBcwsByWeek.merge(i, dto.getBcwsByWeek().getOrDefault(i, BigDecimal.ZERO), BigDecimal::add);
                projBcwpByWeek.merge(i, dto.getBcwpByWeek().getOrDefault(i, BigDecimal.ZERO), BigDecimal::add);
                projAcwpByWeek.merge(i, dto.getAcwpByWeek().getOrDefault(i, BigDecimal.ZERO), BigDecimal::add);
            }
        }

        // Round project-level maps to 2 decimal places
        roundMap(projBcwsByWeek);
        roundMap(projBcwpByWeek);
        roundMap(projAcwpByWeek);

        // ------------------------------------------------------------------
        // Assemble the report DTO
        // ------------------------------------------------------------------
        final EarnedValueReportDTO report = new EarnedValueReportDTO();
        report.setWeekCount(weekCount);
        report.setWorkPackages(wpDtos);
        report.setTotalBcwsByWeek(projBcwsByWeek);
        report.setTotalBcwpByWeek(projBcwpByWeek);
        report.setTotalAcwpByWeek(projAcwpByWeek);  // project-level ACWP 

        return Response.ok(report).build();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static Map<Integer, BigDecimal> initZeroMap(final int size) {
        final Map<Integer, BigDecimal> m = new LinkedHashMap<>();
        for (int i = 1; i <= size; i++) {
            m.put(i, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
        return m;
    }

    private static void roundMap(final Map<Integer, BigDecimal> map) {
        map.replaceAll((k, v) -> v.setScale(2, RoundingMode.HALF_UP));
    }
}