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

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.NotFoundException;

@Stateless
public class ProjectService {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

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

        project.setProjectManager(findEmployee(project.getProjectManagerId()));
        project.setCreatedDate(LocalDateTime.now());
        project.setModifiedDate(LocalDateTime.now());
        em.persist(project);
        assignEmployee(project.getProjId(), project.getProjectManagerId(), ProjectRole.PM);
    }

    public void updateProject(String id, Project project) {
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
        assignEmployee(id, project.getProjectManagerId(), ProjectRole.PM);
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
    }

    public void openProject(String id) {
        Project project = findProject(id);
        project.setStatus(ProjectStatus.OPEN);
        em.merge(project);
    }

    public void addWorkPackage(String projId, WorkPackage wp) {
        Project project = findProject(projId);
        WorkPackageValidation.validate(wp);
        wp.setProject(project);
        em.persist(wp);
    }

    /**
     * Assigns an employee to a project. Creates assignment if none exists; updates role if assignment exists.
     * When role is PM, also syncs Project.projectManager.
     */
    public void assignEmployee(String projId, int empId, ProjectRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Project role is required.");
        }
        Project project = findProject(projId);
        Employee employee = findEmployee(empId);

        TypedQuery<ProjectAssignment> existingQuery = em.createQuery(
                "SELECT pa FROM ProjectAssignment pa WHERE pa.project = :project AND pa.employee = :employee",
                ProjectAssignment.class);
        existingQuery.setParameter("project", project);
        existingQuery.setParameter("employee", employee);
        List<ProjectAssignment> existing = existingQuery.getResultList();

        if (!existing.isEmpty()) {
            ProjectAssignment pa = existing.get(0);
            if (pa.getProjectRole() != role) {
                pa.setProjectRole(role);
                em.merge(pa);
            }
        } else {
            ProjectAssignment assignment = new ProjectAssignment();
            assignment.setProject(project);
            assignment.setEmployee(employee);
            assignment.setAssignmentDate(LocalDate.now());
            assignment.setProjectRole(role);
            em.persist(assignment);
        }

        if (role == ProjectRole.PM) {
            project.setProjectManager(employee);
            em.merge(project);
        }
    }

    public List<WorkPackage> getWorkPackages(String projId) {
        findProject(projId);
        return em.createQuery(
                "SELECT w FROM WorkPackage w WHERE w.project.projId = :projId", WorkPackage.class)
                .setParameter("projId", projId)
                .getResultList();
    }

    public List<Employee> getAssignedEmployees(String projId) {
        findProject(projId);
        return em.createQuery(
                "SELECT e FROM Employee e JOIN ProjectAssignment pa ON e = pa.employee WHERE pa.project.projId = :projId",
                Employee.class)
                .setParameter("projId", projId)
                .getResultList();
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
