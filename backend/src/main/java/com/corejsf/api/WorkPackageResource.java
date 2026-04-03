package com.corejsf.api;

import java.util.List;

import com.corejsf.DTO.EtcUpdateDTO;
import com.corejsf.Entity.Employee;
import com.corejsf.Entity.SystemRole;
import com.corejsf.Entity.WorkPackage;
import com.corejsf.Entity.WpRole;
import com.corejsf.Service.RebacService;
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

    @Inject
    private RebacService rebacService;

    @Inject
    private AuthContext authContext;

    private Response forbidden() {
        return Response.status(Response.Status.FORBIDDEN).entity("Access denied").build();
    }

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
        if (wp == null || wp.getProjId() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("projId is required").build();
        }
        if (!rebacService.canCreateProject(authContext.getSystemRole()) && !rebacService.canManageProject(authContext.getEmpId(), wp.getProjId())) {
            return forbidden();
        }
        WorkPackage created = workPackageService.createWorkPackage(wp);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, WorkPackage wp) {
        if (!rebacService.canEditWorkPackage(authContext.getEmpId(), id)) {
            return forbidden();
        }
        workPackageService.updateWorkPackage(id, wp);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        if (!rebacService.canCreateProject(authContext.getSystemRole()) && !rebacService.canManageWorkPackage(authContext.getEmpId(), id)) {
            return forbidden();
        }
        workPackageService.deleteWorkPackage(id);
        return Response.ok().build();
    }

    @POST
    @Path("/{id}/employees/{empId}")
    public Response assignEmployee(@PathParam("id") String id, @PathParam("empId") int empId,
            @QueryParam("role") WpRole role) {
        if (role == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("role query parameter is required").build();
        }
        if (!rebacService.canManageWorkPackage(authContext.getEmpId(), id)) {
            return forbidden();
        }
        workPackageService.assignEmployee(id, empId, role);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}/employees/{empId}")
    public Response removeEmployee(@PathParam("id") String id, @PathParam("empId") int empId) {
        if (!rebacService.canManageWorkPackage(authContext.getEmpId(), id)) {
            return forbidden();
        }
        workPackageService.removeEmployee(id, empId);
        return Response.ok().build();
    }

    @GET
    @Path("/{id}/employees")
    public List<Employee> getAssignedEmployees(@PathParam("id") String id) {
        return workPackageService.getAssignedEmployees(id);
    }

    @PUT
    @Path("/{id}/close")
    public Response close(@PathParam("id") String id) {
        if (!rebacService.canEditWorkPackage(authContext.getEmpId(), id)) {
            return forbidden();
        }
        workPackageService.close(id);
        return Response.ok().build();
    }

    @PUT
    @Path("/{id}/open")
    public Response open(@PathParam("id") String id) {
        if (!rebacService.canEditWorkPackage(authContext.getEmpId(), id)) {
            return forbidden();
        }
        workPackageService.open(id);
        return Response.ok().build();
    }

    @PUT
    @Path("/{id}/etc")
    public Response updateEtc(@PathParam("id") String id, EtcUpdateDTO dto) {
        if (!rebacService.canEditEtc(authContext.getEmpId(), id)) {
            return forbidden();
        }
        if (dto == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("etc is required").build();
        }
        WorkPackage updated = workPackageService.updateEtc(id, dto.getEtc());
        return Response.ok(updated).build();
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

    @GET
    @Path("/chargeable")
    public Response getChargeableForCurrentUser(){
        int authEmpId = authContext.getEmpId();
        List<WorkPackage> chargeableWp = workPackageService.getAllWorkPackages().stream().filter(wp ->
            rebacService.canChargeToWorkPackage(authEmpId, wp.getWpId())).toList();
        return Response.ok(chargeableWp).build();
    }
}
