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
    * Role checks
    */
    public boolean isOperationsManager(Employee employee) {
        return employee.getSystemRole() == SystemRole.OPERATIONS_MANAGER;
    }

    public boolean isHr(Employee employee) {
        return employee.getSystemRole() == SystemRole.HR;
    }


    /* Project Rules */
    public boolean canCreateProject(Employee employee) {
        return isOperationsManager(employee);
    }

    /* Employee Management Rules    */
    public boolean canManageEmployees(Employee employee) {
        return isHr(employee);
    }

    /* Timesheet Rules  */
    public boolean canEditTimesheet(Employee employee, Integer timesheetId) {

        Timesheet ts = em.find(Timesheet.class, timesheetId);

        if (ts == null) {
            return false;
        }

        return ts.getEmployee().getEmpId().equals(employee.getEmpId());
    }

    public boolean canApproveTimesheet(Employee employee, Integer timesheetId) {

        Timesheet ts = em.find(Timesheet.class, timesheetId);

        if (ts == null || ts.getApprover() == null) {
            return false;
        }

        return ts.getApprover().getEmpId().equals(employee.getEmpId());
    }

    /* Work Package Rules  */
    public boolean canEditWorkPackage(Employee employee, String wpId) {

        WorkPackage wp = em.find(WorkPackage.class, wpId);

        if (wp == null) {
            return false;
        }

        // Responsible Engineer
        if (wp.getResponsibleEmployee() != null &&
                wp.getResponsibleEmployee().getEmpId().equals(employee.getEmpId())) {
            return true;
        }

        // Project Manager
        if (wp.getProject() != null &&
                wp.getProject().getProjectManager() != null &&
                wp.getProject().getProjectManager().getEmpId().equals(employee.getEmpId())) {
            return true;
        }

        return false;
    }

}
