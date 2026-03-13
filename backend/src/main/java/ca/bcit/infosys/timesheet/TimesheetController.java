package ca.bcit.infosys.timesheet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.corejsf.DTO.ReturnTimesheetRequestDTO;
import com.corejsf.DTO.TimesheetRequestDTO;
import com.corejsf.DTO.TimesheetResponseDTO;
import com.corejsf.DTO.TimesheetRowRequestDTO;
import com.corejsf.Entity.Employee;
import com.corejsf.Entity.LaborGrade;
import com.corejsf.Entity.Timesheet;
import com.corejsf.Entity.TimesheetRow;
import com.corejsf.Entity.TimesheetStatus;
import com.corejsf.Entity.WorkPackage;
import com.corejsf.Service.TimesheetService;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST controller for timesheets.
 * Accepts TimesheetRequestDTO on POST/PUT, returns TimesheetResponseDTO on all endpoints.
 */
@Stateless
@Path("/timesheets")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TimesheetController {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    @Inject
    private TimesheetService timesheetService;

    // -------------------------------------------------------------------------
    // Lookup helpers
    // -------------------------------------------------------------------------

    private Timesheet findTimesheet(int id) {
        Timesheet ts = em.find(Timesheet.class, id);
        if (ts == null) {
            throw new NotFoundException("Timesheet with id " + id + " not found.");
        }
        return ts;
    }

    private Employee findEmployee(int id) {
        Employee emp = em.find(Employee.class, id);
        if (emp == null) {
            throw new NotFoundException("Employee with id " + id + " not found.");
        }
        return emp;
    }

    private WorkPackage findWorkPackage(String id) {
        WorkPackage wp = em.find(WorkPackage.class, id);
        if (wp == null) {
            throw new NotFoundException("WorkPackage with id " + id + " not found.");
        }
        return wp;
    }

    private LaborGrade findLaborGrade(int id) {
        LaborGrade lg = em.find(LaborGrade.class, id);
        if (lg == null) {
            throw new NotFoundException("LaborGrade with id " + id + " not found.");
        }
        return lg;
    }

    private List<TimesheetRow> findRows(int tsId) {
        return em.createQuery(
                "SELECT r FROM TimesheetRow r WHERE r.timesheet.tsId = :tsId", TimesheetRow.class)
                .setParameter("tsId", tsId)
                .getResultList();
    }

    // -------------------------------------------------------------------------
    // GET endpoints
    // -------------------------------------------------------------------------

    /**
     * Gets all timesheets, optionally filtered by employee ID.
     */
    @GET
    public List<TimesheetResponseDTO> getAllTimesheets(@QueryParam("empId") Integer empId) {
        List<Timesheet> timesheets;
        if (empId != null) {
            timesheets = em.createQuery(
                    "SELECT t FROM Timesheet t WHERE t.employee.empId = :empId", Timesheet.class)
                    .setParameter("empId", empId)
                    .getResultList();
        } else {
            timesheets = em.createQuery("SELECT t FROM Timesheet t", Timesheet.class)
                    .getResultList();
        }

        List<TimesheetResponseDTO> result = new ArrayList<>();
        for (Timesheet ts : timesheets) {
            List<TimesheetRow> rows = findRows(ts.getTsId());
            result.add(timesheetService.toResponseDTO(ts, rows));
        }
        return result;
    }

    /**
     * Gets a single timesheet by ID, including its rows.
     */
    @GET
    @Path("/{id}")
    public TimesheetResponseDTO getTimesheet(@PathParam("id") int id) {
        Timesheet ts = findTimesheet(id);
        List<TimesheetRow> rows = findRows(id);
        return timesheetService.toResponseDTO(ts, rows);
    }

    // -------------------------------------------------------------------------
    // POST - Create draft
    // -------------------------------------------------------------------------

    /**
     * Creates a new draft timesheet with rows.
     * Validates the request DTO, resolves foreign keys, persists entities.
     */
    @POST
    @Transactional
    public Response createTimesheet(TimesheetRequestDTO dto) {
        TimesheetValidation.validateRequest(dto);

        Employee employee = findEmployee(dto.getEmpId());

        // Build timesheet entity
        Timesheet ts = new Timesheet();
        ts.setEmployee(employee);
        ts.setWeekEnding(dto.getWeekEnding());
        ts.setApprovalStatus(false);

        if (dto.getApproverId() != null) {
            ts.setApprover(findEmployee(dto.getApproverId()));
        }

        em.persist(ts);
        em.flush(); // ensure ts_id is generated before creating rows

        // Build and persist rows
        List<TimesheetRow> rows = createRows(dto.getRows(), ts);

        TimesheetResponseDTO response = timesheetService.toResponseDTO(ts, rows);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    // -------------------------------------------------------------------------
    // PUT - Update draft
    // -------------------------------------------------------------------------

    /**
     * Updates an existing draft timesheet. Replaces all rows.
     * Rejects if the timesheet is already approved (immutability).
     */
    @PUT
    @Path("/{id}")
    @Transactional
    public TimesheetResponseDTO updateTimesheet(@PathParam("id") int id, TimesheetRequestDTO dto) {
        Timesheet ts = findTimesheet(id);
        TimesheetValidation.validateNotApproved(ts.getApprovalStatus());
        TimesheetValidation.validateRequest(dto);

        // Update timesheet fields
        ts.setWeekEnding(dto.getWeekEnding());
        if (dto.getApproverId() != null) {
            ts.setApprover(findEmployee(dto.getApproverId()));
        }

        // Delete existing rows and replace
        em.createQuery("DELETE FROM TimesheetRow r WHERE r.timesheet.tsId = :tsId")
                .setParameter("tsId", id)
                .executeUpdate();

        List<TimesheetRow> rows = createRows(dto.getRows(), ts);

        em.merge(ts);
        return timesheetService.toResponseDTO(ts, rows);
    }

   // -------------------------------------------------------------------------
// PUT /submit - Submit timesheet
// -------------------------------------------------------------------------

/**
 * Submits a timesheet.
 * Draft timesheets can be updated/deleted, but once submitted they become immutable and require approval.
 * Validates submission rules, automatically assigns supervisor as approver, and marks as submitted.
 * Rejects if already approved or if employee has no supervisor.
 */
@PUT
@Path("/{id}/submit")
@Transactional
public TimesheetResponseDTO submitTimesheet(@PathParam("id") int id) {

    Timesheet ts = findTimesheet(id);

    // Reject if already approved
    if (Boolean.TRUE.equals(ts.getApprovalStatus())) {
        throw new WebApplicationException(
            "Approved timesheets cannot be submitted again.",
            Response.Status.BAD_REQUEST
        );
    }

    // Load rows
    List<TimesheetRow> rows = findRows(id);
    List<TimesheetRowRequestDTO> rowDTOs = toRowRequestDTOs(rows);

    // Validate submission rules
    TimesheetValidation.validateForSubmission(rowDTOs);

    // Automatically assign supervisor as approver
    Employee employee = ts.getEmployee();
    Employee supervisor = employee.getSupervisor();

    if (supervisor == null) {
        throw new WebApplicationException(
            "Employee does not have a supervisor assigned.",
            Response.Status.BAD_REQUEST
        );
    }

    ts.setApprover(supervisor);

    // Mark as submitted (waiting for approval)
    ts.setApprovalStatus(false);

    em.merge(ts);

    return timesheetService.toResponseDTO(ts, rows);
}

    // -------------------------------------------------------------------------
    // PUT /approve - Approve timesheet (approver only)
    // -------------------------------------------------------------------------

    /**
     * Approves a submitted timesheet.
     * Only the assigned approver may call this. Transitions SUBMITTED → APPROVED.
     * Clears any previous return comment on success.
     *
     * @param id         the timesheet ID
     * @param approverId temporary caller identity (placeholder until auth is wired)
     */
    @PUT
    @Path("/{id}/approve")
    @Transactional
    public Response approveTimesheet(@PathParam("id") int id,
                                     @QueryParam("approverId") Integer approverId) {
        if (approverId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("approverId query parameter is required.")
                    .build();
        }

        Timesheet ts = findTimesheet(id);

        // Authorization: caller must be the assigned approver
        try {
            TimesheetValidation.validateIsApprover(ts, approverId);
        } catch (SecurityException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(e.getMessage())
                    .build();
        }

        // State transition: only SUBMITTED → APPROVED
        try {
            TimesheetValidation.validateCanApprove(ts.getStatus());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }

        ts.setStatus(TimesheetStatus.APPROVED);
        ts.setReturnComment(null); // clear any previous return comment
        em.merge(ts);

        List<TimesheetRow> rows = findRows(id);
        return Response.ok(timesheetService.toResponseDTO(ts, rows)).build();
    }

    // -------------------------------------------------------------------------
    // PUT /return - Return timesheet (approver only)
    // -------------------------------------------------------------------------

    /**
     * Returns a submitted timesheet to the employee with a required comment.
     * Only the assigned approver may call this. Transitions SUBMITTED → RETURNED.
     *
     * @param id         the timesheet ID
     * @param approverId temporary caller identity (placeholder until auth is wired)
     * @param dto        body containing the required returnComment
     */
    @PUT
    @Path("/{id}/return")
    @Transactional
    public Response returnTimesheet(@PathParam("id") int id,
                                    @QueryParam("approverId") Integer approverId,
                                    ReturnTimesheetRequestDTO dto) {
        if (approverId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("approverId query parameter is required.")
                    .build();
        }

        Timesheet ts = findTimesheet(id);

        // Authorization: caller must be the assigned approver
        try {
            TimesheetValidation.validateIsApprover(ts, approverId);
        } catch (SecurityException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(e.getMessage())
                    .build();
        }

        // State transition: only SUBMITTED → RETURNED
        try {
            TimesheetValidation.validateCanReturn(ts.getStatus());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }

        // Return comment is mandatory
        try {
            TimesheetValidation.validateReturnComment(
                    dto != null ? dto.getReturnComment() : null);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }

        ts.setStatus(TimesheetStatus.RETURNED);
        ts.setReturnComment(dto.getReturnComment().trim());
        em.merge(ts);

        List<TimesheetRow> rows = findRows(id);
        return Response.ok(timesheetService.toResponseDTO(ts, rows)).build();
    }

    // -------------------------------------------------------------------------
    // DELETE - Delete draft
    // -------------------------------------------------------------------------

    /**
     * Deletes a draft timesheet and all its rows.
     * Rejects if the timesheet is approved (immutability).
     */
    @DELETE
    @Path("/{id}")
    @Transactional
    public void deleteTimesheet(@PathParam("id") int id) {
        Timesheet ts = findTimesheet(id);
        TimesheetValidation.validateNotApproved(ts.getApprovalStatus());

        em.createQuery("DELETE FROM TimesheetRow r WHERE r.timesheet.tsId = :tsId")
                .setParameter("tsId", id)
                .executeUpdate();

        em.remove(ts);
    }



// -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Maps a list of TimesheetRowRequestDTOs to TimesheetRow entities,
     * resolving WP and LaborGrade foreign keys, and persists each row.
     */
    private List<TimesheetRow> createRows(List<TimesheetRowRequestDTO> rowDTOs, Timesheet ts) {
        List<TimesheetRow> rows = new ArrayList<>();
        for (TimesheetRowRequestDTO rowDto : rowDTOs) {
            WorkPackage wp = findWorkPackage(rowDto.getWpId());
            TimesheetValidation.validateWorkPackageChargeable(wp);

            LaborGrade lg = findLaborGrade(rowDto.getLaborGradeId());

            TimesheetRow row = new TimesheetRow();
            row.setTimesheet(ts);
            row.setWorkPackage(wp);
            row.setLaborGrade(lg);
            row.setMonday(nz(rowDto.getMonday()));
            row.setTuesday(nz(rowDto.getTuesday()));
            row.setWednesday(nz(rowDto.getWednesday()));
            row.setThursday(nz(rowDto.getThursday()));
            row.setFriday(nz(rowDto.getFriday()));
            row.setSaturday(rowDto.getSaturday()); // nullable per DDL
            row.setSunday(nz(rowDto.getSunday()));

            em.persist(row);
            rows.add(row);
        }
        return rows;
    }

    /**
     * Converts persisted TimesheetRow entities back to request DTOs
     * so they can be run through submission validation.
     */
    private List<TimesheetRowRequestDTO> toRowRequestDTOs(List<TimesheetRow> rows) {
        List<TimesheetRowRequestDTO> dtos = new ArrayList<>();
        for (TimesheetRow row : rows) {
            TimesheetRowRequestDTO dto = new TimesheetRowRequestDTO();
            dto.setWpId(row.getWorkPackage().getWpId());
            dto.setLaborGradeId(row.getLaborGrade().getLaborGradeId());
            dto.setMonday(row.getMonday());
            dto.setTuesday(row.getTuesday());
            dto.setWednesday(row.getWednesday());
            dto.setThursday(row.getThursday());
            dto.setFriday(row.getFriday());
            dto.setSaturday(row.getSaturday());
            dto.setSunday(row.getSunday());
            dtos.add(dto);
        }
        return dtos;
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
