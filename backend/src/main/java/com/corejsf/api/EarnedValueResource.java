package com.corejsf.api;

import java.time.LocalDate;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.corejsf.DTO.MonthlyEVReportDTO;
import com.corejsf.DTO.WorkPackageWeeklyDTO;
import com.corejsf.Service.EarnedValueAggregateInput;
import com.corejsf.Service.EarnedValueAggregationService;
import com.corejsf.Service.EarnedValueCalculationService;
import com.corejsf.Service.MonthlyEVReportService;
import com.corejsf.Service.RebacService;

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
     * Weekly EV data for a control-account (parent WP) and its children.
     *
     */
    @GET
    public List<WorkPackageWeeklyDTO> getWeekly(
            @QueryParam("parentWpId") final String parentWpId,
            @QueryParam("asOf") final String asOf) {

        if (parentWpId == null || parentWpId.isBlank()) {
            throw new IllegalArgumentException("parentWpId is required");
        }

        final LocalDate asOfDate = (asOf == null || asOf.isBlank())
                ? null
                : LocalDate.parse(asOf);

        final EarnedValueAggregateInput input =
                aggregationService.aggregateForParent(parentWpId, asOfDate);

        return calculationService.calculate(input);
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