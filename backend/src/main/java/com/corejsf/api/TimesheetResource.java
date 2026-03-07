package com.corejsf.api;

import java.util.List;

import com.corejsf.DTO.TimesheetRequestDTO;
import com.corejsf.DTO.TimesheetResponseDTO;
import com.corejsf.Service.TimesheetService;

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

    @GET
    public List<TimesheetResponseDTO> getAll(@QueryParam("empId") Integer empId) {
        return timesheetService.getAllTimesheets(empId);
    }

    @GET
    @Path("/{id}")
    public TimesheetResponseDTO get(@PathParam("id") int id) {
        return timesheetService.getTimesheet(id);
    }

    @POST
    public Response create(TimesheetRequestDTO dto) {
        TimesheetResponseDTO response = timesheetService.createTimesheet(dto);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @PUT
    @Path("/{id}")
    public TimesheetResponseDTO update(@PathParam("id") int id, TimesheetRequestDTO dto) {
        return timesheetService.updateTimesheet(id, dto);
    }

    @PUT
    @Path("/{id}/submit")
    public TimesheetResponseDTO submit(@PathParam("id") int id) {
        return timesheetService.submitTimesheet(id);
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") int id) {
        timesheetService.deleteTimesheet(id);
    }
}
