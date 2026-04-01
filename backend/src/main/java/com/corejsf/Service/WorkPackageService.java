package com.corejsf.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.corejsf.Entity.Employee;
import com.corejsf.Entity.Project;
import com.corejsf.Entity.ProjectStatus;
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

    /**
     * JSON-B calls {@link WorkPackage#getProjId()} and {@link WorkPackage#getReEmployeeId()} during
     * response serialization, which touch lazy associations. That happens after this EJB method returns,
     * so without an active persistence context those reads cause LazyInitializationException (500).
     * Touch the associations here while the transaction is still open.
     */
    private void initializeWorkPackageJsonAssociations(WorkPackage wp) {
        wp.getProjId();
        wp.getReEmployeeId();
        wp.getParentWpId();
    }

    private Employee findEmployee(int id) {
        Employee emp = em.find(Employee.class, id);
        if (emp == null) {
            throw new NotFoundException("Employee with id " + id + " not found.");
        }
        return emp;
    }

    private Employee findEmployeeByFullName(String fullName) {
        try {
            return em.createQuery(
                "SELECT e FROM Employee e WHERE LOWER(CONCAT(e.empFirstName, ' ', e.empLastName)) = LOWER(:fullName)",
                Employee.class)
                .setParameter("fullName", fullName.trim())
                .getSingleResult();
        } catch (NoResultException e) {
            throw new NotFoundException("Employee with name '" + fullName + "' not found.");
        }
    }

    public List<WorkPackage> getAllWorkPackages() {
        return em.createQuery(
                "SELECT w FROM WorkPackage w LEFT JOIN FETCH w.responsibleEmployee",
                WorkPackage.class)
                .getResultList();
    }

    public WorkPackage getWorkPackage(String id) {
        WorkPackage wp = findWorkPackage(id);
        initializeWorkPackageJsonAssociations(wp);
        return wp;
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

        String reName = wp.getReEmployeeName();
        Integer reEmpId = wp.getReEmployeeId();
        if (reName != null && !reName.isBlank()) {
            wp.setResponsibleEmployee(findEmployeeByFullName(reName));
        } else if (reEmpId != null) {
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

        validateWpWithParentAndProjectBac(wp);

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

    private void validateWpWithParentAndProjectBac(WorkPackage wp) {
        WorkPackage parent = wp.getParentWorkPackage();
        if (parent != null) {
            validateWpWithParentBac(wp, parent);
        } else {
            validateWpWithProjectBac(wp, wp.getProject());
        }
    }

    private void validateWpWithParentBac(WorkPackage wp, WorkPackage parent) {
        BigDecimal parentBac = parent.getBac();
        validateBacAgainstLimit(
                wp.getBac(),
                parentBac,
                "BAC of child work package (" + wp.getWpId() + ") cannot exceed BAC of parent work package.",
                getChildren(parent.getWpId()),
                "Total BAC of child work packages cannot exceed BAC of parent work package.");
    }

    private void validateWpWithProjectBac(WorkPackage wp, Project project) {
        BigDecimal projectBac = project.getBac();
        List<WorkPackage> existingRootWps = em.createQuery(
                "SELECT w FROM WorkPackage w WHERE w.project.projId = :projId AND w.parentWorkPackage IS NULL",
                WorkPackage.class)
                .setParameter("projId", project.getProjId())
                .getResultList();
        validateBacAgainstLimit(
                wp.getBac(),
                projectBac,
                "BAC of work package (" + wp.getWpId() + ") cannot exceed BAC of project.",
                existingRootWps,
                "Total BAC of root work packages cannot exceed BAC of project.");
    }

    private void validateBacAgainstLimit(
        BigDecimal bacToValidate,
        BigDecimal bacLimit,
        String bacExceededMessage,
        List<WorkPackage> workPackagesToSum,
        String totalExceededMessage
    ) {
        if (bacToValidate.compareTo(bacLimit) > 0) {
            throw new IllegalArgumentException(bacExceededMessage);
        }

        BigDecimal totalBac = bacToValidate;
        for (WorkPackage workPackage : workPackagesToSum) {
            if (workPackage.getBac() != null) {
                totalBac = totalBac.add(workPackage.getBac());
            }
        }

        if (totalBac.compareTo(bacLimit) > 0) {
            throw new IllegalArgumentException(totalExceededMessage);
        }
    }

    /**
     * Validates BAC when it is set for the first time on an existing work package (PUT).
     * Excludes {@code existing} from sibling/root sums so we do not double-count its new BAC.
     */
    private void validateBacOnFirstSetViaUpdate(WorkPackage existing, BigDecimal newBac) {
        WorkPackage parent = existing.getParentWorkPackage();
        if (parent != null) {
            List<WorkPackage> siblings = getChildren(parent.getWpId());
            List<WorkPackage> others = new ArrayList<>();
            for (WorkPackage c : siblings) {
                if (!c.getWpId().equals(existing.getWpId())) {
                    others.add(c);
                }
            }
            validateBacAgainstLimit(
                    newBac,
                    parent.getBac(),
                    "BAC of child work package (" + existing.getWpId() + ") cannot exceed BAC of parent work package.",
                    others,
                    "Total BAC of child work packages cannot exceed BAC of parent work package.");
        } else {
            List<WorkPackage> existingRootWps = em.createQuery(
                    "SELECT w FROM WorkPackage w WHERE w.project.projId = :projId AND w.parentWorkPackage IS NULL",
                    WorkPackage.class)
                    .setParameter("projId", existing.getProject().getProjId())
                    .getResultList();
            List<WorkPackage> otherRoots = new ArrayList<>();
            for (WorkPackage w : existingRootWps) {
                if (!w.getWpId().equals(existing.getWpId())) {
                    otherRoots.add(w);
                }
            }
            validateBacAgainstLimit(
                    newBac,
                    existing.getProject().getBac(),
                    "BAC of work package (" + existing.getWpId() + ") cannot exceed BAC of project.",
                    otherRoots,
                    "Total BAC of root work packages cannot exceed BAC of project.");
        }
    }

    public void updateWorkPackage(String id, WorkPackage wp) {
        WorkPackage existing = findWorkPackage(id);

        WorkPackageValidation.validateName(wp.getWpName());

        String reName = wp.getReEmployeeName();
        Integer reEmpId = wp.getReEmployeeId();
        if (reName != null && !reName.isBlank()) {
            existing.setResponsibleEmployee(findEmployeeByFullName(reName));
        } else if (reEmpId != null) {
            existing.setResponsibleEmployee(findEmployee(reEmpId));
        }

        if (existing.getBac() != null && wp.getBac() != null
                && existing.getBac().compareTo(wp.getBac()) != 0) {
            throw new IllegalArgumentException("BAC cannot be changed after it is set.");
        }
        if (existing.getBac() == null && wp.getBac() != null) {
            validateBacOnFirstSetViaUpdate(existing, wp.getBac());
            existing.setBac(wp.getBac());
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

        TypedQuery<Long> projectAssignQuery = em.createQuery(
                "SELECT COUNT(pa) FROM ProjectAssignment pa WHERE pa.project.projId = :projId AND pa.employee.empId = :empId",
                Long.class);
        projectAssignQuery.setParameter("projId", workPackage.getProject().getProjId());
        projectAssignQuery.setParameter("empId", empId);
        if (projectAssignQuery.getSingleResult() == 0) {
            throw new jakarta.ws.rs.WebApplicationException("Employee must be assigned to the project before being assigned to a work package.", jakarta.ws.rs.core.Response.Status.BAD_REQUEST);
        }

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
        if (wp.getProject() != null && wp.getProject().getStatus() == ProjectStatus.ARCHIVED) {
            throw new jakarta.ws.rs.WebApplicationException(
                    "Cannot reopen a work package while its project is closed.",
                    jakarta.ws.rs.core.Response.Status.BAD_REQUEST);
        }
        wp.setStatus(WorkPackageStatus.OPEN_FOR_CHARGES);
        em.merge(wp);
    }

    public WorkPackage updateEtc(String id, BigDecimal etc) {
        WorkPackageValidation.validateEtc(etc);

        WorkPackage wp = findWorkPackage(id);
        wp.setEtc(etc);
        wp.setModifiedDate(LocalDateTime.now());
        em.merge(wp);
        initializeWorkPackageJsonAssociations(wp);
        return wp;
    }

    public List<WorkPackage> getChildren(String id) {
        findWorkPackage(id);
        return em.createQuery(
                "SELECT w FROM WorkPackage w LEFT JOIN FETCH w.project LEFT JOIN FETCH w.responsibleEmployee WHERE w.parentWorkPackage.wpId = :parentId", WorkPackage.class)
                .setParameter("parentId", id)
                .getResultList();
    }

    public WorkPackage getParent(String id) {
        WorkPackage wp = findWorkPackage(id);
        if (wp.getParentWorkPackage() == null) {
            throw new NotFoundException("Work package " + id + " has no parent.");
        }
        String parentWpId = wp.getParentWorkPackage().getWpId();
        // The persistence context already holds a Hibernate proxy for the parent (loaded when
        // we navigated the lazy association above). em.find would return that same proxy, and
        // JSON-B chokes on its synthetic 'hibernateLazyInitializer' property.
        // Clearing the PC forces a fresh, non-proxied load.
        em.clear();
        WorkPackage parent = findWorkPackage(parentWpId);
        initializeWorkPackageJsonAssociations(parent);
        return parent;
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
