package ca.bcit.infosys.workpackage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import ca.bcit.infosys.employee.Employee;
import ca.bcit.infosys.project.Project;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST controller for work packages.
 * Handles CRUD, employee assignments, and status changes.
 */
@Stateless
@Path("/workpackages")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class WorkPackageController {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    /**
     * Looks up a work package by ID, throws 404 if not found.
     */
    private WorkPackage findWorkPackage(String id) {
        WorkPackage wp = em.find(WorkPackage.class, id);
        if (wp == null) {
            throw new NotFoundException("WorkPackage with id " + id + " not found.");
        }
        return wp;
    }

    /**
     * Looks up an employee by ID, throws 404 if not found.
     */
    private Employee findEmployee(int id) {
        Employee emp = em.find(Employee.class, id);
        if (emp == null) {
            throw new NotFoundException("Employee with id " + id + " not found.");
        }
        return emp;
    }

    /**
     * Gets a single work package by ID.
     */
    @GET
    @Path("/{id}")
    public WorkPackage getWorkPackage(@PathParam("id") String id) {
        return findWorkPackage(id);
    }

    /**
     * Creates a new work package.
     * Validates the WP and makes sure the project and parent WP exist.
     */
    @POST
    @Transactional
    public Response createWorkPackage(WorkPackage wp) {
        WorkPackageValidation.validate(wp);

        if (em.find(Project.class, wp.getProjId()) == null) {
            throw new NotFoundException("Project with id " + wp.getProjId() + " not found.");
        }

        if (wp.getParentWpId() != null && !wp.getParentWpId().isEmpty()) {
            if (em.find(WorkPackage.class, wp.getParentWpId()) == null) {
                throw new NotFoundException("Parent WorkPackage with id " + wp.getParentWpId() + " not found.");
            }
        }

        wp.setCreatedDate(LocalDateTime.now());
        wp.setModifiedDate(LocalDateTime.now());
        em.persist(wp);
        return Response.status(Response.Status.CREATED).entity(wp).build();
    }

    /**
     * Updates an existing work package.
     * Only updates name, description, and parent.
     */
    @PUT
    @Path("/{id}")
    @Transactional
    public void updateWorkPackage(@PathParam("id") String id, WorkPackage wp) {
        WorkPackage existing = findWorkPackage(id);

        WorkPackageValidation.validateName(wp.getWpName());

        if (wp.getParentWpId() != null && !wp.getParentWpId().isEmpty()) {
            if (em.find(WorkPackage.class, wp.getParentWpId()) == null) {
                throw new NotFoundException("Parent WorkPackage with id " + wp.getParentWpId() + " not found.");
            }
        }

        existing.setWpName(wp.getWpName());
        existing.setDescription(wp.getDescription());
        existing.setParentWpId(wp.getParentWpId());
        existing.setModifiedDate(LocalDateTime.now());
        em.merge(existing);
    }

    /**
     * Deletes a work package and all its children recursively.
     * Also removes any assignments tied to each deleted WP.
     */
    @DELETE
    @Path("/{id}")
    @Transactional
    public void deleteWorkPackage(@PathParam("id") String id) {
        findWorkPackage(id);
        deleteWorkPackageRecursive(id);
    }

    /**
     * Recursive helper that deletes children first, then assignments, then the WP
     * itself.
     */
    private void deleteWorkPackageRecursive(String wpId) {
        List<String> childIds = em.createQuery(
                "SELECT w.wpId FROM WorkPackage w WHERE w.parentWpId = :parentId", String.class)
                .setParameter("parentId", wpId)
                .getResultList();

        for (String childId : childIds) {
            deleteWorkPackageRecursive(childId);
        }

        em.createQuery("DELETE FROM WorkPackageAssignment wpa WHERE wpa.wpId = :wpId")
                .setParameter("wpId", wpId)
                .executeUpdate();

        WorkPackage wp = em.find(WorkPackage.class, wpId);
        if (wp != null) {
            em.remove(wp);
        }
    }

    /**
     * Assigns an employee to a work package.
     * Skips if the assignment already exists.
     */
    @POST
    @Path("/{id}/employees/{empId}")
    @Transactional
    public void assignEmployee(@PathParam("id") String wpId, @PathParam("empId") int empId) {
        findWorkPackage(wpId);
        findEmployee(empId);

        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(wpa) FROM WorkPackageAssignment wpa WHERE wpa.wpId = :wpId AND wpa.empId = :empId",
                Long.class);
        query.setParameter("wpId", wpId);
        query.setParameter("empId", empId);
        if (query.getSingleResult() > 0) {
            return;
        }

        WorkPackageAssignment assignment = new WorkPackageAssignment();
        Integer maxId = em.createQuery("SELECT MAX(wpa.wpaId) FROM WorkPackageAssignment wpa", Integer.class)
                .getSingleResult();
        assignment.setWpaId(maxId == null ? 1 : maxId + 1);
        assignment.setWpId(wpId);
        assignment.setEmpId(empId);
        assignment.setAssignmentDate(LocalDate.now());
        em.persist(assignment);
    }

    /**
     * Removes an employee's assignment from a work package.
     */
    @DELETE
    @Path("/{id}/employees/{empId}")
    @Transactional
    public void removeEmployee(@PathParam("id") String wpId, @PathParam("empId") int empId) {
        TypedQuery<WorkPackageAssignment> query = em.createQuery(
                "SELECT wpa FROM WorkPackageAssignment wpa WHERE wpa.wpId = :wpId AND wpa.empId = :empId",
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

    /**
     * Gets all employees assigned to a work package.
     * Uses a direct JPQL join to avoid proxy issues.
     */
    @GET
    @Path("/{id}/employees")
    public List<Employee> getAssignedEmployees(@PathParam("id") String id) {
        findWorkPackage(id);
        return em.createQuery(
                "SELECT e FROM Employee e JOIN WorkPackageAssignment wpa ON e.empId = wpa.empId WHERE wpa.wpId = :wpId",
                Employee.class)
                .setParameter("wpId", id)
                .getResultList();
    }

    /**
     * Closes a work package for charges.
     */
    @PUT
    @Path("/{id}/close")
    @Transactional
    public void close(@PathParam("id") String id) {
        WorkPackage wp = findWorkPackage(id);
        wp.setStatus(WpStatus.CLOSED_FOR_CHARGES);
        em.merge(wp);
    }

    /**
     * Opens a work package for charges.
     */
    @PUT
    @Path("/{id}/open")
    @Transactional
    public void open(@PathParam("id") String id) {
        WorkPackage wp = findWorkPackage(id);
        wp.setStatus(WpStatus.OPEN_FOR_CHARGES);
        em.merge(wp);
    }

    /**
     * Gets all direct children of a work package.
     * Uses a direct JPQL query to avoid proxy issues.
     */
    @GET
    @Path("/{id}/children")
    public List<WorkPackage> getChildren(@PathParam("id") String id) {
        findWorkPackage(id);
        return em.createQuery(
                "SELECT w FROM WorkPackage w WHERE w.parentWpId = :parentId", WorkPackage.class)
                .setParameter("parentId", id)
                .getResultList();
    }

    /**
     * Gets the parent work package.
     * Returns 404 if the WP has no parent.
     */
    @GET
    @Path("/{id}/parent")
    public WorkPackage getParent(@PathParam("id") String id) {
        WorkPackage wp = findWorkPackage(id);
        if (wp.getParentWpId() == null) {
            throw new NotFoundException("Work package " + id + " has no parent.");
        }
        return findWorkPackage(wp.getParentWpId());
    }

    /**
     * Generates a plain text report for a work package.
     */
    @GET
    @Path("/{id}/report")
    @Produces(MediaType.TEXT_PLAIN)
    public String generateReport(@PathParam("id") String id) {
        WorkPackage wp = findWorkPackage(id);
        return "Work Package Report---------------------\n"
                + "ID: " + wp.getWpId() + "\n"
                + "Name: " + wp.getWpName() + "\n"
                + "Project ID: " + wp.getProjId() + "\n"
                + "Status: " + (wp.getStatus() != null ? wp.getStatus().getDisplayName() : "N/A") + "\n"
                + "Description: " + (wp.getDescription() != null ? wp.getDescription() : "N/A") + "\n";
    }
}
