package com.corejsf.api;

import java.util.List;

import com.corejsf.DTO.TimesheetRequestDTO;
import com.corejsf.DTO.TimesheetResponseDTO;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.container.ContainerRequestContext;

@Path("/timesheets")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TimesheetResource {

    @Inject
    private TimesheetService timesheetService;

    @Inject
    private RebacService rebacService;

    @Context
    private ContainerRequestContext requestContext;

    private int getAuthEmpId() {
        return (Integer) requestContext.getProperty(JwtAuthFilter.AUTHENTICATED_EMP_ID);
    }

    private Response forbidden() {
        return Response.status(Response.Status.FORBIDDEN).entity("Access denied").build();
    }

    @GET
    public Response getAll(@QueryParam("empId") Integer empId) {
        int authEmpId = getAuthEmpId();
        int targetEmpId = (empId != null) ? empId : authEmpId;
        if (targetEmpId != authEmpId) {
            return forbidden();
        }
        List<TimesheetResponseDTO> list = timesheetService.getAllTimesheets(targetEmpId);
        return Response.ok(list).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") int id) {
        int authEmpId = getAuthEmpId();
        if (!rebacService.canEditTimesheet(authEmpId, id) && !rebacService.canApproveTimesheet(authEmpId, id)) {
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
        if (dto.getEmpId() != getAuthEmpId()) {
            return forbidden();
        }
        TimesheetResponseDTO response = timesheetService.createTimesheet(dto);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") int id, TimesheetRequestDTO dto) {
        if (!rebacService.canEditTimesheet(getAuthEmpId(), id)) {
            return forbidden();
        }
        TimesheetResponseDTO response = timesheetService.updateTimesheet(id, dto);
        return Response.ok(response).build();
    }

    @PUT
    @Path("/{id}/submit")
    public Response submit(@PathParam("id") int id) {
        if (!rebacService.canEditTimesheet(getAuthEmpId(), id)) {
            return forbidden();
        }
        TimesheetResponseDTO response = timesheetService.submitTimesheet(id);
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") int id) {
        if (!rebacService.canEditTimesheet(getAuthEmpId(), id)) {
            return forbidden();
        }
        timesheetService.deleteTimesheet(id);
        return Response.ok().build();
    }
}
