package com.corejsf.Entity;

import java.time.LocalDate;
import jakarta.persistence.*;

@Entity
@Table(name = "Project_Assignment")
public class ProjectAssignment {

    @Id
    @Column(name = "pa_id")
    private Integer paId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emp_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proj_id", nullable = false)
    private Project project;

    @Column(name = "assignment_date", nullable = false)
    private LocalDate assignmentDate;
}

