package com.corejsf.entity;

import jakarta.persistence.*;

/*
 * This is just a bare-minimum stub for the Employee class so our Timesheet can actually compile.
 */
@Entity
@Table(name = "Employee")
public class Employee {

    @Id
    @Column(name = "emp_id", nullable = false)
    private Integer empId;

    public Employee() {
    }

    public Integer getEmpId() {
        return empId;
    }

    public void setEmpId(final Integer empId) {
        this.empId = empId;
    }
}