package com.corejsf.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "Project")
public class Project {

    @Id
    @Column(name = "proj_id", nullable = false)
    private String projId;

    @Enumerated(EnumType.STRING)
    @Column(name = "proj_type")
    private ProjectType projType;

    @JsonbTransient
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

    @JsonbTransient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Employee createdBy;

    @JsonbTransient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    private Employee modifiedBy;

    @Column(name = "markup_rate", precision = 5, scale = 2)
    private BigDecimal markupRate;

    @Transient
    private Integer projectManagerId;

    public Project() {
    }

    // --- JSON property name mappings for frontend compatibility ---

    @JsonbProperty("project_id")
    public String getProjId() {
        return projId;
    }

    @JsonbProperty("project_id")
    public void setProjId(final String projId) {
        this.projId = projId;
    }

    @JsonbProperty("project_type")
    public ProjectType getProjType() {
        return projType;
    }

    @JsonbProperty("project_type")
    public void setProjType(final ProjectType projType) {
        this.projType = projType;
    }

    @JsonbProperty("project_name")
    public String getProjName() {
        return projName;
    }

    @JsonbProperty("project_name")
    public void setProjName(final String projName) {
        this.projName = projName;
    }

    @JsonbProperty("project_desc")
    public String getDescription() {
        return description;
    }

    @JsonbProperty("project_desc")
    public void setDescription(final String description) {
        this.description = description;
    }

    @JsonbProperty("project_status")
    public ProjectStatus getStatus() {
        return status;
    }

    @JsonbProperty("project_status")
    public void setStatus(final ProjectStatus status) {
        this.status = status;
    }

    @JsonbProperty("start_date")
    public LocalDate getStartDate() {
        return startDate;
    }

    @JsonbProperty("start_date")
    public void setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }

    @JsonbProperty("end_date")
    public LocalDate getEndDate() {
        return endDate;
    }

    @JsonbProperty("end_date")
    public void setEndDate(final LocalDate endDate) {
        this.endDate = endDate;
    }

    @JsonbProperty("markup_rate")
    public BigDecimal getMarkupRate() {
        return markupRate;
    }

    @JsonbProperty("markup_rate")
    public void setMarkupRate(final BigDecimal markupRate) {
        this.markupRate = markupRate;
    }

    // --- Transient raw-ID accessors for the REST contract ---

    @JsonbProperty("project_manager_id")
    public Integer getProjectManagerId() {
        if (projectManagerId != null) return projectManagerId;
        return projectManager != null ? projectManager.getEmpId() : null;
    }

    @JsonbProperty("project_manager_id")
    public void setProjectManagerId(Integer projectManagerId) {
        this.projectManagerId = projectManagerId;
    }

    @JsonbProperty("created_by_id")
    public Integer getCreatedById() {
        return createdBy != null ? createdBy.getEmpId() : null;
    }

    @JsonbProperty("modified_by_id")
    public Integer getModifiedById() {
        return modifiedBy != null ? modifiedBy.getEmpId() : null;
    }

    // --- JPA relationship accessors (hidden from JSON) ---

    public Employee getProjectManager() {
        return projectManager;
    }

    public void setProjectManager(final Employee projectManager) {
        this.projectManager = projectManager;
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

    // --- Remaining accessors ---

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
}
