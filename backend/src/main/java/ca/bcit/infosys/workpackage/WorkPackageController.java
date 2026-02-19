package ca.bcit.infosys.workpackage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.corejsf.Entity.Employee;
import com.corejsf.Entity.Project;
import com.corejsf.Entity.WorkPackage;
import com.corejsf.Entity.WorkPackageAssignment;
import com.corejsf.Entity.WorkPackageStatus;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
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
     * 
     * @param id The ID of the work package to look up.
     * @return The work package with the specified ID.
     * @throws NotFoundException if the work package with the specified ID is not
     *                           found.
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
     * 
     * @param id The ID of the employee to look up.
     * @return The employee with the specified ID.
     * @throws NotFoundException if the employee with the specified ID is not found.
     */
    private Employee findEmployee(int id) {
        Employee emp = em.find(Employee.class, id);
        if (emp == null) {
            throw new NotFoundException("Employee with id " + id + " not found.");
        }
        return emp;
    }

    /**
     * Gets all work packages.
     * 
     * @return A list of all work packages.
     */
    @GET
    public List<WorkPackage> getAllWorkPackages() {
        return em.createQuery("SELECT w FROM WorkPackage w", WorkPackage.class).getResultList();
    }

    /**
     * Gets a single work package by ID.
     * 
     * @param id The ID of the work package to get.
     * @return The work package with the specified ID.
     * @throws NotFoundException if the work package with the specified ID is not
     *                           found.
     */
    @GET
    @Path("/{id}")
    public WorkPackage getWorkPackage(@PathParam("id") String id) {
        return findWorkPackage(id);
    }

    /**
     * Creates a new work package.
     * Validates the WP and makes sure the project and parent WP exist.
     * 
     * @param wp The work package to create.
     * @return The created work package.
     * @throws IllegalArgumentException if the work package is invalid.
     * @throws NotFoundException        if the project or parent work package with
     *                                  the specified ID is not found.
     */
    @POST
    @Transactional
    public Response createWorkPackage(WorkPackage wp) {
        try {
            WorkPackageValidation.validate(wp);

            if (em.find(WorkPackage.class, wp.getWpId()) != null) {
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
            return Response.status(Response.Status.CREATED).entity(wp).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .build();
        }
    }

    /**
     * Updates an existing work package Name, Description, Parent, Responsible
     * Employee, startDate, EndDate, Budget
     * 
     * @param id The ID of the work package to update.
     * @param wp The work package to update.
     * @return The updated work package.
     * @throws IllegalArgumentException if the work package is invalid.
     * @throws NotFoundException        if the work package with the specified ID is
     *                                  not found.
     */
    @PUT
    @Path("/{id}")
    @Transactional
    public Response updateWorkPackage(@PathParam("id") String id, WorkPackage wp) {
        try {
            WorkPackage existing = findWorkPackage(id);
            String parentWpId = wp.getParentWpId();
            Integer responsibleEmployeeId = wp.getReEmployeeId();

            WorkPackageValidation.validateName(wp.getWpName());
            WorkPackageValidation.validateDates(wp.getPlanStartDate(), wp.getPlanEndDate());
            WorkPackageValidation.validateBudget(wp.getBudgetedEffort());

            if (parentWpId != null) {
                WorkPackage parent = em.find(WorkPackage.class, parentWpId);

                if (parent == null) {
                    throw new NotFoundException("Parent WorkPackage with id " + parentWpId + " not found.");
                }

                existing.setParentWorkPackage(parent);
            } else {
                existing.setParentWorkPackage(null);
            }

            if (responsibleEmployeeId != null) {
                existing.setResponsibleEmployee(findEmployee(responsibleEmployeeId));
            }

            existing.setWpName(wp.getWpName());
            existing.setDescription(wp.getDescription());
            existing.setPlanStartDate(wp.getPlanStartDate());
            existing.setPlanEndDate(wp.getPlanEndDate());
            existing.setBudgetedEffort(wp.getBudgetedEffort());

            if (wp.getStatus() != null)
                existing.setStatus(wp.getStatus());
            if (wp.getWpType() != null)
                existing.setWpType(wp.getWpType());

            existing.setModifiedDate(LocalDateTime.now());
            em.merge(existing);

            return Response.ok(existing).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
    }

    /**
     * Deletes a work package and all its children recursively.
     * Also removes any assignments tied to each deleted WP.
     * 
     * @param id The ID of the work package to delete.
     * @throws NotFoundException if the work package with the specified ID is not
     *                           found.
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
     * 
     * @param wpId The ID of the work package to delete.
     */
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
     * Assigns an employee to a work package.
     * Skips if the assignment already exists.
     * 
     * @param wpId  The ID of the work package to assign the employee to.
     * @param empId The ID of the employee to assign to the work package.
     * @throws NotFoundException if the work package or employee with the specified
     *                           ID is not found.
     */
    @POST
    @Path("/{id}/employees/{empId}")
    @Transactional
    public void assignEmployee(@PathParam("id") String wpId, @PathParam("empId") int empId) {
        findWorkPackage(wpId);
        findEmployee(empId);

        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(wpa) FROM WorkPackageAssignment wpa WHERE wpa.workPackage.wpId = :wpId AND wpa.employee.empId = :empId",
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
        assignment.setWorkPackage(findWorkPackage(wpId));
        assignment.setEmployee(findEmployee(empId));
        assignment.setAssignmentDate(LocalDate.now());
        em.persist(assignment);
    }

    /**
     * Removes an employee's assignment from a work package.
     * 
     * @param wpId  The ID of the work package to remove the employee's assignment
     *              from.
     * @param empId The ID of the employee to remove the assignment from the work
     *              package.
     * @throws NotFoundException if the work package or employee with the specified
     *                           ID is not found.
     */
    @DELETE
    @Path("/{id}/employees/{empId}")
    @Transactional
    public void removeEmployee(@PathParam("id") String wpId, @PathParam("empId") int empId) {
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

    /**
     * Gets all employees assigned to a work package.
     * Uses a direct JPQL join to avoid proxy issues.
     * 
     * @param id The ID of the work package to get the assigned employees for.
     * @return A list of all employees assigned to the work package.
     * @throws NotFoundException if the work package with the specified ID is not
     *                           found.
     */
    @GET
    @Path("/{id}/employees")
    public List<Employee> getAssignedEmployees(@PathParam("id") String id) {
        findWorkPackage(id);
        return em.createQuery(
                "SELECT e FROM Employee e JOIN WorkPackageAssignment wpa ON e = wpa.employee WHERE wpa.workPackage.wpId = :wpId",
                Employee.class)
                .setParameter("wpId", id)
                .getResultList();
    }

    /**
     * Closes a work package for charges.
     * 
     * @param id The ID of the work package to close.
     * @throws NotFoundException if the work package with the specified ID is not
     *                           found.
     */
    @PUT
    @Path("/{id}/close")
    @Transactional
    public void close(@PathParam("id") String id) {
        WorkPackage wp = findWorkPackage(id);
        wp.setStatus(WorkPackageStatus.CLOSED_FOR_CHARGES);
        em.merge(wp);
    }

    /**
     * Opens a work package for charges.
     * 
     * @param id The ID of the work package to open.
     * @throws NotFoundException if the work package with the specified ID is not
     *                           found.
     */
    @PUT
    @Path("/{id}/open")
    @Transactional
    public void open(@PathParam("id") String id) {
        WorkPackage wp = findWorkPackage(id);
        wp.setStatus(WorkPackageStatus.OPEN_FOR_CHARGES);
        em.merge(wp);
    }

    /**
     * Gets all direct children of a work package.
     * Uses a direct JPQL query to avoid proxy issues.
     * 
     * @param id The ID of the work package to get the children for.
     * @return A list of all direct children of the work package.
     * @throws NotFoundException if the work package with the specified ID is not
     *                           found.
     */
    @GET
    @Path("/{id}/children")
    public List<WorkPackage> getChildren(@PathParam("id") String id) {
        findWorkPackage(id);
        return em.createQuery(
                "SELECT w FROM WorkPackage w WHERE w.parentWorkPackage.wpId = :parentId", WorkPackage.class)
                .setParameter("parentId", id)
                .getResultList();
    }

    /**
     * Gets the parent work package.
     * Returns 404 if the WP has no parent.
     * 
     * @param id The ID of the work package to get the parent for.
     * @return The parent work package.
     * @throws NotFoundException if the work package with the specified ID is not
     *                           found.
     */
    @GET
    @Path("/{id}/parent")
    public WorkPackage getParent(@PathParam("id") String id) {
        WorkPackage wp = findWorkPackage(id);
        if (wp.getParentWorkPackage() == null) {
            throw new NotFoundException("Work package " + id + " has no parent.");
        }
        return findWorkPackage(wp.getParentWorkPackage().getWpId());
    }

    /**
     * Generates a plain text report for a work package.
     * 
     * @param id The ID of the work package to generate the report for.
     * @return A plain text report for the specified work package.
     * @throws NotFoundException if the work package with the specified ID is not
     *                           found.
     */
    @GET
    @Path("/{id}/report")
    @Produces(MediaType.TEXT_PLAIN)
    public String generateReport(@PathParam("id") String id) {
        WorkPackage wp = findWorkPackage(id);
        return "Work Package Report---------------------\n"
                + "ID: " + wp.getWpId() + "\n"
                + "Name: " + wp.getWpName() + "\n"
                + "Project ID: " + wp.getProject().getProjId() + "\n"
                + "Status: " + (wp.getStatus() != null ? wp.getStatus().name() : "N/A") + "\n"
                + "Description: " + (wp.getDescription() != null ? wp.getDescription() : "N/A") + "\n";
    }
}
