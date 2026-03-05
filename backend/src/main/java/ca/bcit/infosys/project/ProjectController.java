package ca.bcit.infosys.project;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.corejsf.Entity.Employee;
import com.corejsf.Entity.Project;
import com.corejsf.Entity.ProjectAssignment;
import com.corejsf.Entity.ProjectStatus;
import com.corejsf.Entity.WorkPackage;

import ca.bcit.infosys.workpackage.WorkPackageValidation;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
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
     * Finds a project by ID.
     * 
     * @param id The ID of the project to find.
     * @return The project with the specified ID.
     * @throws NotFoundException if the project with the specified ID is not found.
     */
    private Project findProject(String id) {
        Project project = em.find(Project.class, id);
        if (project == null) {
            throw new NotFoundException("Project with id " + id + " not found.");
        }
        return project;
    }

    /**
     * Finds an employee by ID.
     * 
     * @param id The ID of the employee to find.
     * @return The employee with the specified ID.
     * @throws NotFoundException if the employee with the specified ID is not found.
     */
    private Employee findEmployee(int id) {
        Employee employee = em.find(Employee.class, id);
        if (employee == null) {
            throw new NotFoundException("Employee with id " + id + " not found.");
        }
        return employee;
    }

    /**
     * Gets all projects.
     * 
     * @return A list of all projects.
     */
    @GET
    public List<Project> getAllProjects() {
        return em.createQuery("SELECT p FROM Project p", Project.class).getResultList();
    }

    /**
     * Gets a single project by ID.
     * 
     * @param id The ID of the project to get.
     * @return The project with the specified ID.
     * @throws NotFoundException if the project with the specified ID is not found.
     */
    @GET
    @Path("/{id}")
    public Project getProject(@PathParam("id") String id) {
        return findProject(id);
    }

    /**
     * Creates a new project.
     * Validates all required fields before persisting.
     * 
     * @param project The project to create.
     * @return The created project.
     * @throws IllegalArgumentException if the project is invalid.
     */
    @POST
    @Transactional
    public Response createProject(Project project) {
        try {
            ProjectValidation.validateId(project.getProjId());
            ProjectValidation.validateName(project.getProjName());
            ProjectValidation.validateDescription(project.getDescription());
            ProjectValidation.validateStartDate(project.getStartDate());
            ProjectValidation.validateEndDate(project.getEndDate(), project.getStartDate());
            ProjectValidation.validateMarkup(project.getMarkupRate());
            ProjectValidation.validateProjectManagerId(project.getProjectManagerId());

            project.setProjectManager(findEmployee(project.getProjectManagerId()));
            project.setCreatedDate(LocalDateTime.now());
            project.setModifiedDate(LocalDateTime.now());
            em.persist(project);

            return Response.status(Response.Status.CREATED).entity(project).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    /**
     * Updates an existing project.
     * Validates updatable fields before applying changes.
     * 
     * @param id      The ID of the project to update.
     * @param project The project to update.
     * @return The updated project.
     * @throws IllegalArgumentException if the project is invalid.
     */
    @PUT
    @Path("/{id}")
    @Transactional
    public Response updateProject(@PathParam("id") String id, Project project) {
        try {
            Project existing = findProject(id);

            ProjectValidation.validateName(project.getProjName());
            ProjectValidation.validateDescription(project.getDescription());
            ProjectValidation.validateDates(project.getStartDate(), project.getEndDate());
            ProjectValidation.validateMarkup(project.getMarkupRate());
            ProjectValidation.validateProjectManagerId(project.getProjectManagerId());

            existing.setProjName(project.getProjName());
            existing.setDescription(project.getDescription());
            existing.setStartDate(project.getStartDate());
            existing.setEndDate(project.getEndDate());
            existing.setMarkupRate(project.getMarkupRate());
            existing.setProjectManager(findEmployee(project.getProjectManagerId()));
            existing.setModifiedDate(LocalDateTime.now());
            em.merge(existing);

            return Response.ok(existing).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    /**
     * Deletes a project and all its associated work packages and assignments.
     * 
     * @param id The ID of the project to delete.
     */
    @DELETE
    @Path("/{id}")
    @Transactional
    public void deleteProject(@PathParam("id") String id) {
        findProject(id);

        List<String> wpIds = em.createQuery(
                "SELECT w.wpId FROM WorkPackage w WHERE w.project.projId = :projId", String.class)
                .setParameter("projId", id)
                .getResultList();
        for (String wpId : wpIds) {
            em.createQuery("DELETE FROM WorkPackageAssignment wpa WHERE wpa.workPackage.wpId = :wpId")
                    .setParameter("wpId", wpId)
                    .executeUpdate();
        }

        em.createQuery("DELETE FROM WorkPackage w WHERE w.project.projId = :projId")
                .setParameter("projId", id)
                .executeUpdate();
        em.createQuery("DELETE FROM ProjectAssignment pa WHERE pa.project.projId = :projId")
                .setParameter("projId", id)
                .executeUpdate();
        Project project = em.find(Project.class, id);
        em.remove(project);
    }

    /**
     * Closes (archives) a project.
     * 
     * @param id The ID of the project to close.
     */
    @PUT
    @Path("/{id}/close")
    @Transactional
    public void closeProject(@PathParam("id") String id) {
        Project project = findProject(id);
        project.setStatus(ProjectStatus.ARCHIVED);
        em.merge(project);
    }

    /**
     * Opens a project.
     * 
     * @param id The ID of the project to open.
     */
    @PUT
    @Path("/{id}/open")
    @Transactional
    public void openProject(@PathParam("id") String id) {
        Project project = findProject(id);
        project.setStatus(ProjectStatus.OPEN);
        em.merge(project);
    }

    /**
     * Adds a new work package to a project.
     * 
     * @param id The ID of the project to add the work package to.
     * @param wp The work package to add.
     * @return The added work package.
     * @throws IllegalArgumentException if the work package is invalid.
     */
    @POST
    @Path("/{id}/workpackages")
    @Transactional
    public Response addWorkPackage(@PathParam("id") String id, WorkPackage wp) {
        try {
            findProject(id);
            WorkPackageValidation.validate(wp);
            wp.setProject(findProject(id));
            em.persist(wp);

            return Response.status(Response.Status.CREATED).entity(wp).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    /**
     * Assigns an employee to a project.
     * Skips if the assignment already exists.
     * 
     * @param id    The ID of the project to assign the employee to.
     * @param empId The ID of the employee to assign.
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
     * 
     * @param id The ID of the project to get the work packages for.
     * @return A list of all work packages for the specified project.
     */
    @GET
    @Path("/{id}/workpackages")
    public List<WorkPackage> getWorkPackages(@PathParam("id") String id) {
        findProject(id);
        return em.createQuery(
                "SELECT w FROM WorkPackage w WHERE w.project.projId = :projId", WorkPackage.class)
                .setParameter("projId", id)
                .getResultList();
    }

    /**
     * Gets all employees assigned to a project.
     * 
     * @param id The ID of the project to get the assigned employees for.
     * @return A list of all employees assigned to the specified project.
     */
    @GET
    @Path("/{id}/employees")
    public List<Employee> getAssignedEmployees(@PathParam("id") String id) {
        findProject(id);
        return em.createQuery(
                "SELECT e FROM Employee e JOIN ProjectAssignment pa ON e = pa.employee WHERE pa.project.projId = :projId",
                Employee.class)
                .setParameter("projId", id)
                .getResultList();
    }

    /**
     * Generates a plain text report for a project.
     * 
     * @param id The ID of the project to generate the report for.
     * @return A plain text report for the specified project.
     */
    @GET
    @Path("/{id}/report")
    @Produces(MediaType.TEXT_PLAIN)
    public String generateReport(@PathParam("id") String id) {
        Project p = findProject(id);
        return "Project Report---------------------\n"
                + "Project ID: " + p.getProjId() + "\n"
                + "Project Manager: " + p.getProjectManager().getEmpId() + "\n"
                + "Type: " + p.getProjType() + "\n"
                + "Name: " + p.getProjName() + "\n"
                + "Description: " + p.getDescription() + "\n"
                + "Status: " + p.getStatus() + "\n"
                + "Start Date: " + p.getStartDate() + "\n"
                + "End Date: " + p.getEndDate() + "\n"
                + "Markup: " + p.getMarkupRate() + "\n";
    }
}
