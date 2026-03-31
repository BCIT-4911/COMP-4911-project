package com.corejsf.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.NotFoundException;

import com.corejsf.DTO.LaborReportDTO;
import com.corejsf.DTO.LaborReportRowDTO;
import com.corejsf.DTO.LaborGradeDTO;
import com.corejsf.Entity.LaborGrade;
import com.corejsf.Entity.TimesheetStatus;
import com.corejsf.Entity.WorkPackage;

@Stateless
public class LaborGradeService {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    public List<LaborGradeDTO> getAllLaborGrades() {
        List<LaborGrade> grades = em.createQuery(
                "SELECT lg FROM LaborGrade lg ORDER BY lg.laborGradeId", LaborGrade.class)
                .getResultList();
        return toDTOList(grades);
    }

    public LaborGradeDTO getLaborGrade(int id) {
        LaborGrade lg = em.find(LaborGrade.class, id);
        if (lg == null) {
            throw new NotFoundException("LaborGrade with id " + id + " not found.");
        }
        return toDTO(lg);
    }

    public LaborGradeDTO toDTO(LaborGrade lg) {
        LaborGradeDTO dto = new LaborGradeDTO();
        dto.setLaborGradeId(lg.getLaborGradeId());
        dto.setGradeCode(lg.getGradeCode());
        dto.setChargeRate(lg.getChargeRate());
        return dto;
    }

    public List<LaborGradeDTO> toDTOList(List<LaborGrade> list) {
        return list.stream()
                   .map(this::toDTO)
                   .collect(Collectors.toList());
    }

    public LaborReportDTO generateLaborReport(String projectId, String wpId, Integer employeeId, LocalDate weekEnding) {
        Set<String> filteredWpIds = resolveSelectedWorkPackageIds(wpId);

        List<Object[]> rawRows = em.createQuery(
                "SELECT p.projId, " +
                "       p.projName, " +
                "       e.empId, " +
                "       e.empFirstName, " +
                "       e.empLastName, " +
                "       wp.wpId, " +
                "       wp.wpName, " +
                "       lg.laborGradeId, " +
                "       lg.gradeCode, " +
                "       lg.chargeRate, " +
                "       t.weekEnding, " +
                "       t.timesheetStatus, " +
                "       (COALESCE(tr.monday, 0) + COALESCE(tr.tuesday, 0) + COALESCE(tr.wednesday, 0) + " +
                "        COALESCE(tr.thursday, 0) + COALESCE(tr.friday, 0) + COALESCE(tr.saturday, 0) + COALESCE(tr.sunday, 0)), " +
                "       e.expectedWeeklyHours " +
                "FROM TimesheetRow tr " +
                "JOIN tr.timesheet t " +
                "JOIN t.employee e " +
                "JOIN tr.workPackage wp " +
                "JOIN wp.project p " +
                "JOIN tr.laborGrade lg " +
                "WHERE (:projectId IS NULL OR p.projId = :projectId) " +
                "  AND (:filterByWp = FALSE OR wp.wpId IN :wpIds) " +
                "  AND (:employeeId IS NULL OR e.empId = :employeeId) " +
                "  AND (:weekEnding IS NULL OR t.weekEnding = :weekEnding) " +
                "ORDER BY t.weekEnding DESC, e.empLastName, e.empFirstName, wp.wpId, lg.gradeCode",
                Object[].class)
                .setParameter("projectId", projectId)
                .setParameter("filterByWp", !filteredWpIds.isEmpty())
                .setParameter("wpIds", parameterizeWpIds(filteredWpIds))
                .setParameter("employeeId", employeeId)
                .setParameter("weekEnding", weekEnding)
                .getResultList();

        List<LaborReportRowDTO> rows = new ArrayList<>();
        BigDecimal totalHours = BigDecimal.ZERO;
        BigDecimal pendingApprovalHours = BigDecimal.ZERO;
        BigDecimal overtimeHours = BigDecimal.ZERO;
        Set<String> activeWorkPackages = new HashSet<>();
        Map<String, BigDecimal> employeeWeekHours = new HashMap<>();
        Map<String, BigDecimal> employeeWeekExpectedHours = new HashMap<>();

        for (Object[] rawRow : rawRows) {
            String rowProjectId = (String) rawRow[0];
            String rowProjectName = (String) rawRow[1];
            Integer rowEmpId = (Integer) rawRow[2];
            String rowFirstName = (String) rawRow[3];
            String rowLastName = (String) rawRow[4];
            String rowWpId = (String) rawRow[5];
            String rowWpName = (String) rawRow[6];
            Integer rowLaborGradeId = (Integer) rawRow[7];
            String rowLaborGradeCode = (String) rawRow[8];
            BigDecimal rowChargeRate = (BigDecimal) rawRow[9];
            LocalDate rowWeekEnding = (LocalDate) rawRow[10];
            TimesheetStatus rowStatus = (TimesheetStatus) rawRow[11];
            BigDecimal rowHours = nz((BigDecimal) rawRow[12]);
            BigDecimal rowExpectedWeeklyHours = nz((BigDecimal) rawRow[13]);

            LaborReportRowDTO row = new LaborReportRowDTO();
            row.setProjectId(rowProjectId);
            row.setProjectName(rowProjectName);
            row.setEmpId(rowEmpId);
            row.setEmployeeName(rowFirstName + " " + rowLastName);
            row.setWpId(rowWpId);
            row.setWorkPackageName(rowWpName);
            row.setLaborGradeId(rowLaborGradeId);
            row.setLaborGradeCode(rowLaborGradeCode);
            row.setChargeRate(rowChargeRate);
            row.setHours(rowHours);
            row.setWeekEnding(rowWeekEnding);
            row.setStatus(rowStatus.name());
            row.setStatusLabel(toStatusLabel(rowStatus));
            rows.add(row);

            totalHours = totalHours.add(rowHours);
            if (rowStatus == TimesheetStatus.SUBMITTED) {
                pendingApprovalHours = pendingApprovalHours.add(rowHours);
            }
            activeWorkPackages.add(rowWpId);

            String employeeWeekKey = rowEmpId + "|" + rowWeekEnding;
            employeeWeekHours.merge(employeeWeekKey, rowHours, BigDecimal::add);
            employeeWeekExpectedHours.putIfAbsent(employeeWeekKey, rowExpectedWeeklyHours);
        }

        for (Map.Entry<String, BigDecimal> employeeWeekEntry : employeeWeekHours.entrySet()) {
            BigDecimal expectedHours = employeeWeekExpectedHours.getOrDefault(employeeWeekEntry.getKey(), BigDecimal.ZERO);
            BigDecimal extraHours = employeeWeekEntry.getValue().subtract(expectedHours);
            if (extraHours.compareTo(BigDecimal.ZERO) > 0) {
                overtimeHours = overtimeHours.add(extraHours);
            }
        }

        LocalDate baselineWeekEnding = weekEnding;
        if (baselineWeekEnding == null && !rows.isEmpty()) {
            baselineWeekEnding = rows.get(0).getWeekEnding();
        }

        BigDecimal previousWeekHours = BigDecimal.ZERO;
        if (baselineWeekEnding != null) {
            previousWeekHours = getHoursForWeek(projectId, filteredWpIds, employeeId, baselineWeekEnding.minusWeeks(1));
        }

        LaborReportDTO.SummaryDTO summary = new LaborReportDTO.SummaryDTO();
        summary.setTotalHours(totalHours);
        summary.setPreviousWeekHours(previousWeekHours);
        summary.setHoursChangePercent(calculatePercentChange(previousWeekHours, totalHours));
        summary.setOvertimeHours(overtimeHours);
        summary.setPendingApprovalHours(pendingApprovalHours);
        summary.setActiveWorkPackages(activeWorkPackages.size());

        LaborReportDTO report = new LaborReportDTO();
        report.setProjectId(projectId);
        report.setWpId(wpId);
        report.setEmployeeId(employeeId);
        report.setWeekEnding(weekEnding);
        report.setSummary(summary);
        report.setRows(rows);
        report.setTotalRowHours(totalHours);
        return report;
    }

    private BigDecimal getHoursForWeek(String projectId, Set<String> wpIds, Integer employeeId, LocalDate weekEnding) {
        BigDecimal hours = em.createQuery(
                "SELECT COALESCE(SUM(COALESCE(tr.monday, 0) + COALESCE(tr.tuesday, 0) + COALESCE(tr.wednesday, 0) + " +
                "                    COALESCE(tr.thursday, 0) + COALESCE(tr.friday, 0) + COALESCE(tr.saturday, 0) + " +
                "                    COALESCE(tr.sunday, 0)), 0) " +
                "FROM TimesheetRow tr " +
                "JOIN tr.timesheet t " +
                "JOIN t.employee e " +
                "JOIN tr.workPackage wp " +
                "JOIN wp.project p " +
                "WHERE (:projectId IS NULL OR p.projId = :projectId) " +
                "  AND (:filterByWp = FALSE OR wp.wpId IN :wpIds) " +
                "  AND (:employeeId IS NULL OR e.empId = :employeeId) " +
                "  AND t.weekEnding = :weekEnding",
                BigDecimal.class)
                .setParameter("projectId", projectId)
                .setParameter("filterByWp", !wpIds.isEmpty())
                .setParameter("wpIds", parameterizeWpIds(wpIds))
                .setParameter("employeeId", employeeId)
                .setParameter("weekEnding", weekEnding)
                .getSingleResult();
        return nz(hours);
    }

    private Set<String> resolveSelectedWorkPackageIds(String wpId) {
        Set<String> wpIds = new HashSet<>();
        if (wpId == null || wpId.isBlank()) {
            return wpIds;
        }

        WorkPackage selected = em.find(WorkPackage.class, wpId);
        if (selected == null) {
            throw new NotFoundException("WorkPackage with id " + wpId + " not found.");
        }

        wpIds.add(wpId);
        Set<String> frontier = new HashSet<>();
        frontier.add(wpId);

        while (!frontier.isEmpty()) {
            List<String> childIds = em.createQuery(
                    "SELECT wp.wpId FROM WorkPackage wp WHERE wp.parentWorkPackage.wpId IN :parentIds",
                    String.class)
                    .setParameter("parentIds", frontier)
                    .getResultList();

            frontier = new HashSet<>();
            for (String childId : childIds) {
                if (wpIds.add(childId)) {
                    frontier.add(childId);
                }
            }
        }

        return wpIds;
    }

    private Collection<String> parameterizeWpIds(Set<String> wpIds) {
        return wpIds.isEmpty() ? List.of("__NO_MATCH__") : wpIds;
    }

    private BigDecimal calculatePercentChange(BigDecimal previousValue, BigDecimal currentValue) {
        if (previousValue == null || previousValue.compareTo(BigDecimal.ZERO) == 0) {
            return currentValue.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : new BigDecimal("100.00");
        }
        return currentValue.subtract(previousValue)
                .multiply(new BigDecimal("100"))
                .divide(previousValue, 2, RoundingMode.HALF_UP);
    }

    private String toStatusLabel(TimesheetStatus status) {
        if (status == TimesheetStatus.SUBMITTED) {
            return "PENDING";
        }
        return status.name();
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
