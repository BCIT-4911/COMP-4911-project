package com.corejsf.Entity;

import java.math.BigDecimal;
import jakarta.persistence.*;

/*
 * This class represents a single horizontal row within the timesheet grid. 
 * Instead of creating individual vertical entries for each logged hour, the ERD 
 * consolidates an entire week's worth of hours for a specific task into a single row. 
 * This structure aligns efficiently with the intended frontend UI grid representation.
 * * - tsRowId: The primary key identifying the row record.
 * - monday through sunday: Mapped using BigDecimal with precision=4 and scale=1 
 * to adhere to the ERD's DECIMAL(4,1) specification, allowing employees to log 
 * hours in 0.1 increments.
 * - laborGrade: A foreign key referencing Labor_Grade, required to determine the 
 * applicable billing rate for the hours logged in this row.
 * - workPackage: A foreign key referencing Work_Package, indicating the specific 
 * task or project the recorded hours are charged against.
 * - timesheet: A foreign key linking back to the parent Timesheet, associating 
 * the row with a specific weekly submission and its corresponding employee.
 */
@Entity
@Table(name = "Timesheet_Row")
public class TimesheetRow {

    @Id
    @Column(name = "ts_row_id", nullable = false)
    private Integer tsRowId;

    @Column(name = "ts_row_monday", precision = 4, scale = 1, nullable = false)
    private BigDecimal monday;

    @Column(name = "ts_row_tuesday", precision = 4, scale = 1, nullable = false)
    private BigDecimal tuesday;

    @Column(name = "ts_row_wednesday", precision = 4, scale = 1, nullable = false)
    private BigDecimal wednesday;

    @Column(name = "ts_row_thursday", precision = 4, scale = 1, nullable = false)
    private BigDecimal thursday;

    @Column(name = "ts_row_friday", precision = 4, scale = 1, nullable = false)
    private BigDecimal friday;

    @Column(name = "ts_row_saturday", precision = 4, scale = 1, nullable = false)
    private BigDecimal saturday;

    @Column(name = "ts_row_sunday", precision = 4, scale = 1, nullable = false)
    private BigDecimal sunday;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "labor_grade_id", nullable = false)
    private LaborGrade laborGrade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wp_id", nullable = false)
    private WorkPackage workPackage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ts_id", nullable = false)
    private Timesheet timesheet;

    // JPA requires an empty, no-argument constructor
    public TimesheetRow() {
    }

    // Getters and Setters are required by JPA to access and modify the fields
    public Integer getTsRowId() {
        return tsRowId;
    }

    public void setTsRowId(final Integer tsRowId) {
        this.tsRowId = tsRowId;
    }

    public BigDecimal getMonday() {
        return monday;
    }

    public void setMonday(final BigDecimal monday) {
        this.monday = monday;
    }

    public BigDecimal getTuesday() {
        return tuesday;
    }

    public void setTuesday(final BigDecimal tuesday) {
        this.tuesday = tuesday;
    }

    public BigDecimal getWednesday() {
        return wednesday;
    }

    public void setWednesday(final BigDecimal wednesday) {
        this.wednesday = wednesday;
    }

    public BigDecimal getThursday() {
        return thursday;
    }

    public void setThursday(final BigDecimal thursday) {
        this.thursday = thursday;
    }

    public BigDecimal getFriday() {
        return friday;
    }

    public void setFriday(final BigDecimal friday) {
        this.friday = friday;
    }

    public BigDecimal getSaturday() {
        return saturday;
    }

    public void setSaturday(final BigDecimal saturday) {
        this.saturday = saturday;
    }

    public BigDecimal getSunday() {
        return sunday;
    }

    public void setSunday(final BigDecimal sunday) {
        this.sunday = sunday;
    }

    public LaborGrade getLaborGrade() {
        return laborGrade;
    }

    public void setLaborGrade(final LaborGrade laborGrade) {
        this.laborGrade = laborGrade;
    }

    public WorkPackage getWorkPackage() {
        return workPackage;
    }

    public void setWorkPackage(final WorkPackage workPackage) {
        this.workPackage = workPackage;
    }

    public Timesheet getTimesheet() {
        return timesheet;
    }

    public void setTimesheet(final Timesheet timesheet) {
        this.timesheet = timesheet;
    }
}