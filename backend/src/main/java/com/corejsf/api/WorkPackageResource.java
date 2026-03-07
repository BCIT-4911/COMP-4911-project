package com.corejsf.api;

import java.util.List;

import com.corejsf.Entity.Employee;
import com.corejsf.Entity.WorkPackage;
import com.corejsf.Entity.WpRole;
import com.corejsf.Service.WorkPackageService;

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

@Path("/workpackages")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class WorkPackageResource {

    @Inject
    private WorkPackageService workPackageService;

    @GET
    public List<WorkPackage> getAll() {
        return workPackageService.getAllWorkPackages();
    }

    @GET
    @Path("/{id}")
    public WorkPackage get(@PathParam("id") String id) {
        return workPackageService.getWorkPackage(id);
    }

    @POST
    public Response create(WorkPackage wp) {
        WorkPackage created = workPackageService.createWorkPackage(wp);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public void update(@PathParam("id") String id, WorkPackage wp) {
        workPackageService.updateWorkPackage(id, wp);
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") String id) {
        workPackageService.deleteWorkPackage(id);
    }

    @POST
    @Path("/{id}/employees/{empId}")
    public void assignEmployee(@PathParam("id") String id, @PathParam("empId") int empId,
            @QueryParam("role") WpRole role) {
        workPackageService.assignEmployee(id, empId, role);
    }

    @DELETE
    @Path("/{id}/employees/{empId}")
    public void removeEmployee(@PathParam("id") String id, @PathParam("empId") int empId) {
        workPackageService.removeEmployee(id, empId);
    }

    @GET
    @Path("/{id}/employees")
    public List<Employee> getAssignedEmployees(@PathParam("id") String id) {
        return workPackageService.getAssignedEmployees(id);
    }

    @PUT
    @Path("/{id}/close")
    public void close(@PathParam("id") String id) {
        workPackageService.close(id);
    }

    @PUT
    @Path("/{id}/open")
    public void open(@PathParam("id") String id) {
        workPackageService.open(id);
    }

    @GET
    @Path("/{id}/children")
    public List<WorkPackage> getChildren(@PathParam("id") String id) {
        return workPackageService.getChildren(id);
    }

    @GET
    @Path("/{id}/parent")
    public WorkPackage getParent(@PathParam("id") String id) {
        return workPackageService.getParent(id);
    }

    @GET
    @Path("/{id}/report")
    @Produces(MediaType.TEXT_PLAIN)
    public String report(@PathParam("id") String id) {
        return workPackageService.generateReport(id);
    }
}
