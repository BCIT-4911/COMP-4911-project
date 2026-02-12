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
}
