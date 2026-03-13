package com.corejsf.Service;

import java.util.List;

import com.corejsf.Entity.Employee;
import com.corejsf.Entity.Project;
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

    /**
     * Returns true if empId is the project manager of the given project.
     */
    public boolean canManageProject(int empId, String projId) {
        Project project = em.find(Project.class, projId);
        if (project == null || project.getProjectManager() == null) {
            return false;
        }
        return Integer.valueOf(empId).equals(project.getProjectManager().getEmpId());
    }

    /**
     * Returns true if empId is PM of the work package's project.
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
     * Returns true if the employee supervises at least one direct report.
     */
    public boolean canAccessApproverDashboard(int empId) {
        Long reportCount = em.createQuery(
                        "SELECT COUNT(e) FROM Employee e WHERE e.supervisor.empId = :supervisorEmpId",
                        Long.class)
                .setParameter("supervisorEmpId", empId)
                .getSingleResult();
        return reportCount != null && reportCount > 0;
    }

    /**
     * Returns employee IDs for all direct reports of a supervisor.
     */
    public List<Integer> getDirectReportEmpIds(int supervisorEmpId) {
        return em.createQuery(
                        "SELECT e.empId FROM Employee e WHERE e.supervisor.empId = :supervisorEmpId ORDER BY e.empId",
                        Integer.class)
                .setParameter("supervisorEmpId", supervisorEmpId)
                .getResultList();
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
