package com.corejsf.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;

@Entity
@Table(name = "Work_Package")
public class WorkPackage {

    @Id
    @Column(name = "wp_id", nullable = false, length = 255)
    private String wpId;

    @Column(name = "wp_name", length = 255)
    private String wpName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JsonbTransient
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proj_id", nullable = false)
    private Project project;

    @JsonbTransient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_wp_id")
    private WorkPackage parentWorkPackage;

    @Enumerated(EnumType.STRING)
    @Column(name = "wp_type")
    private WorkPackageType wpType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private WorkPackageStatus status;

    @Column(name = "structure_locked")
    private Boolean structureLocked;

    @Column(name = "budgeted_effort", precision = 10, scale = 2)
    private BigDecimal budgetedEffort;

    @Column(name = "bcws", precision = 12, scale = 2)
    private BigDecimal bcws;

    @Column(name = "plan_start_date", nullable = false)
    private LocalDate planStartDate;

    @Column(name = "plan_end_date", nullable = false)
    private LocalDate planEndDate;

    @JsonbTransient
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "re_employee_id", nullable = false)
    private Employee responsibleEmployee;

    @Column(name = "bac", precision = 12, scale = 2)
    private BigDecimal bac;

    @Column(name = "percent_complete", precision = 5, scale = 2)
    private BigDecimal percentComplete;

    @Column(name = "eac", precision = 12, scale = 2)
    private BigDecimal eac;

    @Column(name = "cv", precision = 12, scale = 2)
    private BigDecimal cv;

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

    @Column(name = "work_accomplished", length = 255)
    private String workAccomplished;

    @Column(name = "work_planned", length = 255)
    private String workPlanned;

    @Column(name = "problems", length = 255)
    private String problems;

    @Column(name = "anticipated_problems", length = 255)
    private String anticipatedProblems;

    @Transient
    private String projIdTransient;

    @Transient
    private String parentWpIdTransient;

    @Transient
    private Integer reEmployeeIdTransient;

    public WorkPackage() {
    }

    // --- Transient raw-ID accessors for the REST contract ---

    @JsonbProperty("projId")
    public String getProjId() {
        if (projIdTransient != null) return projIdTransient;
        return project != null ? project.getProjId() : null;
    }

    @JsonbProperty("projId")
    public void setProjId(String projId) {
        this.projIdTransient = projId;
    }

    @JsonbProperty("parentWpId")
    public String getParentWpId() {
        if (parentWpIdTransient != null) return parentWpIdTransient;
        return parentWorkPackage != null ? parentWorkPackage.getWpId() : null;
    }

    @JsonbProperty("parentWpId")
    public void setParentWpId(String parentWpId) {
        this.parentWpIdTransient = parentWpId;
    }

    @JsonbProperty("reEmployeeId")
    public Integer getReEmployeeId() {
        if (reEmployeeIdTransient != null) return reEmployeeIdTransient;
        return responsibleEmployee != null ? responsibleEmployee.getEmpId() : null;
    }

    @JsonbProperty("reEmployeeId")
    public void setReEmployeeId(Integer reEmployeeId) {
        this.reEmployeeIdTransient = reEmployeeId;
    }

    // --- Standard getters/setters ---

    public String getWpId() {
        return wpId;
    }

    public void setWpId(final String wpId) {
        this.wpId = wpId;
    }

    public String getWpName() {
        return wpName;
    }

    public void setWpName(final String wpName) {
        this.wpName = wpName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public WorkPackageType getWpType() {
        return wpType;
    }

    public void setWpType(final WorkPackageType wpType) {
        this.wpType = wpType;
    }

    public WorkPackageStatus getStatus() {
        return status;
    }

    public void setStatus(final WorkPackageStatus status) {
        this.status = status;
    }

    public Boolean getStructureLocked() {
        return structureLocked;
    }

    public void setStructureLocked(final Boolean structureLocked) {
        this.structureLocked = structureLocked;
    }

    public BigDecimal getBudgetedEffort() {
        return budgetedEffort;
    }

    public void setBudgetedEffort(final BigDecimal budgetedEffort) {
        this.budgetedEffort = budgetedEffort;
    }

    public BigDecimal getBcws() {
        return bcws;
    }

    public void setBcws(final BigDecimal bcws) {
        this.bcws = bcws;
    }

    public LocalDate getPlanStartDate() {
        return planStartDate;
    }

    public void setPlanStartDate(final LocalDate planStartDate) {
        this.planStartDate = planStartDate;
    }

    public LocalDate getPlanEndDate() {
        return planEndDate;
    }

    public void setPlanEndDate(final LocalDate planEndDate) {
        this.planEndDate = planEndDate;
    }

    public BigDecimal getBac() {
        return bac;
    }

    public void setBac(final BigDecimal bac) {
        this.bac = bac;
    }

    public BigDecimal getPercentComplete() {
        return percentComplete;
    }

    public void setPercentComplete(final BigDecimal percentComplete) {
        this.percentComplete = percentComplete;
    }

    public BigDecimal getEac() {
        return eac;
    }

    public void setEac(final BigDecimal eac) {
        this.eac = eac;
    }

    public BigDecimal getCv() {
        return cv;
    }

    public void setCv(final BigDecimal cv) {
        this.cv = cv;
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

    public String getWorkAccomplished() {
        return workAccomplished;
    }

    public void setWorkAccomplished(final String workAccomplished) {
        this.workAccomplished = workAccomplished;
    }

    public String getWorkPlanned() {
        return workPlanned;
    }

    public void setWorkPlanned(final String workPlanned) {
        this.workPlanned = workPlanned;
    }

    public String getProblems() {
        return problems;
    }

    public void setProblems(final String problems) {
        this.problems = problems;
    }

    public String getAnticipatedProblems() {
        return anticipatedProblems;
    }

    public void setAnticipatedProblems(final String anticipatedProblems) {
        this.anticipatedProblems = anticipatedProblems;
    }

    // --- JPA relationship accessors (hidden from JSON) ---

    public Project getProject() {
        return project;
    }

    public void setProject(final Project project) {
        this.project = project;
    }

    public WorkPackage getParentWorkPackage() {
        return parentWorkPackage;
    }

    public void setParentWorkPackage(final WorkPackage parentWorkPackage) {
        this.parentWorkPackage = parentWorkPackage;
    }

    public Employee getResponsibleEmployee() {
        return responsibleEmployee;
    }

    public void setResponsibleEmployee(final Employee responsibleEmployee) {
        this.responsibleEmployee = responsibleEmployee;
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
}
