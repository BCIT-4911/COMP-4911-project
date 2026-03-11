package com.corejsf.DTO;

/**
 * Inbound DTO for creating an employee.
 * Team Onboarding implements the full registration flow.
 */
public class EmployeeCreateDTO {

    private String firstName;
    private String lastName;
    private String password;
    private Integer laborGradeId;
    private Integer supervisorId;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getLaborGradeId() {
        return laborGradeId;
    }

    public void setLaborGradeId(Integer laborGradeId) {
        this.laborGradeId = laborGradeId;
    }

    public Integer getSupervisorId() {
        return supervisorId;
    }

    public void setSupervisorId(Integer supervisorId) {
        this.supervisorId = supervisorId;
    }
}
