package com.corejsf.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.corejsf.Entity.Employee;
import com.corejsf.Entity.TimesheetRow;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class LeaveBalanceService {

    private static final BigDecimal BASE_HOURS_CAP = new BigDecimal("40.0");
    private static final BigDecimal BASE_RATE = new BigDecimal("0.1");
    private static final BigDecimal OVERTIME_RATE = new BigDecimal("0.5");

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    public BigDecimal calculateApprovedLeaveHours(List<TimesheetRow> rows) {
        BigDecimal totalWorkedHours = sumTimesheetHours(rows);

        BigDecimal baseHours = totalWorkedHours.min(BASE_HOURS_CAP);
        BigDecimal overtimeHours = totalWorkedHours.subtract(BASE_HOURS_CAP);

        if (overtimeHours.compareTo(BigDecimal.ZERO) < 0) {
            overtimeHours = BigDecimal.ZERO;
        }

        BigDecimal baseLeave = baseHours.multiply(BASE_RATE);
        BigDecimal overtimeLeave = overtimeHours.multiply(OVERTIME_RATE);

        return baseLeave.add(overtimeLeave).setScale(2, RoundingMode.HALF_UP);
    }

    public void applyApprovedLeave(Employee employee, List<TimesheetRow> rows) {
        BigDecimal currentBalance = nz(employee.getVacationSickBalance());
        BigDecimal earnedThisApproval = calculateApprovedLeaveHours(rows);

        employee.setVacationSickBalance(currentBalance.add(earnedThisApproval));
        em.merge(employee);
    }

    private BigDecimal sumTimesheetHours(List<TimesheetRow> rows) {
        BigDecimal total = BigDecimal.ZERO;
        for (TimesheetRow row : rows) {
            total = total
                .add(nz(row.getMonday()))
                .add(nz(row.getTuesday()))
                .add(nz(row.getWednesday()))
                .add(nz(row.getThursday()))
                .add(nz(row.getFriday()))
                .add(nz(row.getSaturday()))
                .add(nz(row.getSunday()));
        }
        return total;
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
