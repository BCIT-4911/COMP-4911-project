package com.corejsf.api;

import java.util.List;

import com.corejsf.DTO.EmployeeCreateDTO;
import com.corejsf.Entity.Employee;
import com.corejsf.Entity.SystemRole;
import com.corejsf.Service.EmployeeService;
import com.corejsf.Service.RebacService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.container.ContainerRequestContext;

@Path("/employees")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EmployeeResource {

    @Inject
    private EmployeeService employeeService;

    @Inject
    private RebacService rebacService;

    @Context
    private ContainerRequestContext requestContext;

    private int getAuthEmpId() {
        return (Integer) requestContext.getProperty(JwtAuthFilter.AUTHENTICATED_EMP_ID);
    }

    private SystemRole getAuthRole() {
        return (SystemRole) requestContext.getProperty(JwtAuthFilter.AUTHENTICATED_SYSTEM_ROLE);
    }

    private Response forbidden() {
        return Response.status(Response.Status.FORBIDDEN).entity("Access denied").build();
    }

    @GET
    public Response getAll() {
        if (!rebacService.canManageEmployees(getAuthRole())) {
            return forbidden();
        }
        List<Employee> list = employeeService.getAllEmployees();
        return Response.ok(list).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") int id) {
        int authEmpId = getAuthEmpId();
        if (id != authEmpId && !rebacService.canManageEmployees(getAuthRole())) {
            return forbidden();
        }
        Employee emp = employeeService.getEmployee(id);
        return Response.ok(emp).build();
    }

    @POST
    public Response create(EmployeeCreateDTO dto) {
        if (!rebacService.canManageEmployees(getAuthRole())) {
            return forbidden();
        }
        Employee created = employeeService.createEmployee(dto);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }
}
