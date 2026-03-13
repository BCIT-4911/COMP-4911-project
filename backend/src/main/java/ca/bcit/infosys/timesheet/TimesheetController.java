package ca.bcit.infosys.timesheet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.corejsf.DTO.TimesheetRequestDTO;
import com.corejsf.DTO.TimesheetReturnRequestDTO;
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
    public List<TimesheetResponseDTO> getAllTimesheets(
            @QueryParam("empId") Integer empId,
            @QueryParam("approverId") Integer approverId,
            @QueryParam("status") TimesheetStatus status) {
        List<Timesheet> timesheets = em.createQuery("SELECT t FROM Timesheet t", Timesheet.class)
                .getResultList();

        List<TimesheetResponseDTO> result = new ArrayList<>();
        for (Timesheet ts : timesheets) {
            if (empId != null && !empId.equals(ts.getEmployee().getEmpId())) {
                continue;
            }
            if (approverId != null) {
                if (ts.getApprover() == null || !approverId.equals(ts.getApprover().getEmpId())) {
                    continue;
                }
            }
            if (status != null && ts.getStatus() != status) {
                continue;
            }
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
        runValidation(() -> TimesheetValidation.validateRequest(dto));

        Employee employee = findEmployee(dto.getEmpId());

        // Build timesheet entity
        Timesheet ts = new Timesheet();
        ts.setEmployee(employee);
        ts.setWeekEnding(dto.getWeekEnding());
        ts.setStatus(TimesheetStatus.DRAFT);

        em.persist(ts);
        em.flush(); // ensure ts_id is generated before creating rows

        // Build and persist rows
        List<TimesheetRow> rows = createRows(dto.getRows(), ts);

        TimesheetResponseDTO response = timesheetService.toResponseDTO(ts, rows);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    // -------------------------------------------------------------------------
    // PUT - Update draft or returned timesheet
    // -------------------------------------------------------------------------

    /**
     * Updates an existing timesheet. Replaces all rows.
     * Only DRAFT and RETURNED timesheets can be edited.
     * Rejects edits on SUBMITTED (pending review) and APPROVED (final).
     */
    @PUT
    @Path("/{id}")
    @Transactional
    public TimesheetResponseDTO updateTimesheet(
            @PathParam("id") int id,
            @QueryParam("actorEmpId") Integer actorEmpId,
            TimesheetRequestDTO dto) {
        Timesheet ts = findTimesheet(id);
        validateActorEmpId(actorEmpId);
        assertOwner(ts, actorEmpId);
        runValidation(() -> TimesheetValidation.validateCanEdit(ts.getStatus()));
        runValidation(() -> TimesheetValidation.validateRequest(dto));

        if (!ts.getEmployee().getEmpId().equals(dto.getEmpId())) {
            throw new WebApplicationException(
                    "Request empId must match the timesheet owner.",
                    Response.Status.BAD_REQUEST);
        }

        // Update timesheet fields
        ts.setWeekEnding(dto.getWeekEnding());

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
 * Draft/returned timesheets can be submitted for approval once validation passes.
 * Assigns the employee's supervisor as approver and moves status to SUBMITTED.
 */
@PUT
@Path("/{id}/submit")
@Transactional
public TimesheetResponseDTO submitTimesheet(@PathParam("id") int id,
                                            @QueryParam("actorEmpId") Integer actorEmpId) {
    Timesheet ts = findTimesheet(id);
    validateActorEmpId(actorEmpId);
    assertOwner(ts, actorEmpId);

    TimesheetStatus currentStatus = ts.getStatus();
    if (currentStatus == TimesheetStatus.SUBMITTED) {
        throw new WebApplicationException(
                "Cannot submit a SUBMITTED timesheet.",
                Response.Status.BAD_REQUEST
        );
    }
    if (currentStatus == TimesheetStatus.APPROVED) {
        throw new WebApplicationException(
                "Cannot submit an APPROVED timesheet.",
                Response.Status.BAD_REQUEST
        );
    }
    if (currentStatus != TimesheetStatus.DRAFT && currentStatus != TimesheetStatus.RETURNED) {
        throw new WebApplicationException(
                "Only DRAFT or RETURNED timesheets can be submitted.",
                Response.Status.BAD_REQUEST
        );
    }

    // Load rows
    List<TimesheetRow> rows = findRows(id);
    List<TimesheetRowRequestDTO> rowDTOs = toRowRequestDTOs(rows);

    // Validate submission rules
    runValidation(() -> TimesheetValidation.validateForSubmission(rowDTOs));

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
    ts.setReturnComment(null);

    // Mark as submitted (waiting for approver action)
    ts.setStatus(TimesheetStatus.SUBMITTED);

    em.merge(ts);
    return timesheetService.toResponseDTO(ts, rows);
}

    /**
     * Approves a submitted timesheet.
     * Only the assigned approver can approve.
     */
    @PUT
    @Path("/{id}/approve")
    @Transactional
    public TimesheetResponseDTO approveTimesheet(@PathParam("id") int id,
                                                 @QueryParam("actorEmpId") Integer actorEmpId) {
        Timesheet ts = findTimesheet(id);
        validateActorEmpId(actorEmpId);
        assertApprover(ts, actorEmpId);

        if (ts.getStatus() != TimesheetStatus.SUBMITTED) {
            throw new WebApplicationException(
                    "Only SUBMITTED timesheets can be approved.",
                    Response.Status.BAD_REQUEST);
        }

        ts.setStatus(TimesheetStatus.APPROVED);
        ts.setReturnComment(null);

        em.merge(ts);
        return timesheetService.toResponseDTO(ts, findRows(id));
    }

    /**
     * Returns a submitted timesheet with a required return comment.
     * Only the assigned approver can return.
     */
    @PUT
    @Path("/{id}/return")
    @Transactional
    public TimesheetResponseDTO returnTimesheet(@PathParam("id") int id,
                                                @QueryParam("actorEmpId") Integer actorEmpId,
                                                TimesheetReturnRequestDTO dto) {
        Timesheet ts = findTimesheet(id);
        validateActorEmpId(actorEmpId);
        assertApprover(ts, actorEmpId);

        if (ts.getStatus() != TimesheetStatus.SUBMITTED) {
            throw new WebApplicationException(
                    "Only SUBMITTED timesheets can be returned.",
                    Response.Status.BAD_REQUEST);
        }
        if (dto == null || dto.getReturnComment() == null || dto.getReturnComment().trim().isEmpty()) {
            throw new WebApplicationException(
                    "Return comment is required when returning a timesheet.",
                    Response.Status.BAD_REQUEST);
        }

        ts.setStatus(TimesheetStatus.RETURNED);
        ts.setReturnComment(dto.getReturnComment().trim());

        em.merge(ts);
        return timesheetService.toResponseDTO(ts, findRows(id));
    }

    // -------------------------------------------------------------------------
    // DELETE - Delete draft or returned timesheet
    // -------------------------------------------------------------------------

    /**
     * Deletes a timesheet and all its rows.
     * Only DRAFT and RETURNED timesheets can be deleted.
     * Rejects deletion of SUBMITTED (pending review) and APPROVED (final).
     */
    @DELETE
    @Path("/{id}")
    @Transactional
    public void deleteTimesheet(@PathParam("id") int id,
                                @QueryParam("actorEmpId") Integer actorEmpId) {
        Timesheet ts = findTimesheet(id);
        validateActorEmpId(actorEmpId);
        assertOwner(ts, actorEmpId);
        runValidation(() -> TimesheetValidation.validateCanDelete(ts.getStatus()));

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

    private void validateActorEmpId(Integer actorEmpId) {
        if (actorEmpId == null || actorEmpId <= 0) {
            throw new WebApplicationException(
                    "actorEmpId query parameter is required and must be a positive integer.",
                    Response.Status.BAD_REQUEST);
        }
    }

    private void runValidation(Runnable validator) {
        try {
            validator.run();
        } catch (IllegalArgumentException ex) {
            throw new WebApplicationException(ex.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    private void assertOwner(Timesheet ts, Integer actorEmpId) {
        if (!ts.getEmployee().getEmpId().equals(actorEmpId)) {
            throw new WebApplicationException(
                    "Only the timesheet owner can perform this action.",
                    Response.Status.FORBIDDEN);
        }
    }

    private void assertApprover(Timesheet ts, Integer actorEmpId) {
        if (ts.getApprover() == null || !ts.getApprover().getEmpId().equals(actorEmpId)) {
            throw new WebApplicationException(
                    "Only the assigned approver can perform this action.",
                    Response.Status.FORBIDDEN);
        }
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
