package com.corejsf.Entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "Timesheet_Row")
public class TimesheetRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(name = "ts_row_saturday", precision = 4, scale = 1, nullable = true)
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

    public TimesheetRow() {
        // Default constructor for JPA
    }

    //setter getters
    public Integer getTsRowId() {
        return tsRowId;
    }

    public void setTsRowId(Integer tsRowId) {
        this.tsRowId = tsRowId;
    }

    public BigDecimal getMonday() {
        return monday;
    }

    public void setMonday(BigDecimal monday) {
        this.monday = monday;
    }

    public BigDecimal getTuesday() {
        return tuesday;
    }

    public void setTuesday(BigDecimal tuesday) {
        this.tuesday = tuesday;
    }

    public BigDecimal getWednesday() {
        return wednesday;
    }

    public void setWednesday(BigDecimal wednesday) {
        this.wednesday = wednesday;
    }

    public BigDecimal getThursday() {
        return thursday;
    }

    public void setThursday(BigDecimal thursday) {
        this.thursday = thursday;
    }

    public BigDecimal getFriday() {
        return friday;
    }

    public void setFriday(BigDecimal friday) {
        this.friday = friday;
    }

    public BigDecimal getSaturday() {
        return saturday;
    }

    public void setSaturday(BigDecimal saturday) {
        this.saturday = saturday;
    }

    public BigDecimal getSunday() {
        return sunday;
    }

    public void setSunday(BigDecimal sunday) {
        this.sunday = sunday;
    }

    public LaborGrade getLaborGrade() {
        return laborGrade;
    }

    public void setLaborGrade(LaborGrade laborGrade) {
        this.laborGrade = laborGrade;
    }

    public WorkPackage getWorkPackage() {
        return workPackage;
    }

    public void setWorkPackage(WorkPackage workPackage) {
        this.workPackage = workPackage;
    }

    public Timesheet getTimesheet() {
        return timesheet;
    }

    public void setTimesheet(Timesheet timesheet) {
        this.timesheet = timesheet;
    }


}

