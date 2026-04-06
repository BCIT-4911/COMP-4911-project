package com.corejsf.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.corejsf.Entity.Employee;
import com.corejsf.Entity.Project;
import com.corejsf.Entity.ProjectAssignment;
import com.corejsf.Entity.ProjectRole;
import com.corejsf.Entity.ProjectStatus;
import com.corejsf.Entity.WorkPackage;
import com.corejsf.Entity.WorkPackageStatus;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.NotFoundException;

@Stateless
public class ProjectService {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    @EJB
    private WorkPackageService workPackageService;

    private Project findProject(String id) {
        Project project = em.find(Project.class, id);
        if (project == null) {
            throw new NotFoundException("Project with id " + id + " not found.");
        }
        return project;
    }

    private Employee findEmployee(int id) {
        Employee employee = em.find(Employee.class, id);
        if (employee == null) {
            throw new NotFoundException("Employee with id " + id + " not found.");
        }
        return employee;
    }

    public List<Project> getAllProjects() {
        return em.createQuery("SELECT p FROM Project p", Project.class)
                .getResultList();
    }

    public List<Project> getProjectsForEmployee(int empId) {
        return em.createQuery(
                "SELECT DISTINCT p FROM Project p " +
                "WHERE (p.projectManager != null AND p.projectManager.empId = :empId) OR " +
                "p.projId IN (SELECT pa.project.projId FROM ProjectAssignment pa WHERE pa.employee.empId = :empId)", 
                Project.class)
                .setParameter("empId", empId)
                .getResultList();
    }

    public Project getProject(String id) {
        return findProject(id);
    }

    public void createProject(Project project) {
        ProjectValidation.validateId(project.getProjId());
        ProjectValidation.validateName(project.getProjName());
        ProjectValidation.validateDescription(project.getDescription());
        ProjectValidation.validateStartDate(project.getStartDate());
        ProjectValidation.validateEndDate(project.getEndDate(), project.getStartDate());
        ProjectValidation.validateMarkup(project.getMarkupRate());
        ProjectValidation.validateProjectManagerId(project.getProjectManagerId());
        ProjectValidation.validateBac(project.getBac());

        Employee pm = findEmployee(project.getProjectManagerId());
        project.setProjectManager(pm);
        project.setCreatedDate(LocalDateTime.now());
        project.setModifiedDate(LocalDateTime.now());
        em.persist(project);

        assignEmployee(project.getProjId(), pm.getEmpId(), ProjectRole.PM);
    }


    public void updateProject(String id, Project project) {
        Project existing = findProject(id);

        ProjectValidation.validateName(project.getProjName());
        ProjectValidation.validateDescription(project.getDescription());
        ProjectValidation.validateDates(project.getStartDate(), project.getEndDate());
        ProjectValidation.validateMarkup(project.getMarkupRate());
        ProjectValidation.validateProjectManagerId(project.getProjectManagerId());
        ProjectValidation.validateBac(project.getBac());

        Employee newPm = findEmployee(project.getProjectManagerId());

        existing.setProjName(project.getProjName());
        existing.setDescription(project.getDescription());
        existing.setStartDate(project.getStartDate());
        existing.setEndDate(project.getEndDate());
        existing.setMarkupRate(project.getMarkupRate());
        existing.setProjectManager(newPm);
        existing.setModifiedDate(LocalDateTime.now());
        em.merge(existing);

        em.createQuery(
                "DELETE FROM ProjectAssignment pa " +
                "WHERE pa.project.projId = :projId AND pa.projectRole = :role")
                .setParameter("projId", existing.getProjId())
                .setParameter("role", ProjectRole.PM)
                .executeUpdate();

        assignEmployee(existing.getProjId(), newPm.getEmpId(), ProjectRole.PM);
    }



    public void deleteProject(String id) {
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

    public void closeProject(String id) {
        Project project = findProject(id);
        project.setStatus(ProjectStatus.ARCHIVED);
        em.merge(project);

        em.createQuery(
                "UPDATE WorkPackage w SET w.status = :closed "
                        + "WHERE w.project.projId = :projId AND w.status = :open")
                .setParameter("closed", WorkPackageStatus.CLOSED_FOR_CHARGES)
                .setParameter("open", WorkPackageStatus.OPEN_FOR_CHARGES)
                .setParameter("projId", id)
                .executeUpdate();
    }

    public void openProject(String id) {
        Project project = findProject(id);
        project.setStatus(ProjectStatus.OPEN);
        em.merge(project);
    }

    public WorkPackage addWorkPackage(String projId, WorkPackage wp) {
        findProject(projId);
        wp.setProjId(projId);
        return workPackageService.createWorkPackage(wp);
    }

    /**
     * Assigns an employee to a project. Skips if the assignment already exists.
     * Uses AUTO_INCREMENT for ID generation instead of manual MAX query.
     */
    public void assignEmployee(String projId, int empId, ProjectRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Project role is required.");
        }

        Project project = findProject(projId);
        Employee employee = findEmployee(empId);

        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(pa) FROM ProjectAssignment pa " +
                "WHERE pa.project = :project AND pa.employee = :employee AND pa.projectRole = :role",
                Long.class);
        query.setParameter("project", project);
        query.setParameter("employee", employee);
        query.setParameter("role", role);

        if (query.getSingleResult() == 0) {
            ProjectAssignment assignment = new ProjectAssignment();
            assignment.setProject(project);
            assignment.setEmployee(employee);
            assignment.setAssignmentDate(LocalDate.now());
            assignment.setProjectRole(role);
            em.persist(assignment);
        }

        if (role == ProjectRole.PM) {
            project.setProjectManager(employee);
            project.setModifiedDate(LocalDateTime.now());
            em.merge(project);
        }
    }


    public List<WorkPackage> getWorkPackages(String projId, int empId, boolean isOpsOrPm) {
        findProject(projId);
        
        if (isOpsOrPm) {
            // Ops Managers and the Project Manager get to see the whole tree
            return em.createQuery(
                    "SELECT w FROM WorkPackage w LEFT JOIN FETCH w.project LEFT JOIN FETCH w.responsibleEmployee LEFT JOIN FETCH w.parentWorkPackage WHERE w.project.projId = :projId", WorkPackage.class)
                    .setParameter("projId", projId)
                    .getResultList();
        } else {
            // Normal employees only see the WPs they are actively assigned to
            return em.createQuery(
                    "SELECT DISTINCT w FROM WorkPackage w " +
                    "LEFT JOIN FETCH w.project LEFT JOIN FETCH w.responsibleEmployee " +
                    "LEFT JOIN FETCH w.parentWorkPackage " +
                    "JOIN WorkPackageAssignment wpa ON w.wpId = wpa.workPackage.wpId " +
                    "WHERE w.project.projId = :projId AND wpa.employee.empId = :empId", WorkPackage.class)
                    .setParameter("projId", projId)
                    .setParameter("empId", empId)
                    .getResultList();
        }
    }

    public List<Employee> getAssignedEmployees(String projId) {
        findProject(projId);
        
        // Fetch the full assignments instead of just the employees
        List<ProjectAssignment> assignments = em.createQuery(
                "SELECT pa FROM ProjectAssignment pa WHERE pa.project.projId = :projId",
                ProjectAssignment.class)
                .setParameter("projId", projId)
                .getResultList();

        // Map to clean objects and inject the specific project role!
        List<Employee> cleanEmployees = new java.util.ArrayList<>();
        for (ProjectAssignment pa : assignments) {
            Employee p = pa.getEmployee();
            Employee clean = new Employee();
            clean.setEmpId(p.getEmpId());
            clean.setEmpFirstName(p.getEmpFirstName());
            clean.setEmpLastName(p.getEmpLastName());
            clean.setProjectRole(pa.getProjectRole().name()); // <-- Grabbing the role!
            cleanEmployees.add(clean);
        }
        return cleanEmployees;
    }

    public void removeEmployee(String projId, int empId) {
        jakarta.persistence.TypedQuery<ProjectAssignment> query = em.createQuery(
                "SELECT pa FROM ProjectAssignment pa WHERE pa.project.projId = :projId AND pa.employee.empId = :empId",
                ProjectAssignment.class);
        query.setParameter("projId", projId);
        query.setParameter("empId", empId);
        try {
            ProjectAssignment assignment = query.getSingleResult();
            em.remove(assignment);
        } catch (jakarta.persistence.NoResultException e) {
            // Nothing to remove
        }
    }

    public String generateReport(String id) {
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
