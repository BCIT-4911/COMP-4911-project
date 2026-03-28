package com.corejsf.api;

import java.time.LocalDate;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.corejsf.DTO.WorkPackageWeeklyDTO;
import com.corejsf.Service.EarnedValueAggregateInput;
import com.corejsf.Service.EarnedValueAggregationService;
import com.corejsf.Service.EarnedValueCalculationService;
import com.corejsf.Service.RebacService;

@Path("/earned-value")
@Produces(MediaType.APPLICATION_JSON)
public class EarnedValueResource {

    @Inject
    private EarnedValueAggregationService aggregationService;

    @Inject
    private EarnedValueCalculationService calculationService;

    @Inject
    private RebacService rebacService;

    @Inject
    private AuthContext authContext;

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
     */
    @GET
    public Response getWeekly(@QueryParam("parentWpId") final String parentWpId,
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

        final EarnedValueAggregateInput input =
                aggregationService.aggregateForParent(parentWpId, asOfDate);

        final List<WorkPackageWeeklyDTO> result = calculationService.calculate(input);

        return Response.ok(result).build();
    }
}