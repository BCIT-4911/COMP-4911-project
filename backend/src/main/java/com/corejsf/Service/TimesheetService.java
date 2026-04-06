package com.corejsf.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

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
import com.corejsf.Entity.WorkPackageType;

@Stateless
public class TimesheetService {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    @Inject
    private TimesheetRowService timesheetRowService;

    @Inject
    private RebacService rebacService;

    @Inject
    private LeaveBalanceService leaveBalanceService;

    // -------------------------------------------------------------------------
    // Entity lookup helpers
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

    private List<TimesheetRow> findRows(int tsId) {
        return em.createQuery(
                "SELECT r FROM TimesheetRow r WHERE r.timesheet.tsId = :tsId", TimesheetRow.class)
                .setParameter("tsId", tsId)
                .getResultList();
    }

    // -------------------------------------------------------------------------
    // DTO conversion (existing)
    // -------------------------------------------------------------------------

    /**
     * Converts a Timesheet entity + its rows into a TimesheetResponseDTO.
     * Navigates relationships to populate empName, approverName, etc.
     */
    public TimesheetResponseDTO toResponseDTO(Timesheet ts, List<TimesheetRow> rows) {
        TimesheetResponseDTO dto = new TimesheetResponseDTO();
        dto.setTsId(ts.getTsId());
        dto.setEmpId(ts.getEmployee().getEmpId());
        dto.setEmpName(ts.getEmployee().getEmpFirstName() + " " + ts.getEmployee().getEmpLastName());
        dto.setWeekEnding(ts.getWeekEnding());
        dto.setStatus(ts.getStatus());
        if (ts.getApprover() != null) {
            dto.setApproverId(ts.getApprover().getEmpId());
            dto.setApproverName(ts.getApprover().getEmpFirstName() + " " + ts.getApprover().getEmpLastName());
        }
        dto.setReturnComment(ts.getReturnComment());
        dto.setRows(timesheetRowService.toResponseDTOList(rows));
        return dto;
    }

    // -------------------------------------------------------------------------
    // CRUD operations (moved from TimesheetController)
    // -------------------------------------------------------------------------

    public List<TimesheetResponseDTO> getAllTimesheets(Integer empId) {
        return getAllTimesheets(empId, null, null);
    }

    public List<TimesheetResponseDTO> getAllTimesheets(Integer empId,
            Integer approverId,
            TimesheetStatus status) {
        StringBuilder jpql = new StringBuilder("SELECT t FROM Timesheet t WHERE 1=1");
        if (empId != null)
            jpql.append(" AND t.employee.empId = :empId");
        if (approverId != null)
            jpql.append(" AND t.approver.empId = :approverId");
        if (status != null)
            jpql.append(" AND t.timesheetStatus = :status");

        TypedQuery<Timesheet> query = em.createQuery(jpql.toString(), Timesheet.class);
        if (empId != null)
            query.setParameter("empId", empId);
        if (approverId != null)
            query.setParameter("approverId", approverId);
        if (status != null)
            query.setParameter("status", status);

        List<Timesheet> timesheets = query.getResultList();
        List<TimesheetResponseDTO> result = new ArrayList<>();
        for (Timesheet ts : timesheets) {
            result.add(toResponseDTO(ts, findRows(ts.getTsId())));
        }
        return result;
    }

    public TimesheetResponseDTO getTimesheet(int id) {
        Timesheet ts = findTimesheet(id);
        List<TimesheetRow> rows = findRows(id);
        return toResponseDTO(ts, rows);
    }

    public TimesheetResponseDTO createTimesheet(TimesheetRequestDTO dto) {
        runValidation(() -> TimesheetValidation.validateRequest(dto));

        Employee employee = findEmployee(dto.getEmpId());

        Timesheet ts = new Timesheet();
        ts.setEmployee(employee);
        ts.setWeekEnding(dto.getWeekEnding());
        ts.setStatus(TimesheetStatus.DRAFT);
        ts.setReturnComment(null);

        em.persist(ts);
        em.flush();

        List<TimesheetRow> rows = createRows(dto.getRows(), ts);
        return toResponseDTO(ts, rows);
    }

    public TimesheetResponseDTO updateTimesheet(int id, TimesheetRequestDTO dto) {
        Timesheet ts = findTimesheet(id);
        runValidation(() -> TimesheetValidation.validateCanEdit(ts.getStatus()));
        runValidation(() -> TimesheetValidation.validateRequest(dto));

        if (!ts.getEmployee().getEmpId().equals(dto.getEmpId())) {
            throw new WebApplicationException(
                    "Request empId must match the timesheet owner.",
                    Response.Status.BAD_REQUEST);
        }

        ts.setWeekEnding(dto.getWeekEnding());

        em.createQuery("DELETE FROM TimesheetRow r WHERE r.timesheet.tsId = :tsId")
                .setParameter("tsId", id)
                .executeUpdate();

        List<TimesheetRow> rows = createRows(dto.getRows(), ts);

        em.merge(ts);
        return toResponseDTO(ts, rows);
    }

    public TimesheetResponseDTO submitTimesheet(int id) {
        Timesheet ts = findTimesheet(id);
        TimesheetStatus currentStatus = ts.getStatus();
        if (currentStatus == TimesheetStatus.SUBMITTED) {
            throw new WebApplicationException(
                    "Cannot submit a SUBMITTED timesheet.",
                    Response.Status.BAD_REQUEST);
        }
        if (currentStatus == TimesheetStatus.APPROVED) {
            throw new WebApplicationException(
                    "Cannot submit an APPROVED timesheet.",
                    Response.Status.BAD_REQUEST);
        }
        if (currentStatus != TimesheetStatus.DRAFT && currentStatus != TimesheetStatus.RETURNED) {
            throw new WebApplicationException(
                    "Only DRAFT or RETURNED timesheets can be submitted.",
                    Response.Status.BAD_REQUEST);
        }

        List<TimesheetRow> rows = findRows(id);
        List<TimesheetRowRequestDTO> rowDTOs = toRowRequestDTOs(rows);
        runValidation(() -> TimesheetValidation.validateForSubmission(rowDTOs));

        Employee supervisor = ts.getEmployee().getSupervisor();
        if (supervisor == null) {
            throw new WebApplicationException(
                    "Employee does not have a supervisor assigned.",
                    Response.Status.BAD_REQUEST);
        }

        ts.setApprover(supervisor);
        ts.setReturnComment(null);
        ts.setStatus(TimesheetStatus.SUBMITTED);
        em.merge(ts);

        return toResponseDTO(ts, rows);
    }

    public TimesheetResponseDTO approveTimesheet(int id) {
        Timesheet ts = findTimesheet(id);
        if (ts.getStatus() != TimesheetStatus.SUBMITTED) {
            throw new WebApplicationException(
                    "Only SUBMITTED timesheets can be approved.",
                    Response.Status.BAD_REQUEST);
        }

        ts.setStatus(TimesheetStatus.APPROVED);
        ts.setReturnComment(null);

        em.merge(ts);

        List<TimesheetRow> rows = findRows(id);

        leaveBalanceService.applyApprovedLeave(ts.getEmployee(), rows);

        Set<String> processedWpIds = new HashSet<>();
        for (TimesheetRow row : rows) {
            WorkPackage wp = row.getWorkPackage();
            if (wp.getWpType() == WorkPackageType.LOWEST_LEVEL
                    && !Boolean.TRUE.equals(wp.getStructureLocked())
                    && processedWpIds.add(wp.getWpId())) {
                wp.setStructureLocked(true);
                em.merge(wp);
            }
        }

        return toResponseDTO(ts, rows);
    }

    public TimesheetResponseDTO returnTimesheet(int id, TimesheetReturnRequestDTO dto) {
        Timesheet ts = findTimesheet(id);
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
        return toResponseDTO(ts, findRows(id));
    }

    public void deleteTimesheet(int id) {
        Timesheet ts = findTimesheet(id);
        runValidation(() -> TimesheetValidation.validateCanDelete(ts.getStatus()));

        em.createQuery("DELETE FROM TimesheetRow r WHERE r.timesheet.tsId = :tsId")
                .setParameter("tsId", id)
                .executeUpdate();

        em.remove(ts);
    }

    // -------------------------------------------------------------------------
    // Row helpers (moved from TimesheetController)
    // -------------------------------------------------------------------------

    /**
     * Maps TimesheetRowRequestDTOs to TimesheetRow entities,
     * resolving WP foreign keys and assigning the timesheet owner's labor grade (client row laborGradeId is ignored).
     */
    public List<TimesheetRow> createRows(List<TimesheetRowRequestDTO> rowDTOs, Timesheet ts) {
        LaborGrade employeeGrade = ts.getEmployee().getLaborGrade();
        if (employeeGrade == null) {
            throw new WebApplicationException(
                    "Timesheet owner does not have a labor grade assigned.",
                    Response.Status.BAD_REQUEST);
        }

        List<TimesheetRow> rows = new ArrayList<>();
        for (TimesheetRowRequestDTO rowDto : rowDTOs) {
            WorkPackage wp = findWorkPackage(rowDto.getWpId());
            runValidation(() -> TimesheetValidation.validateWorkPackageChargeable(wp));

            if (!rebacService.canChargeToWorkPackage(ts.getEmployee().getEmpId(), wp.getWpId())) {
                throw new WebApplicationException(
                        "Employee does not have permission to charge to work package " + wp.getWpId(),
                        Response.Status.FORBIDDEN);
            }

            TimesheetRow row = new TimesheetRow();
            row.setTimesheet(ts);
            row.setWorkPackage(wp);
            row.setLaborGrade(employeeGrade);
            row.setMonday(nz(rowDto.getMonday()));
            row.setTuesday(nz(rowDto.getTuesday()));
            row.setWednesday(nz(rowDto.getWednesday()));
            row.setThursday(nz(rowDto.getThursday()));
            row.setFriday(nz(rowDto.getFriday()));
            row.setSaturday(rowDto.getSaturday());
            row.setSunday(nz(rowDto.getSunday()));

            em.persist(row);
            rows.add(row);
        }
        return rows;
    }

    /**
     * Converts persisted TimesheetRow entities back to request DTOs
     * for submission validation.
     */
    public List<TimesheetRowRequestDTO> toRowRequestDTOs(List<TimesheetRow> rows) {
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

    private void runValidation(Runnable validator) {
        try {
            validator.run();
        } catch (IllegalArgumentException ex) {
            throw new WebApplicationException(ex.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
