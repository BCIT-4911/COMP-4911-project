package ca.bcit.infosys.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "Work_Package_Assignment")
public class WorkPackageAssignment implements Serializable {

    @Id
    @Column(name = "wpa_id", nullable = false)
    private Integer wpaId;

    @Column(name = "emp_id", nullable = false)
    private Integer empId;

    @Column(name = "wp_id", nullable = false)
    private String wpId;

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

    public Integer getEmpId() {
        return empId;
    }

    public void setEmpId(Integer empId) {
        this.empId = empId;
    }

    public String getWpId() {
        return wpId;
    }

    public void setWpId(String wpId) {
        this.wpId = wpId;
    }

    public LocalDate getAssignmentDate() {
        return assignmentDate;
    }

    public void setAssignmentDate(LocalDate assignmentDate) {
        this.assignmentDate = assignmentDate;
    }
}
