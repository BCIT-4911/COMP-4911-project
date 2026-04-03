package com.corejsf.Service;

import java.util.List;

import com.corejsf.Entity.Employee;
import com.corejsf.Entity.Project;
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


    private boolean isAdmin(int empId) {
        Employee emp = em.find(Employee.class, empId);
        return emp != null && emp.getSystemRole() == SystemRole.ADMIN;
    }

    /*
     * Role checks (SystemRole-based, no DB lookup)
     */
    public boolean canCreateProject(SystemRole role) {
        return role == SystemRole.ADMIN || role == SystemRole.OPERATIONS_MANAGER;
    }

    public boolean canManageEmployees(SystemRole role) {
        // Allowed Employees to see the directory so PMs can make assignments
        return role == SystemRole.ADMIN || role == SystemRole.HR || role == SystemRole.OPERATIONS_MANAGER || role == SystemRole.EMPLOYEE;
    }

    /**
     * Create, update, and delete employees (POST/PUT/DELETE). Only HR and ADMIN.
     */
    public boolean canWriteEmployees(SystemRole role) {
        return role == SystemRole.ADMIN || role == SystemRole.HR;
    }

    /**
     * Returns true if the given role is allowed to manage labor grades (CRUD).
     * Only ADMIN and OPERATIONS_MANAGER have access.
     */
    public boolean canManageLaborGrades(SystemRole role) {
        return role == SystemRole.ADMIN || role == SystemRole.OPERATIONS_MANAGER;
    }

    /**
     * Returns true if empId is the project manager of the given project.
     */
    public boolean canManageProject(int empId, String projId) {
        if (isAdmin(empId)) return true;
        Project project = em.find(Project.class, projId);
        if (project == null) {
            return false;
        }
        
        // 1. Check if they are the primary PM on the Project entity
        if (project.getProjectManager() != null && Integer.valueOf(empId).equals(project.getProjectManager().getEmpId())) {
            return true;
        }
        
        // 2. Check if they are assigned as a PM in the ProjectAssignment table
        Long pmCount = em.createQuery(
                "SELECT COUNT(pa) FROM ProjectAssignment pa " +
                "WHERE pa.project.projId = :projId " +
                "AND pa.employee.empId = :empId " +
                "AND pa.projectRole = :role", Long.class)
                .setParameter("projId", projId)
                .setParameter("empId", empId)
                .setParameter("role", ProjectRole.PM)
                .getSingleResult();
                
        return pmCount != null && pmCount > 0;
    }

    /**
     * Returns true if empId is PM of the work package's project.
     */
    public boolean canManageWorkPackage(int empId, String wpId) {
        if (isAdmin(empId)) return true;
        WorkPackage wp = em.find(WorkPackage.class, wpId);
        if (wp == null || wp.getProject() == null) {
            return false;
        }
        return canManageProject(empId, wp.getProject().getProjId());
    }

    // -----------------------------------------------------------------------
    // EV Report access control (EV Security feature)
    // -----------------------------------------------------------------------

    /**
     * Returns true if the caller is allowed to view EV reports for the project
     * that contains the given parent work package (parentWpId).
     *
     * Access rules:
     *    PM assigned to the project:                           ALLOWED
     *   PM NOT assigned to the project:                       DENIED
     *   OPERATIONS_MANAGER (system role):                     ALLOWED
     *   ADMIN (system role):                                  ALLOWED
     *   Regular EMPLOYEE, unrelated RE, unrelated HR:         DENIED
     *
     * The parentWpId is used rather than a project ID because the existing
     * weekly EV endpoint is keyed on a work package. We look up the project
     * from the WP, then delegate to canManageProject for the PM check.
     *
     * @param empId       the authenticated employee ID
     * @param systemRole  the authenticated employee's system role
     * @param parentWpId  the parent work package ID passed to the EV endpoint
     * @return true if access is permitted, false otherwise
     */
    public boolean canViewEVReport(int empId, SystemRole systemRole, String parentWpId) {
        // Operations Manager always has access
        if (systemRole == SystemRole.OPERATIONS_MANAGER) {
            return true;
        }

        // Admin always has access
        if (systemRole == SystemRole.ADMIN) {
            return true;
        }

        // For all other roles, the only path to access is being the
        // PM of the project that owns this work package.
        // canManageWorkPackage internally calls canManageProject, which checks
        // both the Project.projectManager field AND the ProjectAssignment table.
        // EMPLOYEE, HR, and any RE who is not also a PM all fall through
        // to this check and return false because canManageWorkPackage is PM-onl y.
        return canManageWorkPackage(empId, parentWpId);
    }


    /*
     * Role checks (Employee-based, for backward compatibility)
     */
    public boolean isAdmin(Employee employee) {
        return employee != null && employee.getSystemRole() == SystemRole.ADMIN;
    }

    public boolean isOperationsManager(Employee employee) {
        return employee != null && employee.getSystemRole() == SystemRole.OPERATIONS_MANAGER;
    }

    public boolean isHr(Employee employee) {
        return employee != null && employee.getSystemRole() == SystemRole.HR;
    }

    public boolean canCreateProject(Employee employee) {
        return isAdmin(employee) || isOperationsManager(employee);
    }

    public boolean canManageEmployees(Employee employee) {
        return isAdmin(employee) || isHr(employee) || isOperationsManager(employee) || (employee != null && employee.getSystemRole() == SystemRole.EMPLOYEE);
    }

    public boolean canWriteEmployees(Employee employee) {
        return isAdmin(employee) || isHr(employee);
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
        if (isAdmin(empId)) return true;
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
        if (isAdmin(empId)) return true;
        Timesheet ts = em.find(Timesheet.class, timesheetId);
        if (ts == null || ts.getEmployee() == null) {
            return false;
        }
        return Integer.valueOf(empId).equals(ts.getEmployee().getEmpId());
    }

    public boolean canApproveTimesheet(int empId, int timesheetId) {
        if (isAdmin(empId)) return true;
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
        if (isAdmin(empId)) return true;
        WorkPackage wp = em.find(WorkPackage.class, wpId);
        if (wp == null) {
            return false;
        }
        
        // 1. Are they the Project Manager? (This uses our previously fixed PM logic!)
        if (wp.getProject() != null && canManageProject(empId, wp.getProject().getProjId())) {
            return true;
        }

        // 2. Are they the primary RE?
        Integer empIdBoxed = Integer.valueOf(empId);
        if (wp.getResponsibleEmployee() != null && empIdBoxed.equals(wp.getResponsibleEmployee().getEmpId())) {
            return true;
        }
        
        // 3. Are they assigned as an RE in the WorkPackageAssignment table?
        Long reCount = em.createQuery(
                "SELECT COUNT(wpa) FROM WorkPackageAssignment wpa " +
                "WHERE wpa.workPackage.wpId = :wpId " +
                "AND wpa.employee.empId = :empId " +
                "AND wpa.wpRole = :role", Long.class)
                .setParameter("wpId", wpId)
                .setParameter("empId", empId)
                .setParameter("role", WpRole.RE)
                .getSingleResult();
                
        return reCount != null && reCount > 0;
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

    public boolean canViewProject(final int empId, final String projId) {
        Employee employee = em.find(Employee.class, empId);
        if (employee == null) {
            return false;
        }

        if (employee.getSystemRole() == SystemRole.OPERATIONS_MANAGER
                || employee.getSystemRole() == SystemRole.HR) {
            return true;
        }

        return canManageProject(empId, projId);
    }

    public boolean canEditEtc(final int empId, final String wpId) {
        WorkPackage wp = em.find(WorkPackage.class, wpId);
        if (wp == null) {
            return false;
        }

        Employee employee = em.find(Employee.class, empId);
        if (employee == null) {
            return false;
        }

        if (employee.getSystemRole() == SystemRole.OPERATIONS_MANAGER) {
            return true;
        }

        Integer empIdBoxed = Integer.valueOf(empId);

        if (wp.getResponsibleEmployee() != null
                && empIdBoxed.equals(wp.getResponsibleEmployee().getEmpId())) {
            return true;
        }

        // Keep this only if PM is allowed by your policy
        if (wp.getProject() != null && canManageProject(empId, wp.getProject().getProjId())) {
            return true;
        }

        return false;
    }

}
