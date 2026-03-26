package com.corejsf.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.corejsf.Entity.Employee;
import com.corejsf.Entity.Project;
import com.corejsf.Entity.WorkPackage;
import com.corejsf.Entity.WorkPackageAssignment;
import com.corejsf.Entity.WorkPackageStatus;
import com.corejsf.Entity.WorkPackageType;
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

        WorkPackage existingWp = em.find(WorkPackage.class, wp.getWpId());
        if (existingWp != null) {
            throw new IllegalArgumentException("WorkPackage with id " + wp.getWpId() + " already exists.");
        }

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

        String wpIds[] = wp.getWpId().split("\\.");
        if (wpIds.length > 1) {
            String parentWpId = String.join(".", Arrays.copyOfRange(wpIds, 0, wpIds.length - 1));
            if (parentWpId != null) {
                WorkPackage parent = em.find(WorkPackage.class, parentWpId);
                if (parent == null) {
                    throw new NotFoundException("Parent WorkPackage with id " + parentWpId + " not found.");
                }

                parent.setWpType(WorkPackageType.SUMMARY);
                wp.setParentWorkPackage(parent);
            }
        } else {
            wp.setParentWorkPackage(null); 
        }

        wp.setCreatedDate(LocalDateTime.now());
        wp.setModifiedDate(LocalDateTime.now());
        em.persist(wp);

        List<WorkPackage> wpChildren = getChildren(wp.getWpId());
        if (!wpChildren.isEmpty()) {
            wp.setWpType(WorkPackageType.SUMMARY);
        } else {
            wp.setWpType(WorkPackageType.LOWEST_LEVEL);
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

        Integer reEmpId = wp.getReEmployeeId();
        if (reEmpId != null) {
            existing.setResponsibleEmployee(findEmployee(reEmpId));
        }

        // Map the existing fields
        existing.setWpName(wp.getWpName());
        existing.setDescription(wp.getDescription());
        
        // Map the NEW Estimate Fields!
        existing.setEac(wp.getEac());
        existing.setPercentComplete(wp.getPercentComplete());
        existing.setBudgetedEffort(wp.getBudgetedEffort());

        existing.setModifiedDate(LocalDateTime.now());
        em.merge(existing);
    }

    public void deleteWorkPackage(String id) {
        WorkPackage wp = findWorkPackage(id);

        WorkPackage parentWp = wp.getParentWorkPackage();
        if (parentWp != null) {
            List<WorkPackage> parentWpChildren = getChildren(parentWp.getWpId());
            if (parentWpChildren.size() == 1) {
                parentWp.setWpType(WorkPackageType.LOWEST_LEVEL);
            }
        }

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
     * Assigns an employee to a work package. Skips if already assigned.
     * Uses AUTO_INCREMENT for ID generation instead of manual MAX query.
     */
    public void assignEmployee(String wpId, int empId, WpRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Work package role is required.");
        }
        WorkPackage workPackage = findWorkPackage(wpId);
        Employee employee = findEmployee(empId);

        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(wpa) FROM WorkPackageAssignment wpa WHERE wpa.workPackage.wpId = :wpId AND wpa.employee.empId = :empId",
                Long.class);
        query.setParameter("wpId", wpId);
        query.setParameter("empId", empId);
        if (query.getSingleResult() > 0) {
            return;
        }

        WorkPackageAssignment assignment = new WorkPackageAssignment();
        assignment.setWorkPackage(workPackage);
        assignment.setEmployee(employee);
        assignment.setAssignmentDate(LocalDate.now());
        assignment.setWpRole(role);
        em.persist(assignment);
    }

    public void removeEmployee(String wpId, int empId) {
        TypedQuery<WorkPackageAssignment> query = em.createQuery(
                "SELECT wpa FROM WorkPackageAssignment wpa WHERE wpa.workPackage.wpId = :wpId AND wpa.employee.empId = :empId",
                WorkPackageAssignment.class);
        query.setParameter("wpId", wpId);
        query.setParameter("empId", empId);
        try {
            WorkPackageAssignment assignment = query.getSingleResult();
            em.remove(assignment);
        } catch (NoResultException e) {
            // nothing to remove
        }
    }

    public List<Employee> getAssignedEmployees(String wpId) {
        findWorkPackage(wpId);
        
        List<WorkPackageAssignment> assignments = em.createQuery(
                "SELECT wpa FROM WorkPackageAssignment wpa WHERE wpa.workPackage.wpId = :wpId",
                WorkPackageAssignment.class)
                .setParameter("wpId", wpId)
                .getResultList();

        List<Employee> cleanEmployees = new java.util.ArrayList<>();
        for (WorkPackageAssignment wpa : assignments) {
            Employee p = wpa.getEmployee();
            Employee clean = new Employee();
            clean.setEmpId(p.getEmpId());
            clean.setEmpFirstName(p.getEmpFirstName());
            clean.setEmpLastName(p.getEmpLastName());
            clean.setWpRole(wpa.getWpRole().name()); // <-- Grabbing the WP role!
            cleanEmployees.add(clean);
        }
        return cleanEmployees;
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
