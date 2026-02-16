package com.corejsf.Entity;

import java.math.BigDecimal;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "Employee")
public class Employee {

    @Id
    @Column(name = "emp_id", nullable = false)
    private Integer empId;

    @Column(name = "emp_first_name", nullable = false, length = 255)
    private String empFirstName;

    @Column(name = "emp_last_name", nullable = false, length = 255)
    private String empLastName;

    @JsonbTransient
    @Column(name = "emp_password", nullable = false, length = 255)
    private String empPassword;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_role", nullable = true, length = 20)
    private SystemRole systemRole;

    @JsonbTransient
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emp_e_sig_id", nullable = false)
    private EmployeeESignature eSignature;

    @JsonbTransient
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "labor_grade_id", nullable = false)
    private LaborGrade laborGrade;

    @JsonbTransient
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "supervisor_id")
    private Employee supervisor;

    @Column(name = "vacation_sick_balance", precision = 10, scale = 2)
    private BigDecimal vacationSickBalance;

    @Column(name = "expected_weekly_hours", nullable = false, precision = 3, scale = 1)
    private BigDecimal expectedWeeklyHours;

    public Employee() {
    }

    // Getters / Setters

    public Integer getEmpId() {
        return empId;
    }

    public void setEmpId(final Integer empId) {
        this.empId = empId;
    }

    public String getEmpFirstName() {
        return empFirstName;
    }

    public void setEmpFirstName(final String empFirstName) {
        this.empFirstName = empFirstName;
    }

    public String getEmpLastName() {
        return empLastName;
    }

    public void setEmpLastName(final String empLastName) {
        this.empLastName = empLastName;
    }

    public String getEmpPassword() {
        return empPassword;
    }

    public void setEmpPassword(final String empPassword) {
        this.empPassword = empPassword;
    }

    public SystemRole getSystemRole() {
        return systemRole;
    }

    public void setSystemRole(final SystemRole systemRole) {
        this.systemRole = systemRole;
    }

    public EmployeeESignature getESignature() {
        return eSignature;
    }

    public void setESignature(final EmployeeESignature eSignature) {
        this.eSignature = eSignature;
    }

    public LaborGrade getLaborGrade() {
        return laborGrade;
    }

    public void setLaborGrade(final LaborGrade laborGrade) {
        this.laborGrade = laborGrade;
    }

    public Employee getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(final Employee supervisor) {
        this.supervisor = supervisor;
    }

    public BigDecimal getVacationSickBalance() {
        return vacationSickBalance;
    }

    public void setVacationSickBalance(final BigDecimal vacationSickBalance) {
        this.vacationSickBalance = vacationSickBalance;
    }

    public BigDecimal getExpectedWeeklyHours() {
        return expectedWeeklyHours;
    }

    public void setExpectedWeeklyHours(final BigDecimal expectedWeeklyHours) {
        this.expectedWeeklyHours = expectedWeeklyHours;
    }


}
