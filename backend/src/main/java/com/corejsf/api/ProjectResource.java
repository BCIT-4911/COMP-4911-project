package com.corejsf.api;

import java.util.List;

import com.corejsf.Entity.Employee;
import com.corejsf.Entity.Project;
import com.corejsf.Entity.ProjectRole;
import com.corejsf.Entity.SystemRole;
import com.corejsf.Entity.WorkPackage;
import com.corejsf.Service.ProjectService;
import com.corejsf.Service.RebacService;

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
import jakarta.ws.rs.core.Response;

@Path("/projects")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ProjectResource {

    @Inject
    private ProjectService projectService;

    @Inject
    private RebacService rebacService;

    @Inject
    private AuthContext authContext;

    private Response forbidden() {
        return Response.status(Response.Status.FORBIDDEN).entity("Access denied").build();
    }

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
    public Response create(Project project) {
        if (!rebacService.canCreateProject(authContext.getSystemRole())) {
            return forbidden();
        }
        projectService.createProject(project);
        return Response.status(Response.Status.CREATED).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, Project project) {
        if (!rebacService.canCreateProject(authContext.getSystemRole()) && !rebacService.canManageProject(authContext.getEmpId(), id)) {
            return forbidden();
        }
        projectService.updateProject(id, project);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        if (!rebacService.canCreateProject(authContext.getSystemRole())) {
            return forbidden();
        }
        projectService.deleteProject(id);
        return Response.ok().build();
    }

    @PUT
    @Path("/{id}/close")
    public Response close(@PathParam("id") String id) {
        if (!rebacService.canCreateProject(authContext.getSystemRole()) && !rebacService.canManageProject(authContext.getEmpId(), id)) {
            return forbidden();
        }
        projectService.closeProject(id);
        return Response.ok().build();
    }

    @PUT
    @Path("/{id}/open")
    public Response open(@PathParam("id") String id) {
        if (!rebacService.canCreateProject(authContext.getSystemRole()) && !rebacService.canManageProject(authContext.getEmpId(), id)) {
            return forbidden();
        }
        projectService.openProject(id);
        return Response.ok().build();
    }

    @POST
    @Path("/{id}/workpackages")
    public Response addWorkPackage(@PathParam("id") String id, WorkPackage wp) {
        if (!rebacService.canCreateProject(authContext.getSystemRole()) && !rebacService.canManageProject(authContext.getEmpId(), id)) {
            return forbidden();
        }
        projectService.addWorkPackage(id, wp);
        return Response.status(Response.Status.CREATED).build();
    }

    @POST
    @Path("/{id}/employees/{empId}")
    public Response assignEmployee(@PathParam("id") String id, @PathParam("empId") int empId,
            @QueryParam("role") ProjectRole role) {
        if (role == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("role query parameter is required").build();
        }
        if (!rebacService.canCreateProject(authContext.getSystemRole()) && !rebacService.canManageProject(authContext.getEmpId(), id)) {
            return forbidden();
        }
        projectService.assignEmployee(id, empId, role);
        return Response.ok().build();
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
