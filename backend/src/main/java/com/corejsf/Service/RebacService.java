package com.corejsf.Service;

import com.corejsf.Entity.Employee;
import com.corejsf.Entity.ProjectRole;
import com.corejsf.Entity.SystemRole;
import com.corejsf.Entity.Timesheet;
import com.corejsf.Entity.WorkPackage;
import com.corejsf.Entity.WpRole;

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

    /**
     * Returns true if empId has ProjectRole.PM on the given project (via ProjectAssignment).
     */
    public boolean canManageProject(int empId, String projId) {
        Long count = em.createQuery(
                "SELECT COUNT(pa) FROM ProjectAssignment pa " +
                "WHERE pa.employee.empId = :empId " +
                "AND pa.project.projId = :projId " +
                "AND pa.projectRole = :role", Long.class)
                .setParameter("empId", empId)
                .setParameter("projId", projId)
                .setParameter("role", ProjectRole.PM)
                .getSingleResult();
        return count != null && count > 0;
    }

    /**
     * Returns true if empId has ProjectRole.PM on the work package's project (via canManageProject).
     */
    public boolean canManageWorkPackage(int empId, String wpId) {
        WorkPackage wp = em.find(WorkPackage.class, wpId);
        if (wp == null || wp.getProject() == null) {
            return false;
        }
        return canManageProject(empId, wp.getProject().getProjId());
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

    /**
     * Returns true if authEmpId is the supervisor of targetEmpId.
     */
    public boolean isSupervisorOf(int authEmpId, int targetEmpId) {
        Employee target = em.find(Employee.class, targetEmpId);
        if (target == null || target.getSupervisor() == null) {
            return false;
        }
        return Integer.valueOf(authEmpId).equals(target.getSupervisor().getEmpId());
    }

    /**
     * Returns true if empId can view the timesheet (owner or approver).
     */
    public boolean canViewTimesheet(int empId, int timesheetId) {
        Timesheet ts = em.find(Timesheet.class, timesheetId);
        if (ts == null) return false;
        Integer boxed = Integer.valueOf(empId);
        if (ts.getEmployee() != null && boxed.equals(ts.getEmployee().getEmpId())) return true;
        if (ts.getApprover() != null && boxed.equals(ts.getApprover().getEmpId())) return true;
        return false;
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

    /**
     * Returns true if empId has WpRole.RE on the work package (via WorkPackageAssignment)
     * or is PM on the work package's project.
     */
    public boolean canEditWorkPackage(int empId, String wpId) {
        Long count = em.createQuery(
                "SELECT COUNT(wpa) FROM WorkPackageAssignment wpa " +
                "WHERE wpa.employee.empId = :empId " +
                "AND wpa.workPackage.wpId = :wpId " +
                "AND wpa.wpRole = :role", Long.class)
                .setParameter("empId", empId)
                .setParameter("wpId", wpId)
                .setParameter("role", WpRole.RE)
                .getSingleResult();
        if (count != null && count > 0) {
            return true;
        }
        WorkPackage wp = em.find(WorkPackage.class, wpId);
        if (wp == null || wp.getProject() == null) {
            return false;
        }
        return canManageProject(empId, wp.getProject().getProjId());
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
