package ca.bcit.infosys.workpackage;

import ca.bcit.infosys.employee.Employee;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Entity for the Work_Package_Assignment table.
 * Links an employee to a work package.
 */
@Entity
@Table(name = "Work_Package_Assignment")
public class WorkPackageAssignment implements Serializable {

    /**
     * Auto-generated assignment ID.
     * Note: the DB has no AUTO_INCREMENT, so IDs are generated manually in the
     * controller.
     */
    @Id
    @Column(name = "wpa_id", nullable = false)
    private Integer wpaId;

    /**
     * The assigned employee's ID (FK to Employee table).
     */
    @Column(name = "emp_id", nullable = false)
    private Integer empId;

    /**
     * JPA relationship to the Employee.
     * Hidden from JSON to avoid proxy issues.
     */
    @JsonbTransient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", referencedColumnName = "emp_id", insertable = false, updatable = false)
    private Employee employee;

    /**
     * The work package ID this assignment belongs to.
     */
    @Column(name = "wp_id", nullable = false)
    private String wpId;

    /**
     * JPA relationship to the WorkPackage.
     * Hidden from JSON to avoid proxy issues.
     */
    @JsonbTransient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wp_id", referencedColumnName = "wp_id", insertable = false, updatable = false)
    private WorkPackage workPackage;

    /**
     * When this assignment was created.
     */
    @Column(name = "assignment_date", nullable = false)
    private LocalDate assignmentDate;

    /**
     * Default no-arg constructor, required by JPA.
     */
    public WorkPackageAssignment() {
    }

    // Getters and Setters

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

    public Employee getEmployee() {
        return employee;
    }

    public String getWpId() {
        return wpId;
    }

    public void setWpId(String wpId) {
        this.wpId = wpId;
    }

    public WorkPackage getWorkPackage() {
        return workPackage;
    }

    public LocalDate getAssignmentDate() {
        return assignmentDate;
    }

    public void setAssignmentDate(LocalDate assignmentDate) {
        this.assignmentDate = assignmentDate;
    }
}
