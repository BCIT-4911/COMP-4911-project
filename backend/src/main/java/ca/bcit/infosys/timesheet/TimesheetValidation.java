package ca.bcit.infosys.timesheet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.corejsf.DTO.TimesheetRequestDTO;
import com.corejsf.DTO.TimesheetRowRequestDTO;
import com.corejsf.Entity.WorkPackage;
import com.corejsf.Entity.WorkPackageStatus;
import com.corejsf.Entity.WorkPackageType;

/**
 * Validation class for Timesheet operations.
 * Validates request DTOs before they reach the persistence layer.
 */
public final class TimesheetValidation {

    private static final BigDecimal INCREMENT = new BigDecimal("0.1");
    private static final BigDecimal MAX_DAILY_HOURS = new BigDecimal("24.0");
    private static final BigDecimal MAX_WEEKLY_HOURS = new BigDecimal("168.0"); 

    // -------------------------------------------------------------------------
    // Top-level validation entry points
    // -------------------------------------------------------------------------

    /**
     * Validates a full TimesheetRequestDTO for create or update.
     * Call this from the controller on POST and PUT.
     */
    public static void validateRequest(TimesheetRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Timesheet request cannot be null.");
        }
        validateEmpId(dto.getEmpId());
        validateWeekEnding(dto.getWeekEnding());
        validateRowsNotEmpty(dto.getRows());
        validateAllRowHours(dto.getRows());
    }

    /**
     * Validates that rows meet submission requirements (stricter than save-draft).
     * Call this from the controller on PUT /submit, after validateRequest.
     */
    public static void validateForSubmission(List<TimesheetRowRequestDTO> rows) {
        validateRowsNotEmpty(rows);
        validateWeeklyTotal(rows);
    }

    // -------------------------------------------------------------------------
    // Field-level validators
    // -------------------------------------------------------------------------

    public static void validateEmpId(Integer empId) {
        if (empId == null || empId <= 0) {
            throw new IllegalArgumentException("Employee ID must be a positive integer.");
        }
    }

    public static void validateWeekEnding(LocalDate weekEnding) {
        if (weekEnding == null) {
            throw new IllegalArgumentException("Week ending date is required.");
        }        
    }

    public static void validateRowsNotEmpty(List<TimesheetRowRequestDTO> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("Timesheet must contain at least one row.");
        }
    }

    // -------------------------------------------------------------------------
    // Hour validation (proves Risk #7 mitigation)
    // -------------------------------------------------------------------------

    /**
     * Rejects values that are not multiples of 0.1.
     * e.g. 1.3 is valid, 1.33 is rejected.
     */
    public static void validateHourIncrement(BigDecimal hours, String fieldName) {
        if (hours == null) {
            return; // null hours treated as 0 by the entity layer
        }
        if (hours.remainder(INCREMENT).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException(
                    fieldName + " value " + hours + " is not a valid 0.1h increment.");
        }
    }

    /**
     * Hours for a single day must be between 0.0 and 24.0 inclusive.
     */
    public static void validateHoursRange(BigDecimal hours, String fieldName) {
        if (hours == null) {
            return;
        }
        if (hours.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative.");
        }
        if (hours.compareTo(MAX_DAILY_HOURS) > 0) {
            throw new IllegalArgumentException(
                    fieldName + " cannot exceed " + MAX_DAILY_HOURS + " hours.");
        }
    }

    /**
     * Validates all 7 day columns on a single row for increment and range.
     */
    public static void validateRowHours(TimesheetRowRequestDTO row, int rowIndex) {
        String prefix = "Row " + (rowIndex + 1) + " ";
        validateHourIncrement(row.getMonday(), prefix + "Monday");
        validateHoursRange(row.getMonday(), prefix + "Monday");
        validateHourIncrement(row.getTuesday(), prefix + "Tuesday");
        validateHoursRange(row.getTuesday(), prefix + "Tuesday");
        validateHourIncrement(row.getWednesday(), prefix + "Wednesday");
        validateHoursRange(row.getWednesday(), prefix + "Wednesday");
        validateHourIncrement(row.getThursday(), prefix + "Thursday");
        validateHoursRange(row.getThursday(), prefix + "Thursday");
        validateHourIncrement(row.getFriday(), prefix + "Friday");
        validateHoursRange(row.getFriday(), prefix + "Friday");
        validateHourIncrement(row.getSaturday(), prefix + "Saturday");
        validateHoursRange(row.getSaturday(), prefix + "Saturday");
        validateHourIncrement(row.getSunday(), prefix + "Sunday");
        validateHoursRange(row.getSunday(), prefix + "Sunday");
    }

    /**
     * Validates hours on every row and checks that each row has a WP and labor grade.
     */
    public static void validateAllRowHours(List<TimesheetRowRequestDTO> rows) {
        for (int i = 0; i < rows.size(); i++) {
            TimesheetRowRequestDTO row = rows.get(i);
            validateRowWpId(row.getWpId(), i);
            validateRowLaborGradeId(row.getLaborGradeId(), i);
            validateRowHours(row, i);
        }
    }

    // -------------------------------------------------------------------------
    // Weekly total validation
    // -------------------------------------------------------------------------

    /**
     * Sums all 7-day columns across all rows. Rejects if total exceeds 168h or is negative.
     */
    public static void validateWeeklyTotal(List<TimesheetRowRequestDTO> rows) {
        BigDecimal total = BigDecimal.ZERO;
        for (TimesheetRowRequestDTO row : rows) {
            total = total.add(nz(row.getMonday()));
            total = total.add(nz(row.getTuesday()));
            total = total.add(nz(row.getWednesday()));
            total = total.add(nz(row.getThursday()));
            total = total.add(nz(row.getFriday()));
            total = total.add(nz(row.getSaturday()));
            total = total.add(nz(row.getSunday()));
        }
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Weekly total hours cannot be negative.");
        }
        if (total.compareTo(MAX_WEEKLY_HOURS) > 0) {
            throw new IllegalArgumentException(
                    "Weekly total hours (" + total + ") exceeds maximum of " + MAX_WEEKLY_HOURS + ".");
        }
    }

    /**
     * Ensures at least {@code minDays} distinct days have hours > 0 across all rows.
     * Per ASUC-1: employee must enter hours for 3+ days.
     */
    public static void validateMinimumDaysWorked(List<TimesheetRowRequestDTO> rows, int minDays) {
        BigDecimal[] dayTotals = new BigDecimal[7];
        for (int d = 0; d < 7; d++) {
            dayTotals[d] = BigDecimal.ZERO;
        }
        for (TimesheetRowRequestDTO row : rows) {
            dayTotals[0] = dayTotals[0].add(nz(row.getMonday()));
            dayTotals[1] = dayTotals[1].add(nz(row.getTuesday()));
            dayTotals[2] = dayTotals[2].add(nz(row.getWednesday()));
            dayTotals[3] = dayTotals[3].add(nz(row.getThursday()));
            dayTotals[4] = dayTotals[4].add(nz(row.getFriday()));
            dayTotals[5] = dayTotals[5].add(nz(row.getSaturday()));
            dayTotals[6] = dayTotals[6].add(nz(row.getSunday()));
        }
        int daysWithHours = 0;
        for (BigDecimal dt : dayTotals) {
            if (dt.compareTo(BigDecimal.ZERO) > 0) {
                daysWithHours++;
            }
        }
        if (daysWithHours < minDays) {
            throw new IllegalArgumentException(
                    "Timesheet must have hours entered for at least " + minDays
                            + " days. Currently only " + daysWithHours + " days have hours.");
        }
    }

    // -------------------------------------------------------------------------
    // State / immutability validation (proves Risk #8 mitigation)
    // -------------------------------------------------------------------------

    /**
     * Ensures the timesheet has not been approved.
     * An approved timesheet cannot be edited, submitted again, or deleted.
     */
    public static void validateNotApproved(Boolean approved) {
        if (Boolean.TRUE.equals(approved)) {
            throw new IllegalArgumentException(
                    "Cannot modify an approved timesheet. Approved timesheets are immutable.");
        }
    }

    // -------------------------------------------------------------------------
    // Work package validation (proves Risk #11 mitigation)
    // -------------------------------------------------------------------------

    /**
     * Validates that a work package is chargeable:
     * must be LOWEST_LEVEL type and OPEN_FOR_CHARGES status.
     */
    public static void validateWorkPackageChargeable(WorkPackage wp) {
        if (wp.getWpType() != WorkPackageType.LOWEST_LEVEL) {
            throw new IllegalArgumentException(
                    "Cannot charge to work package " + wp.getWpId()
                            + ": it is a summary package. Only lowest-level work packages accept charges.");
        }
        if (wp.getStatus() != WorkPackageStatus.OPEN_FOR_CHARGES) {
            throw new IllegalArgumentException(
                    "Cannot charge to work package " + wp.getWpId()
                            + ": it is not open for charges (current status: " + wp.getStatus() + ").");
        }
    }

    // -------------------------------------------------------------------------
    // Row field validators
    // -------------------------------------------------------------------------

    public static void validateRowWpId(String wpId, int rowIndex) {
        if (wpId == null || wpId.isEmpty()) {
            throw new IllegalArgumentException(
                    "Row " + (rowIndex + 1) + ": Work Package ID is required.");
        }
    }

    public static void validateRowLaborGradeId(Integer laborGradeId, int rowIndex) {
        if (laborGradeId == null || laborGradeId <= 0) {
            throw new IllegalArgumentException(
                    "Row " + (rowIndex + 1) + ": Labor Grade ID must be a positive integer.");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
