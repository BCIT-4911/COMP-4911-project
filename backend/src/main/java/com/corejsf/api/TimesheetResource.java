package com.corejsf.api;

import java.util.List;

import com.corejsf.DTO.TimesheetRequestDTO;
import com.corejsf.DTO.TimesheetReturnRequestDTO;
import com.corejsf.DTO.TimesheetResponseDTO;
import com.corejsf.Entity.SystemRole;
import com.corejsf.Entity.TimesheetStatus;
import com.corejsf.Service.TimesheetService;
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

@Path("/timesheets")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TimesheetResource {

    @Inject
    private TimesheetService timesheetService;

    @Inject
    private RebacService rebacService;

    @Inject
    private AuthContext authContext;

    private Response forbidden() {
        return Response.status(Response.Status.FORBIDDEN).entity("Access denied").build();
    }

    @GET
    public Response getAll(@QueryParam("empId") Integer empId,
                           @QueryParam("approverId") Integer approverId,
                           @QueryParam("status") TimesheetStatus status) {
        int authEmpId = authContext.getEmpId();

        boolean isAdmin = authContext.getSystemRole() == SystemRole.ADMIN;
        if (empId != null && empId != authEmpId && !isAdmin && !rebacService.isSupervisorOf(authEmpId, empId)) {
            return forbidden();
        }

        // Approver queue can only be queried by the approver themself.
        if (approverId != null && approverId != authEmpId && !isAdmin) {
            return forbidden();
        }

        Integer effectiveEmpId;
        if (isAdmin && empId == null && approverId == null) {
            effectiveEmpId = null;
        } else if (empId != null) {
            effectiveEmpId = empId;
        } else if (approverId == null) {
            effectiveEmpId = authEmpId;
        } else {
            effectiveEmpId = null;
        }
        List<TimesheetResponseDTO> list = timesheetService.getAllTimesheets(effectiveEmpId, approverId, status);
        return Response.ok(list).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") int id) {
        int authEmpId = authContext.getEmpId();
        if (!rebacService.canViewTimesheet(authEmpId, id)) {
            return forbidden();
        }
        TimesheetResponseDTO dto = timesheetService.getTimesheet(id);
        return Response.ok(dto).build();
    }

    @POST
    public Response create(TimesheetRequestDTO dto) {
        if (dto == null || dto.getEmpId() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("empId is required").build();
        }
        if (dto.getEmpId().intValue() != authContext.getEmpId() && authContext.getSystemRole() != SystemRole.ADMIN) {
            return forbidden();
        }
        TimesheetResponseDTO response = timesheetService.createTimesheet(dto);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") int id, TimesheetRequestDTO dto) {
        if (!rebacService.canEditTimesheet(authContext.getEmpId(), id)) {
            return forbidden();
        }
        TimesheetResponseDTO response = timesheetService.updateTimesheet(id, dto);
        return Response.ok(response).build();
    }

    @PUT
    @Path("/{id}/submit")
    public Response submit(@PathParam("id") int id) {
        if (!rebacService.canEditTimesheet(authContext.getEmpId(), id)) {
            return forbidden();
        }
        TimesheetResponseDTO response = timesheetService.submitTimesheet(id);
        return Response.ok(response).build();
    }

    @PUT
    @Path("/{id}/approve")
    public Response approve(@PathParam("id") int id) {
        if (!rebacService.canApproveTimesheet(authContext.getEmpId(), id)) {
            return forbidden();
        }
        TimesheetResponseDTO response = timesheetService.approveTimesheet(id);
        return Response.ok(response).build();
    }

    @PUT
    @Path("/{id}/return")
    public Response returnTimesheet(@PathParam("id") int id, TimesheetReturnRequestDTO dto) {
        if (!rebacService.canApproveTimesheet(authContext.getEmpId(), id)) {
            return forbidden();
        }
        TimesheetResponseDTO response = timesheetService.returnTimesheet(id, dto);
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") int id) {
        if (!rebacService.canEditTimesheet(authContext.getEmpId(), id)) {
            return forbidden();
        }
        timesheetService.deleteTimesheet(id);
        return Response.ok().build();
    }
}
