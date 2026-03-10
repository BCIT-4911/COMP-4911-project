package ca.bcit.infosys.timesheet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.corejsf.DTO.TimesheetRequestDTO;
import com.corejsf.DTO.TimesheetResponseDTO;
import com.corejsf.DTO.TimesheetRowRequestDTO;
import com.corejsf.Entity.Employee;
import com.corejsf.Entity.LaborGrade;
import com.corejsf.Entity.Timesheet;
import com.corejsf.Entity.TimesheetRow;
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
     * Submits a draft timesheet: validates submission rules, sets approved = true.
     * Rejects if already approved.
     */
    @PUT
    @Path("/{id}/submit")
    @Transactional
    public TimesheetResponseDTO submitTimesheet(@PathParam("id") int id) {
        Timesheet ts = findTimesheet(id);
        TimesheetValidation.validateNotApproved(ts.getApprovalStatus());

        // Load existing rows and convert to request DTOs for validation
        List<TimesheetRow> rows = findRows(id);
        List<TimesheetRowRequestDTO> rowDTOs = toRowRequestDTOs(rows);

        TimesheetValidation.validateForSubmission(rowDTOs);

        // Transition state
        ts.setApprovalStatus(true);
        em.merge(ts);

        return timesheetService.toResponseDTO(ts, rows);
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
