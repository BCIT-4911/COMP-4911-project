package ca.bcit.infosys.project;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import ca.bcit.infosys.employee.Employee;
import ca.bcit.infosys.workpackage.WorkPackage;
import ca.bcit.infosys.workpackage.WorkPackageValidation;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/**
 * REST controller for projects.
 * Handles CRUD, employee assignments, work packages, and status changes.
 */
@Stateless
@Path("/projects")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ProjectController {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    /**
     * Looks up a project by ID, throws 404 if not found.
     */
    private Project findProject(String id) {
        Project project = em.find(Project.class, id);
        if (project == null) {
            throw new NotFoundException("Project with id " + id + " not found.");
        }
        return project;
    }

    /**
     * Looks up an employee by ID, throws 404 if not found.
     */
    private Employee findEmployee(int id) {
        Employee employee = em.find(Employee.class, id);
        if (employee == null) {
            throw new NotFoundException("Employee with id " + id + " not found.");
        }
        return employee;
    }

    /**
     * Gets a single project by ID.
     */
    @GET
    @Path("/{id}")
    public Project getProject(@PathParam("id") String id) {
        return findProject(id);
    }

    /**
     * Creates a new project.
     * Validates all required fields before persisting.
     */
    @POST
    @Transactional
    public void createProject(Project project) {
        ProjectValidation.validateId(project.getProject_id());
        ProjectValidation.validateName(project.getProject_name());
        ProjectValidation.validateDescription(project.getProject_desc());
        ProjectValidation.validateStartDate(project.getStart_date());
        ProjectValidation.validateEndDate(project.getEnd_date(), project.getStart_date());
        ProjectValidation.validateMarkup(project.getMarkup_rate());
        ProjectValidation.validateProjectManagerId(project.getProject_manager_id());

        project.setCreated_date(LocalDateTime.now());
        project.setModified_date(LocalDateTime.now());
        em.persist(project);
    }

    /**
     * Updates an existing project.
     * Validates updatable fields before applying changes.
     */
    @PUT
    @Path("/{id}")
    @Transactional
    public void updateProject(@PathParam("id") String id, Project project) {
        Project existing = findProject(id);

        ProjectValidation.validateName(project.getProject_name());
        ProjectValidation.validateDescription(project.getProject_desc());
        ProjectValidation.validateDates(project.getStart_date(), project.getEnd_date());
        ProjectValidation.validateMarkup(project.getMarkup_rate());
        ProjectValidation.validateProjectManagerId(project.getProject_manager_id());

        existing.setProject_name(project.getProject_name());
        existing.setProject_desc(project.getProject_desc());
        existing.setStart_date(project.getStart_date());
        existing.setEnd_date(project.getEnd_date());
        existing.setMarkup_rate(project.getMarkup_rate());
        existing.setProject_manager_id(project.getProject_manager_id());
        existing.setModified_date(LocalDateTime.now());
        em.merge(existing);
    }

    /**
     * Closes (archives) a project.
     */
    @PUT
    @Path("/{id}/close")
    @Transactional
    public void closeProject(@PathParam("id") String id) {
        Project project = findProject(id);
        project.setProject_status(ProjectStatus.ARCHIVED);
        em.merge(project);
    }

    /**
     * Opens a project.
     */
    @PUT
    @Path("/{id}/open")
    @Transactional
    public void openProject(@PathParam("id") String id) {
        Project project = findProject(id);
        project.setProject_status(ProjectStatus.OPEN);
        em.merge(project);
    }

    /**
     * Adds a new work package to a project.
     */
    @POST
    @Path("/{id}/workpackages")
    @Transactional
    public void addWorkPackage(@PathParam("id") String id, WorkPackage wp) {
        findProject(id);
        WorkPackageValidation.validate(wp);
        wp.setProjId(id);
        em.persist(wp);
    }

    /**
     * Assigns an employee to a project.
     * Skips if the assignment already exists.
     */
    @POST
    @Path("/{id}/employees/{empId}")
    @Transactional
    public void assignEmployee(@PathParam("id") String id, @PathParam("empId") int empId) {
        Project project = findProject(id);
        Employee employee = findEmployee(empId);

        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(pa) FROM ProjectAssignment pa WHERE pa.project = :project AND pa.employee = :employee",
                Long.class);
        query.setParameter("project", project);
        query.setParameter("employee", employee);
        if (query.getSingleResult() > 0) {
            return;
        }

        ProjectAssignment assignment = new ProjectAssignment();
        Integer maxId = em.createQuery("SELECT MAX(pa.paId) FROM ProjectAssignment pa", Integer.class)
                .getSingleResult();
        assignment.setPaId(maxId == null ? 1 : maxId + 1);
        assignment.setProject(project);
        assignment.setEmployee(employee);
        assignment.setAssignmentDate(LocalDate.now());
        em.persist(assignment);
    }

    /**
     * Gets all work packages for a project.
     * Uses a direct JPQL query to avoid proxy issues.
     */
    @GET
    @Path("/{id}/workpackages")
    public List<WorkPackage> getWorkPackages(@PathParam("id") String id) {
        findProject(id);
        return em.createQuery(
                "SELECT w FROM WorkPackage w WHERE w.projId = :projId", WorkPackage.class)
                .setParameter("projId", id)
                .getResultList();
    }

    /**
     * Gets all employees assigned to a project.
     * Uses a direct JPQL join to avoid proxy issues.
     */
    @GET
    @Path("/{id}/employees")
    public List<Employee> getAssignedEmployees(@PathParam("id") String id) {
        findProject(id);
        return em.createQuery(
                "SELECT e FROM Employee e JOIN ProjectAssignment pa ON e.empId = pa.empId WHERE pa.projId = :projId",
                Employee.class)
                .setParameter("projId", id)
                .getResultList();
    }

    /**
     * Generates a plain text report for a project.
     */
    @GET
    @Path("/{id}/report")
    @Produces(MediaType.TEXT_PLAIN)
    public String generateReport(@PathParam("id") String id) {
        Project p = findProject(id);
        return "Project Report---------------------\n"
                + "Project ID: " + p.getProject_id() + "\n"
                + "Project Manager: " + p.getProject_manager_id() + "\n"
                + "Type: " + p.getProject_type() + "\n"
                + "Name: " + p.getProject_name() + "\n"
                + "Description: " + p.getProject_desc() + "\n"
                + "Status: " + p.getProject_status() + "\n"
                + "Start Date: " + p.getStart_date() + "\n"
                + "End Date: " + p.getEnd_date() + "\n"
                + "Markup: " + p.getMarkup_rate() + "\n";
    }
}
