package com.corejsf.Entity;

import java.time.LocalDate;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;

@Entity
@Table(name = "Work_Package_Assignment")
public class WorkPackageAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wpa_id", nullable = false)
    private Integer wpaId;

    @JsonbTransient
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emp_id", nullable = false)
    private Employee employee;

    @JsonbTransient
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wp_id", nullable = false)
    private WorkPackage workPackage;

    @Column(name = "assignment_date", nullable = false)
    private LocalDate assignmentDate;

    public WorkPackageAssignment() {
    }

    public Integer getWpaId() {
        return wpaId;
    }

    public void setWpaId(Integer wpaId) {
        this.wpaId = wpaId;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public WorkPackage getWorkPackage() {
        return workPackage;
    }

    public void setWorkPackage(WorkPackage workPackage) {
        this.workPackage = workPackage;
    }

    public LocalDate getAssignmentDate() {
        return assignmentDate;
    }

    public void setAssignmentDate(LocalDate assignmentDate) {
        this.assignmentDate = assignmentDate;
    }
}
