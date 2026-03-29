package com.corejsf.api;

import java.time.LocalDate;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.corejsf.DTO.EarnedValueReportDTO;
import com.corejsf.Service.EarnedValueAggregateInput;
import com.corejsf.Service.EarnedValueAggregationService;
import com.corejsf.Service.EarnedValueCalculationService;

@Path("/earned-value")
@Produces(MediaType.APPLICATION_JSON)
public class EarnedValueResource {

    @Inject
    private EarnedValueAggregationService aggregationService;

    @Inject
    private EarnedValueCalculationService calculationService;

    /**
     * Returns the full EV report for a control account (parent WP) and its
     * child work packages.
     *
     * The response includes:
     *  - Per-WP breakdown with BCWS, BCWP, ACWP per week, plus scalar
     *    SV, CV, EAC, VAC and BAC for each WP.
     *  - Project-level per-week totals (BCWS, BCWP, ACWP, SV, CV by week index).
     *  - Project-level scalar totals: SV, CV, EAC, VAC, BAC.
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

        final LocalDate asOfDate = (asOf == null || asOf.isBlank())
                ? null
                : LocalDate.parse(asOf);

        final EarnedValueAggregateInput input =
                aggregationService.aggregateForParent(parentWpId, asOfDate);

        final EarnedValueReportDTO report = calculationService.calculate(input);

        return Response.ok(report).build();
    }
}