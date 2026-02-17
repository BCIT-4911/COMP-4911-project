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

    /**
     * Grabs Id and List of TimesheetRows that are to be saved, which are
     * sent to TimesheetController to send the saved data to the database
     * @param id
     * @param rows
     * @return Response
     */
    @POST
    @Path("/{id}/draft")
    public Response saveAsDraft(@PathParam("id") Integer id, List<TimesheetRow> rows) {
        try {
            Timesheet saved = timesheetController.saveAsDraft(id, rows);
            return Response.ok(saved).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }
    }

    /**
     * If hours = 40, timesheet data is sent to database via TimesheetController.
     * If hours != 40, error message stating why Timesheet cannot be submitted.
     * @param id
     * @param signatureData
     * @return Response
     */
    @POST
    @Path("/{id}/submit")
    public Response submitTimesheet(@PathParam("id") Integer id, byte[] signatureData) {
        try {
            boolean valid = timesheetController.validateHours(id, new BigDecimal("40"));
            if (!valid) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Timesheet does not meet the 40-hour requirement")
                        .build();
            }

            timesheetController.finalizeSubmission(id, signatureData);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }

    }

    /**
     * Approval is sent to the database, and a response to
     * ASP.NET is sent.
     * @param id
     * @return Response
     */
    @POST
    @Path("/{id}/approve")
    public Response approveTimesheet(@PathParam("id") Integer id) {
        try {
            timesheetController.applyApprovalLock(id);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
    }
}

    /**
     * Grabs Id and comment, and it sends it over to the controller, which
     * ultimately stores the rejected timesheet in the database. Then,
     * if that was a success, a message is sent to the Front-End.
     * @param id
     * @param comments
     * @return Response
     */
    @POST
    @Path("/{id}/reject")
    public Response rejectTimesheet(@PathParam("id") Integer id, String comments) {
        try {
            timesheetController.returnForCorrection(id, comments);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();

        }
    }

    /**
     * Grabs Id to delete the appropriate Timesheet. If deleted = true,
     * returns a Response signifying no content.
     * @param id
     * @return Response
     */
    @DELETE
    @Path("/{id}")
    public Response deleteTimesheet(@PathParam("id") Integer id) {
        boolean deleted = timesheetController.deleteTimesheet(id);
        if (deleted) {
            return Response.noContent().build();
        }
        return Response.status(Response.Status.BAD_REQUEST)
                .entity("Cannot delete submitted timesheet")
                .build();
    }


}
