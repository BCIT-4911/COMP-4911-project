package com.corejsf.api;

import com.corejsf.Entity.SystemRole;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class AuthContext {

    private int empId;
    private SystemRole systemRole;

    public int getEmpId() {
        return empId;
    }

    public void setEmpId(int empId) {
        this.empId = empId;
    }

    public SystemRole getSystemRole() {
        return systemRole;
    }

    public void setSystemRole(SystemRole systemRole) {
        this.systemRole = systemRole;
    }
}
