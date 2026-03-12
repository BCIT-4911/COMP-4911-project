package com.corejsf.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.corejsf.Entity.Employee;
import com.corejsf.Entity.Project;
import com.corejsf.Entity.WorkPackage;
import com.corejsf.Entity.WorkPackageAssignment;
import com.corejsf.Entity.WorkPackageStatus;
import com.corejsf.Entity.WpRole;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.NotFoundException;

@Stateless
public class WorkPackageService {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    private WorkPackage findWorkPackage(String id) {
        WorkPackage wp = em.find(WorkPackage.class, id);
        if (wp == null) {
            throw new NotFoundException("WorkPackage with id " + id + " not found.");
        }
        return wp;
    }

    private Employee findEmployee(int id) {
        Employee emp = em.find(Employee.class, id);
        if (emp == null) {
            throw new NotFoundException("Employee with id " + id + " not found.");
        }
        return emp;
    }

    public List<WorkPackage> getAllWorkPackages() {
        return em.createQuery("SELECT w FROM WorkPackage w", WorkPackage.class)
                .getResultList();
    }

    public WorkPackage getWorkPackage(String id) {
        return findWorkPackage(id);
    }

    public WorkPackage createWorkPackage(WorkPackage wp) {
        WorkPackageValidation.validate(wp);

        String projId = wp.getProjId();
        if (projId == null) {
            throw new IllegalArgumentException("projId is required.");
        }
        Project project = em.find(Project.class, projId);
        if (project == null) {
            throw new NotFoundException("Project with id " + projId + " not found.");
        }
        wp.setProject(project);

        Integer reEmpId = wp.getReEmployeeId();
        if (reEmpId != null) {
            wp.setResponsibleEmployee(findEmployee(reEmpId));
        }

        String parentWpId = wp.getParentWpId();
        if (parentWpId != null) {
            WorkPackage parent = em.find(WorkPackage.class, parentWpId);
            if (parent == null) {
                throw new NotFoundException("Parent WorkPackage with id " + parentWpId + " not found.");
            }
            wp.setParentWorkPackage(parent);
        }

        wp.setCreatedDate(LocalDateTime.now());
        wp.setModifiedDate(LocalDateTime.now());
        em.persist(wp);
        if (reEmpId != null) {
            assignEmployee(wp.getWpId(), reEmpId, WpRole.RE);
        }
        return wp;
    }

    public void updateWorkPackage(String id, WorkPackage wp) {
        WorkPackage existing = findWorkPackage(id);

        WorkPackageValidation.validateName(wp.getWpName());

        String parentWpId = wp.getParentWpId();
        if (parentWpId != null) {
            WorkPackage parent = em.find(WorkPackage.class, parentWpId);
            if (parent == null) {
                throw new NotFoundException("Parent WorkPackage with id " + parentWpId + " not found.");
            }
            existing.setParentWorkPackage(parent);
        } else {
            existing.setParentWorkPackage(null);
        }

        existing.setWpName(wp.getWpName());
        existing.setDescription(wp.getDescription());
        existing.setModifiedDate(LocalDateTime.now());
        em.merge(existing);
    }

    public void deleteWorkPackage(String id) {
        findWorkPackage(id);
        deleteWorkPackageRecursive(id);
    }

    private void deleteWorkPackageRecursive(String wpId) {
        List<String> childIds = em.createQuery(
                "SELECT w.wpId FROM WorkPackage w WHERE w.parentWorkPackage.wpId = :parentId", String.class)
                .setParameter("parentId", wpId)
                .getResultList();

        for (String childId : childIds) {
            deleteWorkPackageRecursive(childId);
        }

        em.createQuery("DELETE FROM WorkPackageAssignment wpa WHERE wpa.workPackage.wpId = :wpId")
                .setParameter("wpId", wpId)
                .executeUpdate();

        WorkPackage wp = em.find(WorkPackage.class, wpId);
        if (wp != null) {
            em.remove(wp);
        }
    }

    /**
     * Assigns an employee to a work package. Creates assignment if none exists; updates role if assignment exists.
     * When role is RE, also syncs WorkPackage.responsibleEmployee.
     */
    public void assignEmployee(String wpId, int empId, WpRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Work package role is required.");
        }
        WorkPackage workPackage = findWorkPackage(wpId);
        Employee employee = findEmployee(empId);

        TypedQuery<WorkPackageAssignment> existingQuery = em.createQuery(
                "SELECT wpa FROM WorkPackageAssignment wpa WHERE wpa.workPackage.wpId = :wpId AND wpa.employee.empId = :empId",
                WorkPackageAssignment.class);
        existingQuery.setParameter("wpId", wpId);
        existingQuery.setParameter("empId", empId);
        List<WorkPackageAssignment> existing = existingQuery.getResultList();

        if (!existing.isEmpty()) {
            WorkPackageAssignment wpa = existing.get(0);
            if (wpa.getWpRole() != role) {
                wpa.setWpRole(role);
                em.merge(wpa);
            }
        } else {
            WorkPackageAssignment assignment = new WorkPackageAssignment();
            assignment.setWorkPackage(workPackage);
            assignment.setEmployee(employee);
            assignment.setAssignmentDate(LocalDate.now());
            assignment.setWpRole(role);
            em.persist(assignment);
        }

        if (role == WpRole.RE) {
            List<WorkPackageAssignment> existingReAssignments = em.createQuery(
                    "SELECT wpa FROM WorkPackageAssignment wpa WHERE wpa.workPackage.wpId = :wpId AND wpa.wpRole = :reRole AND wpa.employee.empId <> :empId",
                    WorkPackageAssignment.class)
                    .setParameter("wpId", wpId)
                    .setParameter("reRole", WpRole.RE)
                    .setParameter("empId", empId)
                    .getResultList();
            for (WorkPackageAssignment wpa : existingReAssignments) {
                wpa.setWpRole(WpRole.MEMBER);
                em.merge(wpa);
            }
            workPackage.setResponsibleEmployee(employee);
            em.merge(workPackage);
        }
    }

    public void removeEmployee(String wpId, int empId) {
        TypedQuery<WorkPackageAssignment> query = em.createQuery(
                "SELECT wpa FROM WorkPackageAssignment wpa WHERE wpa.workPackage.wpId = :wpId AND wpa.employee.empId = :empId",
                WorkPackageAssignment.class);
        query.setParameter("wpId", wpId);
        query.setParameter("empId", empId);
        try {
            WorkPackageAssignment assignment = query.getSingleResult();
            if (assignment.getWpRole() == WpRole.RE) {
                throw new IllegalArgumentException("Cannot remove RE; reassign another employee as RE first.");
            }
            em.remove(assignment);
        } catch (NoResultException e) {
            // nothing to remove
        }
    }

    public List<Employee> getAssignedEmployees(String wpId) {
        findWorkPackage(wpId);
        return em.createQuery(
                "SELECT e FROM Employee e JOIN WorkPackageAssignment wpa ON e = wpa.employee WHERE wpa.workPackage.wpId = :wpId",
                Employee.class)
                .setParameter("wpId", wpId)
                .getResultList();
    }

    public void close(String id) {
        WorkPackage wp = findWorkPackage(id);
        wp.setStatus(WorkPackageStatus.CLOSED_FOR_CHARGES);
        em.merge(wp);
    }

    public void open(String id) {
        WorkPackage wp = findWorkPackage(id);
        wp.setStatus(WorkPackageStatus.OPEN_FOR_CHARGES);
        em.merge(wp);
    }

    public List<WorkPackage> getChildren(String id) {
        findWorkPackage(id);
        return em.createQuery(
                "SELECT w FROM WorkPackage w WHERE w.parentWorkPackage.wpId = :parentId", WorkPackage.class)
                .setParameter("parentId", id)
                .getResultList();
    }

    public WorkPackage getParent(String id) {
        WorkPackage wp = findWorkPackage(id);
        if (wp.getParentWorkPackage() == null) {
            throw new NotFoundException("Work package " + id + " has no parent.");
        }
        return findWorkPackage(wp.getParentWorkPackage().getWpId());
    }

    public String generateReport(String id) {
        WorkPackage wp = findWorkPackage(id);
        return "Work Package Report---------------------\n"
                + "ID: " + wp.getWpId() + "\n"
                + "Name: " + wp.getWpName() + "\n"
                + "Project ID: " + wp.getProject().getProjId() + "\n"
                + "Status: " + (wp.getStatus() != null ? wp.getStatus().name() : "N/A") + "\n"
                + "Description: " + (wp.getDescription() != null ? wp.getDescription() : "N/A") + "\n";
    }
}
