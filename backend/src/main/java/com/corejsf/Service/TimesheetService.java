package com.corejsf.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.NotFoundException;

import com.corejsf.DTO.TimesheetRequestDTO;
import com.corejsf.DTO.TimesheetResponseDTO;
import com.corejsf.DTO.TimesheetRowRequestDTO;
import com.corejsf.Entity.Employee;
import com.corejsf.Entity.LaborGrade;
import com.corejsf.Entity.Timesheet;
import com.corejsf.Entity.TimesheetRow;
import com.corejsf.Entity.WorkPackage;


@Stateless
public class TimesheetService {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    @Inject
    private TimesheetRowService timesheetRowService;

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
        dto.setApproved(ts.getApprovalStatus());
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
            result.add(toResponseDTO(ts, rows));
        }
        return result;
    }

    public TimesheetResponseDTO getTimesheet(int id) {
        Timesheet ts = findTimesheet(id);
        List<TimesheetRow> rows = findRows(id);
        return toResponseDTO(ts, rows);
    }

    public TimesheetResponseDTO createTimesheet(TimesheetRequestDTO dto) {
        TimesheetValidation.validateRequest(dto);

        Employee employee = findEmployee(dto.getEmpId());

        Timesheet ts = new Timesheet();
        ts.setEmployee(employee);
        ts.setWeekEnding(dto.getWeekEnding());
        ts.setApprovalStatus(false);

        if (dto.getApproverId() != null) {
            ts.setApprover(findEmployee(dto.getApproverId()));
        }

        em.persist(ts);
        em.flush();

        List<TimesheetRow> rows = createRows(dto.getRows(), ts);
        return toResponseDTO(ts, rows);
    }

    public TimesheetResponseDTO updateTimesheet(int id, TimesheetRequestDTO dto) {
        Timesheet ts = findTimesheet(id);
        TimesheetValidation.validateNotApproved(ts.getApprovalStatus());
        TimesheetValidation.validateRequest(dto);

        ts.setWeekEnding(dto.getWeekEnding());
        if (dto.getApproverId() != null) {
            ts.setApprover(findEmployee(dto.getApproverId()));
        }

        em.createQuery("DELETE FROM TimesheetRow r WHERE r.timesheet.tsId = :tsId")
                .setParameter("tsId", id)
                .executeUpdate();

        List<TimesheetRow> rows = createRows(dto.getRows(), ts);

        em.merge(ts);
        return toResponseDTO(ts, rows);
    }

    public TimesheetResponseDTO submitTimesheet(int id) {
        Timesheet ts = findTimesheet(id);
        TimesheetValidation.validateNotApproved(ts.getApprovalStatus());

        List<TimesheetRow> rows = findRows(id);
        List<TimesheetRowRequestDTO> rowDTOs = toRowRequestDTOs(rows);

        TimesheetValidation.validateForSubmission(rowDTOs);

        ts.setApprovalStatus(true);
        em.merge(ts);

        return toResponseDTO(ts, rows);
    }

    public void deleteTimesheet(int id) {
        Timesheet ts = findTimesheet(id);
        TimesheetValidation.validateNotApproved(ts.getApprovalStatus());

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
     * resolving WP and LaborGrade foreign keys, and persists each row.
     */
    public List<TimesheetRow> createRows(List<TimesheetRowRequestDTO> rowDTOs, Timesheet ts) {
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

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
