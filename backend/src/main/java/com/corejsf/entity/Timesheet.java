package com.corejsf.Entity;

import java.time.LocalDate;
import java.util.List;
import jakarta.persistence.*;

/*
 * This class serves as the primary container for an employee's weekly timesheet submission.
 * * - tsId: The primary key identifying the timesheet record.
 * - employee: A foreign key linking to the specific employee who logged the hours.
 * - weekEnding: Represents the final day of the work week this timesheet covers.
 * - approver: A foreign key linking to the supervisor responsible for reviewing the submission.
 * - approvalStatus: A boolean flag indicating whether the timesheet has been approved. Setting this to true acts as a system lock to prevent further edits.
 * - returnComment: Stores the approver's feedback or reasoning if the timesheet is returned for correction.
 * - eSignature: A foreign key linking to the employee's electronic signature record, attached upon final submission.
 * * Note: A @OneToMany mapping for 'rows' is included. While not a direct column in the ERD's Timesheet table, this relationship is required by JPA to easily retrieve all associated TimesheetRow entries in the Java code.
 */
@Entity
@Table(name = "Timesheet")
public class Timesheet {

    @Id
    @Column(name = "ts_id", nullable = false)
    private Integer tsId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emp_id", nullable = false)
    private Employee employee;

    @Column(name = "week_ending", nullable = false)
    private LocalDate weekEnding;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approver_id", nullable = false)
    private Employee approver;

    @Column(name = "approval_status", nullable = false)
    private Boolean approvalStatus;

    @Column(name = "return_comment", columnDefinition = "TEXT")
    private String returnComment;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_e_sig_id")
    private EmployeeESignature eSignature;

    // Bi-directional mapping to grab all rows for this timesheet easily
    @OneToMany(mappedBy = "timesheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimesheetRow> rows;

    public Timesheet() {
    }

    public Integer getTsId() {
        return tsId;
    }

    public void setTsId(final Integer tsId) {
        this.tsId = tsId;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(final Employee employee) {
        this.employee = employee;
    }

    public LocalDate getWeekEnding() {
        return weekEnding;
    }

    public void setWeekEnding(final LocalDate weekEnding) {
        this.weekEnding = weekEnding;
    }

    public Employee getApprover() {
        return approver;
    }

    public void setApprover(final Employee approver) {
        this.approver = approver;
    }

    public Boolean getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(final Boolean approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getReturnComment() {
        return returnComment;
    }

    public void setReturnComment(final String returnComment) {
        this.returnComment = returnComment;
    }

    public EmployeeESignature geteSignature() {
        return eSignature;
    }

    public void seteSignature(final EmployeeESignature eSignature) {
        this.eSignature = eSignature;
    }

    public List<TimesheetRow> getRows() {
        return rows;
    }

    public void setRows(final List<TimesheetRow> rows) {
        this.rows = rows;
    }
}