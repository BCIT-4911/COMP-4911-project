package com.corejsf.boundary;


import com.corejsf.Control.TimesheetController;
import com.corejsf.entity.Timesheet;
import com.corejsf.entity.TimesheetRow;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;

@Path("/api/timesheets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TimesheetResource {

    @Inject
    private TimesheetController timesheetController;

    /**
     * Obtains timesheet with database-assigned ID from TimesheetController
     * and returns it and its status to ASP.NET. Otherwise, throws an error.
     * @param timesheet
     * @return Response
     */
    @POST
    public Response createTimesheet(Timesheet timesheet) {
        try {
            Timesheet created = timesheetController.createTimesheet(timesheet);
            return Response.status(Response.Status.CREATED)
                    .entity(created)
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }
    }

    /**
     * Gets id from URL path, sends to TimesheetController,
     * and receives the desired Timesheet.
     * Returns a status response and/or Timesheet.
     * @param id
     * @return Response
     */
    @GET
    @Path("/{id}")
    public Response getTimesheet(@PathParam("id") Integer id) {
        Timesheet timesheet = timesheetController.getTimesheet(id);
        if (timesheet == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(timesheet).build();
    }

    /** Plan to create method for getting Timesheets from specific Employees later */

    /**
     * Obtains from TimesheetController and database all Pending Timesheets.
     * @return Response
     */
    @GET
    @Path("/pending")
    public Response getPendingTimesheets() {
        List<Timesheet> timesheets = timesheetController.getPendingTimesheets();
        return Response.ok(timesheets).build();
    }




}
