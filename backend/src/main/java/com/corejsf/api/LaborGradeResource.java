package com.corejsf.api;

import java.util.List;
import java.time.LocalDate;

import com.corejsf.DTO.LaborReportDTO;
import com.corejsf.DTO.LaborGradeDTO;
import com.corejsf.Entity.SystemRole;
import com.corejsf.Service.LaborGradeService;
import com.corejsf.Service.RebacService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Resource class for managing labor grade data.
 * Provides endpoints for retrieving labor grade information.
 */
@Path("/labor-grades")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LaborGradeResource {

    @Inject
    private LaborGradeService laborGradeService;

    @Inject
    private RebacService rebacService;

    @Inject
    private AuthContext authContext;

    private Response forbidden() {
        return Response.status(Response.Status.FORBIDDEN).entity("Access denied").build();
    }

    /**
     * Retrieves all labor grades.
     * @return a list of all labor grades.
     */
    @GET
    public List<LaborGradeDTO> getAll() {
        return laborGradeService.getAllLaborGrades();
    }

    /**
     * Retrieves a labor grade by ID.
     * @param id the ID of the labor grade to retrieve.
     * @return the labor grade with the specified ID.
     * @throws NotFoundException if the labor grade with the specified ID is not found.
     */
    @GET
    @Path("/{id}")
    public LaborGradeDTO get(@PathParam("id") int id) {
        return laborGradeService.getLaborGrade(id);
    }

    @GET
    @Path("/report")
    public Response getLaborReport(@QueryParam("projectId") String projectId,
                                   @QueryParam("wpId") String wpId,
                                   @QueryParam("employeeId") Integer employeeId,
                                   @QueryParam("weekEnding") String weekEnding) {
        if (weekEnding == null || weekEnding.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("weekEnding is required")
                    .build();
        }
        LocalDate parsedWeekEnding;
        try {
            parsedWeekEnding = LocalDate.parse(weekEnding);
        } catch (RuntimeException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("weekEnding must be in YYYY-MM-DD format")
                    .build();
        }

        int authEmpId = authContext.getEmpId();
        SystemRole role = authContext.getSystemRole();

        boolean globalAccess = role == SystemRole.HR
                || role == SystemRole.OPERATIONS_MANAGER
                || role == SystemRole.ADMIN;
        boolean projectManagerAccess = projectId != null && rebacService.canManageProject(authEmpId, projectId);

        Integer effectiveEmployeeId = employeeId;
        if (!globalAccess && !projectManagerAccess) {
            if (employeeId != null
                    && employeeId.intValue() != authEmpId
                    && !rebacService.isSupervisorOf(authEmpId, employeeId)) {
                return forbidden();
            }

            if (effectiveEmployeeId == null) {
                effectiveEmployeeId = authEmpId;
            }
        }

        LaborReportDTO report = laborGradeService.generateLaborReport(projectId, wpId, effectiveEmployeeId, parsedWeekEnding);
        return Response.ok(report).build();
    }
}
