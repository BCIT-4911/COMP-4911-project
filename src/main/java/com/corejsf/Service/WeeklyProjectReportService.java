package com.corejsf.Service;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.corejsf.DTO.WeeklyProjectReportDTO;
import com.corejsf.Entity.TimesheetStatus;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Stateless
public class WeeklyProjectReportService {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    public WeeklyProjectReportDTO generateReport(String projectId) {
        LocalDate today = LocalDate.now();

        // Current week: Sunday-ending week that contains today
        LocalDate currentWeekEnding = today.with(
            java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        // Current month boundaries
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        // JPQL query: fetch approved timesheet rows for WPs in this project
        List<Object[]> rows = em.createQuery(
            "SELECT tr.timesheet.employee.empId, " +
            "       tr.timesheet.employee.empFirstName, " +
            "       tr.timesheet.employee.empLastName, " +
            "       tr.workPackage.wpId, " +
            "       tr.workPackage.wpName, " +
            "       tr.timesheet.weekEnding, " +
            "       (tr.monday + tr.tuesday + tr.wednesday + tr.thursday + " +
            "        tr.friday + tr.saturday + tr.sunday) " +
            "FROM TimesheetRow tr " +
            "WHERE tr.workPackage.project.projId = :projId " +
            "  AND tr.timesheet.timesheetStatus = :status",
            Object[].class)
            .setParameter("projId", projectId)
            .setParameter("status", TimesheetStatus.APPROVED)
            .getResultList();

        // Group by employee -> then work package
Map<Integer, WeeklyProjectReportDTO.EmployeeReportEntryDTO> employeeMap = new HashMap<>();

for (Object[] row : rows) {
    int empId = (Integer) row[0];
    String firstName = (String) row[1];
    String lastName = (String) row[2];
    String wpId = (String) row[3];
    String wpName = (String) row[4];
    LocalDate weekEnding = ((java.sql.Date) row[5]).toLocalDate();
    BigDecimal hours = (BigDecimal) row[6];

    WeeklyProjectReportDTO.EmployeeReportEntryDTO empEntry =
            employeeMap.computeIfAbsent(empId, id -> {
                WeeklyProjectReportDTO.EmployeeReportEntryDTO e =
                        new WeeklyProjectReportDTO.EmployeeReportEntryDTO();
                e.setEmpId(empId);
                e.setEmpName(firstName + " " + lastName);
                e.setWorkPackages(new ArrayList<>());
                e.setCurrentWeekHours(BigDecimal.ZERO);
                e.setCurrentMonthHours(BigDecimal.ZERO);
                e.setProjectToDateHours(BigDecimal.ZERO);
                return e;
            });

    // Find or create WP entry
    WeeklyProjectReportDTO.WpHoursEntryDTO wpEntry = empEntry.getWorkPackages()
            .stream()
            .filter(wp -> wp.getWpId().equals(wpId))
            .findFirst()
            .orElseGet(() -> {
                WeeklyProjectReportDTO.WpHoursEntryDTO newWp =
                        new WeeklyProjectReportDTO.WpHoursEntryDTO();
                newWp.setWpId(wpId);
                newWp.setWpName(wpName);
                newWp.setCurrentWeekHours(BigDecimal.ZERO);
                newWp.setCurrentMonthHours(BigDecimal.ZERO);
                newWp.setProjectToDateHours(BigDecimal.ZERO);
                empEntry.getWorkPackages().add(newWp);
                return newWp;
            });

    // ---- Aggregations ----

    // Project-to-date (everything)
    wpEntry.setProjectToDateHours(
            wpEntry.getProjectToDateHours().add(hours));
    empEntry.setProjectToDateHours(
            empEntry.getProjectToDateHours().add(hours));

    // Current week
    if (weekEnding.equals(currentWeekEnding)) {
        wpEntry.setCurrentWeekHours(
                wpEntry.getCurrentWeekHours().add(hours));
        empEntry.setCurrentWeekHours(
                empEntry.getCurrentWeekHours().add(hours));
    }

    // Current month
    if (!weekEnding.isBefore(monthStart) && !weekEnding.isAfter(monthEnd)) {
        wpEntry.setCurrentMonthHours(
                wpEntry.getCurrentMonthHours().add(hours));
        empEntry.setCurrentMonthHours(
                empEntry.getCurrentMonthHours().add(hours));
    }
}

// Build final DTO
WeeklyProjectReportDTO report = new WeeklyProjectReportDTO();
report.setProjectId(projectId);

// fetch project name (safe fallback)
String projectName = em.createQuery(
        "SELECT p.projName FROM Project p WHERE p.projId = :id",
        String.class)
        .setParameter("id", projectId)
        .getResultStream()
        .findFirst()
        .orElse("Unknown Project");

report.setProjectName(projectName);
report.setEmployees(new ArrayList<>(employeeMap.values()));

return report;
    }
}