package com.corejsf.DTO;

/**
 * Inbound DTO for login request body.
 */
public class LoginRequestDTO {

    private Integer empId;
    private String password;

    public Integer getEmpId() {
        return empId;
    }

    public void setEmpId(Integer empId) {
        this.empId = empId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
