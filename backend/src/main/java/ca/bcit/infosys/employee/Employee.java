package ca.bcit.infosys.employee;

import java.io.Serializable;
import java.math.BigDecimal;
import jakarta.persistence.*;

/**
 * Entity for the Employee table.
 * Maps directly to the DB schema, just fields, getters, and setters.
 */
@Entity
@Table(name = "Employee")
public class Employee implements Serializable {

    /**
     * The unique employee ID.
     */
    @Id
    @Column(name = "emp_id")
    private int empId;

    /**
     * Employee's first name.
     */
    @Column(name = "emp_first_name", nullable = false)
    private String empFirstName;

    /**
     * Employee's last name.
     */
    @Column(name = "emp_last_name", nullable = false)
    private String empLastName;

    /**
     * Hashed password.
     */
    @Column(name = "emp_password", nullable = false)
    private String empPassword;

    /**
     * System role: HR, ADMIN, or EMPLOYEE.
     */
    @Column(name = "system_role")
    @Enumerated(EnumType.STRING)
    private SystemRole systemRole;

    /**
     * FK to the Employee_E_Signature table.
     */
    @Column(name = "emp_e_sig_id", nullable = false)
    private int empESigId;

    /**
     * FK to the Labor_Grade table.
     */
    @Column(name = "labor_grade_id", nullable = false)
    private int laborGradeId;

    /**
     * The employee's supervisor (FK to Employee table).
     */
    @Column(name = "supervisor_id", nullable = false)
    private int supervisorId;

    /**
     * Remaining vacation/sick balance in hours.
     */
    @Column(name = "vacation_sick_balance")
    private BigDecimal vacationSickBalance;

    /**
     * Expected hours per week.
     */
    @Column(name = "expected_weekly_hours", nullable = false)
    private BigDecimal expectedWeeklyHours;

    /**
     * Default no-arg constructor, required by JPA.
     */
    public Employee() {
    }

    // Getters and Setters

    public int getEmpId() {
        return empId;
    }

    public void setEmpId(int empId) {
        this.empId = empId;
    }

    public String getEmpFirstName() {
        return empFirstName;
    }

    public void setEmpFirstName(String empFirstName) {
        this.empFirstName = empFirstName;
    }

    public String getEmpLastName() {
        return empLastName;
    }

    public void setEmpLastName(String empLastName) {
        this.empLastName = empLastName;
    }

    public String getEmpPassword() {
        return empPassword;
    }

    public void setEmpPassword(String empPassword) {
        this.empPassword = empPassword;
    }

    public SystemRole getSystemRole() {
        return systemRole;
    }

    public void setSystemRole(SystemRole systemRole) {
        this.systemRole = systemRole;
    }

    public int getEmpESigId() {
        return empESigId;
    }

    public void setEmpESigId(int empESigId) {
        this.empESigId = empESigId;
    }

    public int getLaborGradeId() {
        return laborGradeId;
    }

    public void setLaborGradeId(int laborGradeId) {
        this.laborGradeId = laborGradeId;
    }

    public int getSupervisorId() {
        return supervisorId;
    }

    public void setSupervisorId(int supervisorId) {
        this.supervisorId = supervisorId;
    }

    public BigDecimal getVacationSickBalance() {
        return vacationSickBalance;
    }

    public void setVacationSickBalance(BigDecimal vacationSickBalance) {
        this.vacationSickBalance = vacationSickBalance;
    }

    public BigDecimal getExpectedWeeklyHours() {
        return expectedWeeklyHours;
    }

    public void setExpectedWeeklyHours(BigDecimal expectedWeeklyHours) {
        this.expectedWeeklyHours = expectedWeeklyHours;
    }
}
