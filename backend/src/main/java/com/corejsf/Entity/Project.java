package com.corejsf.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "Project")
public class Project {

    @Id
    @Column(name = "proj_id", nullable = false)
    private String projId;

    @Enumerated(EnumType.STRING)
    @Column(name = "proj_type")
    private ProjectType projType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pm_employee_id", nullable = false)
    private Employee projectManager;

    @Column(name = "proj_name")
    private String projName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ProjectStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Employee createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    private Employee modifiedBy;

    @Column(name = "markup_rate", precision = 5, scale = 2)
    private BigDecimal markupRate;

    
   
}

