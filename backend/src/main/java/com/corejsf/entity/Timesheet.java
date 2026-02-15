package com.corejsf.entity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;

import com.corejsf.employee.Employee;

/**
 * A class representing a single Timesheet.
 *
 *
 *
 *
 */
public class Timesheet implements java.io.Serializable {

    /** Number of days in a week. */
    public static final int DAYS_IN_WEEK = 7;

    /** Number of hours in a day as double. */
    public static final double HOURS_IN_DAY = 24.0;

    /** Number of work hours in week as double. */
    public static final double FULL_WORK_WEEK_HOURS = 40.0;

    /** Week fields of week ending on Friday. */
    public static final WeekFields FRIDAY_END
            = WeekFields.of(DayOfWeek.SATURDAY, 1);

    /** Serial version number. */
    private static final long serialVersionUID = 4L;

    /** Timesheet ID: Primary Key */
    private int timesheetId;

    /** Approver ID: Foreign Key */
    private int approverID;

    /** Approver Decision regarding Timesheet */
    private boolean approvalStatus;

    /** Comment explaining rejection of Timesheet */
    private String returnComment;

    /** Employee Signature ID: Foreign Key */
    private int empESigId;

    /** The employee id associated with this timesheet. */
    private Integer empId;

    /** The date of Friday for the week of the timesheet. */
    private LocalDate endDate;

    /** The List of all details (i.e. rows) that the form contains. */
    private List<TimesheetRow> details;


    /**
     * Constructor for Timesheet.
     * Initialize a Timesheet with no rows, no employee and
     * to the current date.
     */
    public Timesheet() {
        details = new ArrayList<>();
        endDate = LocalDate.now().
                with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
    }

    /**
     * Creates new timesheet with no rows.  Date is adjusted to Friday.
     * @param empId corresponding to employee
     * @param endDate date in timesheet week
     */
    public Timesheet(int empId, LocalDate endDate) {
        details = new ArrayList<>();
        this.empId = empId;
        this.endDate = endDate.
                with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
    }

    /**
     * Creates a Timesheet object with all fields set.
     * Date is adjusted to Friday.
     *
     * @param empId Corresponds to the owner of the timesheet
     * @param endDate The date of the end of the week for this
     *                 timesheet (Friday)
     * @param details The detailed hours charged for the week for this
     *        timesheet
     */
    public Timesheet(final int empId, final LocalDate endDate,
                     final List<TimesheetRow> details) {
        this.empId = empId;
        this.endDate = endDate.
                with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        this.details = details;
    }

    /**
     * Getter for time sheet owner.
     * @return the employee.
     */
    public int getEmpId() {
        return empId;
    }

    /**
     * Setter for time sheet owner.
     * Allows user to be null.
     * @param empId corresponds to the employee for the timesheet.
     */
    public void setEmpId(final int empId) {
        this.empId = empId;
    }

    /**
     * Getter for timesheet's end of week date.
     * @return the endWeek
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Sets the timesheet end of week. Adjusted to be next or same Friday.
     * @param end the endWeek to set.
     */
    public void setEndDate(final LocalDate end) {
        endDate = end.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
    }

    /**
     * Calculate the week number of the timesheet.
     * @return the calculated week number
     */
    public int getWeekNumber() {
        return endDate.get(FRIDAY_END.weekOfWeekBasedYear());
    }

    /**
     * Sets the end of week based on the week number.
     *
     * @param weekNo the week number of the timesheet week
     * @param weekYear the year of the timesheet
     */
    public void setWeekNumber(final int weekNo, final int weekYear) {
        final LocalDate weekByNumber =
                LocalDate.of(weekYear, 1, 1).
                        with(FRIDAY_END.weekOfWeekBasedYear(), weekNo);

        final TemporalAdjuster adjuster =
                TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY);
        endDate = weekByNumber.with(adjuster);
    }

    public int getApproverID() {
        return approverID;
    }

    public void setApproverID(int approverID) {
        this.approverID = approverID;
    }

    public boolean isApprovalStatus() {
        return approvalStatus;
    }

    /**
     * In later implementations, this will have to be locked except for approvers
     * @param approvalStatus
     */
    public void setApprovalStatus(boolean approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getReturnComment() {
        return returnComment;
    }

    /**
     * In later implementations, this will have to be locked except for approvers
     * @param returnComment
     */
    public void setReturnComment(String returnComment) {
        this.returnComment = returnComment;
    }

    public int getEmpESigId() {
        return empESigId;
    }

    public void setEmpESigId(int empESigId) {
        this.empESigId = empESigId;
    }

    /**
     * Calculate the time sheet's end date as a string.
     * @return the endWeek as string yyyy-mm-dd
     */
    public String getWeekEnding() {
        return endDate.toString();
    }

    /**
     * Getter for timesheet row details.
     * @return the details
     */
    public List<TimesheetRow> getDetails() {
        return details;
    }

    /**
     * Sets the details of the timesheet.
     *
     * @param newDetails new weekly charges to set
     */
    public void setDetails(final List<TimesheetRow> newDetails) {
        details = newDetails;
    }


    /**
     * Calculates the total hours.
     *
     * @return total hours for timesheet.
     */
    public float getTotalHours() {
        return getTotalDecihours() / TimesheetRow.BASE10;
    }

    /**
     * Calculates the total decihours.
     *
     * @return total decihours for timesheet.
     */
    public int getTotalDecihours() {
        int sum = 0;
        for (TimesheetRow row : details) {
            sum = sum + row.getDeciSum();
        }
        return sum;
    }

    /**
     * Calculates the daily total hours.
     *
     * @return array of total hours for each day of week for timesheet.
     */
    public float[] getDailyHours() {
        int[] deciSums = new int[DAYS_IN_WEEK];
        float[] sums = new float[DAYS_IN_WEEK];
        for (TimesheetRow day : details) {
            int[] hours = day.getDecihours();
            for (int i = 0; i < DAYS_IN_WEEK; i++) {
                deciSums[i] += hours[i];
            }
        }
        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            sums[i] = deciSums[i] / TimesheetRow.BASE10;
        }
        return sums;
    }

    /**
     * Calculates the daily total decihours.
     *
     * @return array of total hours for each day of week for timesheet.
     */
    public int[] getDailyDecihours() {
        int[] deciSums = new int[DAYS_IN_WEEK];
        for (TimesheetRow day : details) {
            int[] hours = day.getDecihours();
            for (int i = 0; i < DAYS_IN_WEEK; i++) {
                deciSums[i] += hours[i];
            }
        }
        return deciSums;
    }

    /**
     * Checks to see if timesheet total nets 40 hours.
     * @return true if FULL_WORK_WEEK == hours -flextime - overtime
     *     and at most one of flex time and overtime is non zero
     */
    public boolean isValid() {
        float total = getTotalHours();

        return (total == FULL_WORK_WEEK_HOURS);
    }



    /**
     * Deletes the specified row from the timesheet.
     *
     * @param rowToRemove
     *            the row to remove from the timesheet.
     */
    public void deleteRow(final TimesheetRow rowToRemove) {
        details.remove(rowToRemove);
    }

    /**
     * Add an empty row to the end of the timesheet details.
     */
    public void addRow() {
        details.add(new TimesheetRow());
    }

    @Override
    public String toString() {
        String result = empId.toString() + '\t' + endDate;
        for (TimesheetRow tsr : details) {
            result += '\n' + tsr.toString();
        }
        return result;
    }

}










