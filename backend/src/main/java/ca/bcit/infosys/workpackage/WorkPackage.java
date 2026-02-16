package ca.bcit.infosys.workpackage;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Work_Package")
public class WorkPackage implements Serializable {

    @Id
    @Column(name = "wp_id", nullable = false)
    private String wpId;

    @Column(name = "wp_name")
    private String wpName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "proj_id", nullable = false)
    private String projId;

    @Column(name = "parent_wp_id")
    private String parentWpId;

    @Column(name = "wp_type")
    private WpType wpType;

    @Column(name = "status")
    private WpStatus status;

    @Column(name = "structure_locked", nullable = false)
    private Boolean structureLocked;

    @Column(name = "budgeted_effort", precision = 10, scale = 2)
    private BigDecimal budgetedEffort;

    @Column(name = "bcws", precision = 12, scale = 2)
    private BigDecimal bcws;

    @Column(name = "plan_start_date", nullable = false)
    private LocalDate planStartDate;

    @Column(name = "plan_end_date", nullable = false)
    private LocalDate planEndDate;

    @Column(name = "re_employee_id", nullable = false)
    private Integer reEmployeeId;

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

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "modified_by")
    private Integer modifiedBy;

    @Column(name = "work_accomplished")
    private String workAccomplished;

    @Column(name = "work_planned")
    private String workPlanned;

    @Column(name = "problems")
    private String problems;

    @Column(name = "anticipated_problems")
    private String anticipatedProblems;

    public WorkPackage() {
    }

    public String getWpId() {
        return wpId;
    }

    public void setWpId(String wpId) {
        this.wpId = wpId;
    }

    public String getWpName() {
        return wpName;
    }

    public void setWpName(String wpName) {
        this.wpName = wpName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProjId() {
        return projId;
    }

    public void setProjId(String projId) {
        this.projId = projId;
    }

    public String getParentWpId() {
        return parentWpId;
    }

    public void setParentWpId(String parentWpId) {
        this.parentWpId = parentWpId;
    }

    public WpType getWpType() {
        return wpType;
    }

    public void setWpType(WpType wpType) {
        this.wpType = wpType;
    }

    public WpStatus getStatus() {
        return status;
    }

    public void setStatus(WpStatus status) {
        this.status = status;
    }

    public Boolean getStructureLocked() {
        return structureLocked;
    }

    public void setStructureLocked(Boolean structureLocked) {
        this.structureLocked = structureLocked;
    }

    public BigDecimal getBudgetedEffort() {
        return budgetedEffort;
    }

    public void setBudgetedEffort(BigDecimal budgetedEffort) {
        this.budgetedEffort = budgetedEffort;
    }

    public BigDecimal getBcws() {
        return bcws;
    }

    public void setBcws(BigDecimal bcws) {
        this.bcws = bcws;
    }

    public LocalDate getPlanStartDate() {
        return planStartDate;
    }

    public void setPlanStartDate(LocalDate planStartDate) {
        this.planStartDate = planStartDate;
    }

    public LocalDate getPlanEndDate() {
        return planEndDate;
    }

    public void setPlanEndDate(LocalDate planEndDate) {
        this.planEndDate = planEndDate;
    }

    public Integer getReEmployeeId() {
        return reEmployeeId;
    }

    public void setReEmployeeId(Integer reEmployeeId) {
        this.reEmployeeId = reEmployeeId;
    }

    public BigDecimal getBac() {
        return bac;
    }

    public void setBac(BigDecimal bac) {
        this.bac = bac;
    }

    public BigDecimal getPercentComplete() {
        return percentComplete;
    }

    public void setPercentComplete(BigDecimal percentComplete) {
        this.percentComplete = percentComplete;
    }

    public BigDecimal getEac() {
        return eac;
    }

    public void setEac(BigDecimal eac) {
        this.eac = eac;
    }

    public BigDecimal getCv() {
        return cv;
    }

    public void setCv(BigDecimal cv) {
        this.cv = cv;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(LocalDateTime modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(Integer modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public String getWorkAccomplished() {
        return workAccomplished;
    }

    public void setWorkAccomplished(String workAccomplished) {
        this.workAccomplished = workAccomplished;
    }

    public String getWorkPlanned() {
        return workPlanned;
    }

    public void setWorkPlanned(String workPlanned) {
        this.workPlanned = workPlanned;
    }

    public String getProblems() {
        return problems;
    }

    public void setProblems(String problems) {
        this.problems = problems;
    }

    public String getAnticipatedProblems() {
        return anticipatedProblems;
    }

    public void setAnticipatedProblems(String anticipatedProblems) {
        this.anticipatedProblems = anticipatedProblems;
    }
}