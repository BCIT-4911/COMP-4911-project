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
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.corejsf.DTO.EarnedValueReportDTO;
import com.corejsf.DTO.WorkPackageWeeklyDTO;
import com.corejsf.Service.EarnedValueAggregateInput;
import com.corejsf.Service.EarnedValueAggregationService;
import com.corejsf.Service.EarnedValueCalculationService;
import com.corejsf.Service.MonthlyEVReportService;
import com.corejsf.Service.RebacService;
import com.corejsf.DTO.MonthlyEVReportDTO;
import com.corejsf.DTO.EarnedValueReportDTO;


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

    @Inject
    private MonthlyEVReportService monthlyEVReportService;

    @Inject
    private RebacService rebacService;

    @Inject
    private AuthContext authContext;

   

    /**
     * Returns the full EV report for a control account (parent WP) and its
     * child work packages.
     *
     * The response includes:
     *  - Per-WP breakdown with BCWS, BCWP, ACWP per week, plus scalar
     *    SV, CV, EAC, VAC and BAC for each WP.
     *  - Project-level per-week totals (BCWS, BCWP, ACWP, SV, CV by week index).
     *  - Project-level scalar totals: SV, CV, EAC, VAC, BAC.
    /**
     * Weekly EV data for a control-account (parent WP) and its children.
     *
     * Now protected by ReBAC (EV Security feature):
     *   - OPERATIONS_MANAGER:           allowed 
     *   - ADMIN:                        allowed 
     *   - PM of the project that owns   allowed 
     *     parentWpId:
     *   - PM of a different project:    403     
     *   - EMPLOYEE / HR / unrelated RE: 403     
     *
     * Example:
     *   GET /api/earned-value?parentWpId=CA-1
     *   GET /api/earned-value?parentWpId=CA-1&asOf=2026-02-15
     *
     * @param parentWpId the ID of the parent (control-account) work package
     * @param asOf       optional ISO-8601 date; defaults to today
     * @return 200 EarnedValueReportDTO, or 400 if parentWpId is missing
     */
    @GET
    public Response getEVReport(@QueryParam("parentWpId") final String parentWpId,
                                @QueryParam("asOf") final String asOf) {

        if (parentWpId == null || parentWpId.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("parentWpId is required")
                    .build();
        }

        // ReBAC authorization check — delegates to RebacService.canViewEVReport
        // which encodes all acceptance criteria.
        if (!rebacService.canViewEVReport(
                authContext.getEmpId(),
                authContext.getSystemRole(),
                parentWpId)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Access denied")
                    .build();
        }

        final LocalDate asOfDate = (asOf == null || asOf.isBlank())
                ? null
                : LocalDate.parse(asOf);

        // Build the aggregation input (date range, week-ending list, child WPs)
        final EarnedValueAggregateInput input = aggregationService.aggregateForParent(parentWpId, asOfDate);

        // Calculate per-WP BCWS, BCWP, and real ACWP from approved timesheets
        final EarnedValueReportDTO calculatedReport = calculationService.calculate(input);

        final List<WorkPackageWeeklyDTO> wpDtos = calculatedReport.getWorkPackages();
        final int weekCount = input.getWeekEndings().size();

        // ------------------------------------------------------------------
        // Roll up per-WP values to project level
        // ------------------------------------------------------------------
        final Map<Integer, BigDecimal> projBcwsByWeek = initZeroMap(weekCount);
        final Map<Integer, BigDecimal> projBcwpByWeek = initZeroMap(weekCount);
        final Map<Integer, BigDecimal> projAcwpByWeek = initZeroMap(weekCount);

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
        report.setTotalAcwpByWeek(projAcwpByWeek);

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

    // -----------------------------------------------------------------------
    // New endpoint — Monthly EV Report 
    // -----------------------------------------------------------------------

    /**
     * Monthly Earned Value Report for an entire project.
     *
     * Returns BCWS, BCWP, and ACWP at both project level and per-work-package
     * level, plus derived SV and CV.
     *
     * Authorization:
     *   - OPERATIONS_MANAGER: always allowed.
     *   - Project Manager of the requested project: allowed.
     *   - Anyone else: 403 Forbidden.
     *
     * @param projectId the project to report on
     * @param asOf      optional ISO-8601 date string; defaults to today
     * @return 200 with MonthlyEVReportDTO, or 403 if unauthorized
     */
    @GET
    @Path("/projects/{projectId}/monthly-report")
    public Response getMonthlyReport(
            @PathParam("projectId") final String projectId,
            @QueryParam("asOf") final String asOf) {

         // authorization check
        final boolean isOpsManager =
                rebacService.canCreateProject(authContext.getSystemRole());
        final boolean isProjectManager =
                rebacService.canManageProject(authContext.getEmpId(), projectId);

        if (!isOpsManager && !isProjectManager) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Access denied")
                    .build();
        }

        // Parse optional asOf date
        final LocalDate asOfDate = (asOf == null || asOf.isBlank())
                ? LocalDate.now()
                : LocalDate.parse(asOf);

        // AC1, AC2, AC3 — delegate to the service
        final MonthlyEVReportDTO report =
                monthlyEVReportService.generateReport(projectId, asOfDate);

        return Response.ok(report).build();
    }
}