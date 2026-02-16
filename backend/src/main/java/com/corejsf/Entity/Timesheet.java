package com.corejsf.Entity;

import java.time.LocalDate;
import jakarta.persistence.*;

@Entity
@Table(name = "Timesheet")
public class Timesheet {

    @Id
    @Column(name = "ts_id")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_e_sig_id")
    private EmployeeESignature signature;

    public Timesheet() {
        // Default constructor for JPA
    }

    //setter getters
    public Integer getTsId() {
        return tsId;
    }

    public void setTsId(Integer tsId) {
        this.tsId = tsId;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public LocalDate getWeekEnding() {
        return weekEnding;
    }

    public void setWeekEnding(LocalDate weekEnding) {
        this.weekEnding = weekEnding;
    }

    public Employee getApprover() {
        return approver;
    }

    public void setApprover(Employee approver) {
        this.approver = approver;
    }

    public Boolean getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(Boolean approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getReturnComment() {
        return returnComment;
    }

    public void setReturnComment(String returnComment) {
        this.returnComment = returnComment;
    }

    public EmployeeESignature getSignature() {
        return signature;
    }

    public void setSignature(EmployeeESignature signature) {
        this.signature = signature;
    }


}
