package com.corejsf.Service;

import com.corejsf.Entity.Employee;
import com.corejsf.Entity.SystemRole;
import com.corejsf.Entity.Timesheet;
import com.corejsf.Entity.WorkPackage;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class RebacService {
    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;


    /*
     * Role checks (SystemRole-based, no DB lookup)
     */
    public boolean canCreateProject(SystemRole role) {
        return role == SystemRole.OPERATIONS_MANAGER;
    }

    public boolean canManageEmployees(SystemRole role) {
        return role == SystemRole.HR;
    }

    /*
     * Role checks (Employee-based, for backward compatibility)
     */
    public boolean isOperationsManager(Employee employee) {
        return employee != null && employee.getSystemRole() == SystemRole.OPERATIONS_MANAGER;
    }

    public boolean isHr(Employee employee) {
        return employee != null && employee.getSystemRole() == SystemRole.HR;
    }

    public boolean canCreateProject(Employee employee) {
        return isOperationsManager(employee);
    }

    public boolean canManageEmployees(Employee employee) {
        return isHr(employee);
    }

    /*
     * Relationship-based checks (empId-based, preferred for Resources)
     */
    public boolean canEditTimesheet(int empId, int timesheetId) {
        Timesheet ts = em.find(Timesheet.class, timesheetId);
        if (ts == null || ts.getEmployee() == null) {
            return false;
        }
        return Integer.valueOf(empId).equals(ts.getEmployee().getEmpId());
    }

    public boolean canApproveTimesheet(int empId, int timesheetId) {
        Timesheet ts = em.find(Timesheet.class, timesheetId);
        if (ts == null || ts.getApprover() == null) {
            return false;
        }
        return Integer.valueOf(empId).equals(ts.getApprover().getEmpId());
    }

    public boolean canEditWorkPackage(int empId, String wpId) {
        WorkPackage wp = em.find(WorkPackage.class, wpId);
        if (wp == null) {
            return false;
        }
        Integer empIdBoxed = Integer.valueOf(empId);
        if (wp.getResponsibleEmployee() != null &&
                empIdBoxed.equals(wp.getResponsibleEmployee().getEmpId())) {
            return true;
        }
        if (wp.getProject() != null &&
                wp.getProject().getProjectManager() != null &&
                empIdBoxed.equals(wp.getProject().getProjectManager().getEmpId())) {
            return true;
        }
        return false;
    }

    /*
     * Relationship-based checks (Employee-based, for backward compatibility)
     */
    public boolean canEditTimesheet(Employee employee, Integer timesheetId) {
        return employee != null && canEditTimesheet(employee.getEmpId(), timesheetId);
    }

    public boolean canApproveTimesheet(Employee employee, Integer timesheetId) {
        return employee != null && canApproveTimesheet(employee.getEmpId(), timesheetId);
    }

    public boolean canEditWorkPackage(Employee employee, String wpId) {
        return employee != null && canEditWorkPackage(employee.getEmpId(), wpId);
    }

}
