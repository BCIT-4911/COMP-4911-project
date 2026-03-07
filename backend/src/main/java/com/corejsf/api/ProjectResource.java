package com.corejsf.api;

import java.util.List;

import com.corejsf.Entity.Employee;
import com.corejsf.Entity.Project;
import com.corejsf.Entity.ProjectRole;
import com.corejsf.Entity.WorkPackage;
import com.corejsf.Service.ProjectService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/projects")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ProjectResource {

    @Inject
    private ProjectService projectService;

    @GET
    public List<Project> getAll() {
        return projectService.getAllProjects();
    }

    @GET
    @Path("/{id}")
    public Project get(@PathParam("id") String id) {
        return projectService.getProject(id);
    }

    @POST
    public void create(Project project) {
        projectService.createProject(project);
    }

    @PUT
    @Path("/{id}")
    public void update(@PathParam("id") String id, Project project) {
        projectService.updateProject(id, project);
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") String id) {
        projectService.deleteProject(id);
    }

    @PUT
    @Path("/{id}/close")
    public void close(@PathParam("id") String id) {
        projectService.closeProject(id);
    }

    @PUT
    @Path("/{id}/open")
    public void open(@PathParam("id") String id) {
        projectService.openProject(id);
    }

    @POST
    @Path("/{id}/workpackages")
    public void addWorkPackage(@PathParam("id") String id, WorkPackage wp) {
        projectService.addWorkPackage(id, wp);
    }

    @POST
    @Path("/{id}/employees/{empId}")
    public void assignEmployee(@PathParam("id") String id, @PathParam("empId") int empId,
            @QueryParam("role") ProjectRole role) {
        projectService.assignEmployee(id, empId, role);
    }

    @GET
    @Path("/{id}/workpackages")
    public List<WorkPackage> getWorkPackages(@PathParam("id") String id) {
        return projectService.getWorkPackages(id);
    }

    @GET
    @Path("/{id}/employees")
    public List<Employee> getAssignedEmployees(@PathParam("id") String id) {
        return projectService.getAssignedEmployees(id);
    }

    @GET
    @Path("/{id}/report")
    @Produces(MediaType.TEXT_PLAIN)
    public String report(@PathParam("id") String id) {
        return projectService.generateReport(id);
    }
}
