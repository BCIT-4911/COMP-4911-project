package ca.bcit.infosys.project;

import java.io.Serializable;
import java.time.LocalDate;

import ca.bcit.infosys.employee.Employee;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;

/**
 * Entity for the Project_Assignment table.
 * Links an employee to a project.
 */
@Entity
@Table(name = "Project_Assignment")
public class ProjectAssignment implements Serializable {

    /**
     * Assignment ID.
     * Note: the DB has no AUTO_INCREMENT, so IDs are generated manually in the
     * controller.
     */
    @Id
    @Column(name = "pa_id")
    private int paId;

    /**
     * The assigned employee's ID.
     */
    @Column(name = "emp_id", insertable = false, updatable = false)
    private Integer empId;

    /**
     * JPA relationship to the Employee.
     * Hidden from JSON to avoid proxy issues.
     */
    @JsonbTransient
    @ManyToOne
    @JoinColumn(name = "emp_id", referencedColumnName = "emp_id")
    private Employee employee;

    /**
     * The project ID this assignment belongs to.
     */
    @Column(name = "proj_id", insertable = false, updatable = false)
    private String projId;

    /**
     * JPA relationship to the Project.
     * Hidden from JSON to avoid proxy issues.
     */
    @JsonbTransient
    @ManyToOne
    @JoinColumn(name = "proj_id", referencedColumnName = "proj_id")
    private Project project;

    /**
     * When this assignment was created.
     */
    @Column(name = "assignment_date", nullable = false)
    private LocalDate assignmentDate;

    /**
     * Default no-arg constructor, required by JPA.
     */
    public ProjectAssignment() {
    }

    // Getters and Setters

    public int getPaId() {
        return paId;
    }

    public void setPaId(int paId) {
        this.paId = paId;
    }

    public Integer getEmpId() {
        return empId;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public String getProjId() {
        return projId;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public LocalDate getAssignmentDate() {
        return assignmentDate;
    }

    public void setAssignmentDate(LocalDate assignmentDate) {
        this.assignmentDate = assignmentDate;
    }
}
