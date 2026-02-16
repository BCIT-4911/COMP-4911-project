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

    public Project() {
        // Default constructor for JPA
    }


    //setter getters
    public String getProjId() {
        return projId;
    }

    public void setProjId(final String projId) {
        this.projId = projId;
    }

    public ProjectType getProjType() {
        return projType;
    }

    public void setProjType(final ProjectType projType) {
        this.projType = projType;
    }

    public Employee getProjectManager() {
        return projectManager;
    }

    public void setProjectManager(final Employee projectManager) {
        this.projectManager = projectManager;
    }

    public String getProjName() {
        return projName;
    }

    public void setProjName(final String projName) {
        this.projName = projName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(final ProjectStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(final LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(final LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(final LocalDateTime modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public Employee getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final Employee createdBy) {
        this.createdBy = createdBy;
    }

    public Employee getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(final Employee modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public BigDecimal getMarkupRate() {
        return markupRate;
    }

    public void setMarkupRate(final BigDecimal markupRate) {
        this.markupRate = markupRate;
    }


}

