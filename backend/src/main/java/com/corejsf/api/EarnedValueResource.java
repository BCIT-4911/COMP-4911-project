package com.corejsf.api;

import java.time.LocalDate;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.corejsf.DTO.WorkPackageWeeklyDTO;
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
     * Example:
     * GET /api/earned-value?parentWpId=CA-1
     * GET /api/earned-value?parentWpId=CA-1&asOf=2026-02-15
     */
    @GET
    public List<WorkPackageWeeklyDTO> getWeekly(@QueryParam("parentWpId") final String parentWpId,
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
}