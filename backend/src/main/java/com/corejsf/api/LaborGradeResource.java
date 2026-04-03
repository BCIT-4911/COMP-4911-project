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
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Resource class for managing labor grade data.
 * Provides endpoints for retrieving, creating, updating, and deleting labor grades.
 * Create, update, and delete operations are restricted to ADMIN and OPERATIONS_MANAGER roles.
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

    /**
     * Creates a new labor grade.
     * Only accessible to ADMIN and OPERATIONS_MANAGER roles.
     *
     * @param dto the labor grade data to create.
     * @return the created labor grade with CREATED status.
     */
    @POST
    public Response create(LaborGradeDTO dto) {
        if (!rebacService.canManageLaborGrades(authContext.getSystemRole())) {
            return forbidden();
        }
        LaborGradeDTO created = laborGradeService.createLaborGrade(dto);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    /**
     * Updates an existing labor grade.
     * Only accessible to ADMIN and OPERATIONS_MANAGER roles.
     *
     * @param id the ID of the labor grade to update.
     * @param dto the updated labor grade data.
     * @return the updated labor grade with OK status.
     */
    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") int id, LaborGradeDTO dto) {
        if (!rebacService.canManageLaborGrades(authContext.getSystemRole())) {
            return forbidden();
        }
        LaborGradeDTO updated = laborGradeService.updateLaborGrade(id, dto);
        return Response.ok(updated).build();
    }

    /**
     * Deletes a labor grade by ID.
     * Only accessible to ADMIN and OPERATIONS_MANAGER roles.
     * Returns 409 Conflict if the labor grade is currently assigned to employees or timesheet rows.
     *
     * @param id the ID of the labor grade to delete.
     * @return OK status on successful deletion, or 409 Conflict if in use.
     */
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") int id) {
        if (!rebacService.canManageLaborGrades(authContext.getSystemRole())) {
            return forbidden();
        }
        try {
            laborGradeService.deleteLaborGrade(id);
            return Response.ok().build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }
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
