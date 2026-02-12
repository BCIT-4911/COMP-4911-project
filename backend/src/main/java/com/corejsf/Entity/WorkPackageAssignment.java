package com.corejsf.Entity;

import java.time.LocalDate;
import jakarta.persistence.*;

@Entity
@Table(name = "Work_Package_Assignment")
public class WorkPackageAssignment {

    @Id
    @Column(name = "wpa_id")
    private Integer wpaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emp_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wp_id", nullable = false)
    private WorkPackage workPackage;

    @Column(name = "assignment_date", nullable = false)
    private LocalDate assignmentDate;
}

