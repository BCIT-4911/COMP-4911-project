package com.corejsf.api;

import com.corejsf.DTO.employee.EmployeeManagerUpdateDto;
import com.corejsf.DTO.employee.EmployeeSelfUpdateDto;
import jakarta.ejb.EJBException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.List;

import com.corejsf.DTO.employee.EmployeeCreateDTO;
import com.corejsf.Entity.Employee;
import com.corejsf.Service.EmployeeService;
import com.corejsf.Service.RebacService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/employees")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EmployeeResource {

    @Inject
    private EmployeeService employeeService;

    @Inject
    private RebacService rebacService;

    @Inject
    private AuthContext authContext;

    private Response forbidden() {
        return Response.status(Response.Status.FORBIDDEN).entity("Access denied").build();
    }

    private static Throwable unwrap(Exception ex) {
        return (ex instanceof EJBException && ex.getCause() != null)
                ? ex.getCause() : ex;
    }

    @GET
    public Response getAll() {
        if (!rebacService.canManageEmployees(authContext.getSystemRole())) {
            return forbidden();
        }
        List<Employee> list = employeeService.getAllEmployees();
        return Response.ok(list).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") int id) {
        int authEmpId = authContext.getEmpId();
        if (id != authEmpId && !rebacService.canManageEmployees(authContext.getSystemRole())) {
            return forbidden();
        }
        Employee emp = employeeService.getEmployee(id);
        return Response.ok(emp).build();
    }

    @POST
    public Response create(EmployeeCreateDTO dto) {
        if (!rebacService.canWriteEmployees(authContext.getSystemRole())) {
            return forbidden();
        }

        Employee created;

        try {
            created = employeeService.createEmployee(dto);
        } catch (final Exception ex) {
            Throwable cause = unwrap(ex);
            if (cause instanceof BadRequestException) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Bad Request: " + cause.getMessage()).build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error creating employee: "
                            + cause.getClass().getName() + " - " + cause.getMessage()).build();
        }

        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") final int id,
                           final EmployeeManagerUpdateDto dto) {
        if (!rebacService.canWriteEmployees(authContext.getSystemRole())) {
            return forbidden();
        }

        Employee updated;

        try {
            updated = employeeService.updateEmployee(id, dto);
        } catch (final Exception ex) {
            Throwable cause = unwrap(ex);
            if (cause instanceof NotFoundException) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(cause.getMessage()).build();
            }
            if (cause instanceof BadRequestException) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(cause.getMessage()).build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error updating employee: "
                            + cause.getClass().getName() + " - " + cause.getMessage()).build();
        }

        if (updated == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Unknown error when updating employee. "
                            + "Please contact system administrator.").build();
        }
        return Response.ok().entity(updated).build();
    }

    @POST
    @Path("/employee-self-update-password")
    public Response selfUpdatePassword(final EmployeeSelfUpdateDto dto) {
        final int empId = authContext.getEmpId();
        return employeeService.employeeSelfUpdatePassword(empId, dto);
    }

    @DELETE
    @Path("/{id}")
    public Response deleteEmployee(@PathParam("id") final int id) {
        if (!rebacService.canWriteEmployees(authContext.getSystemRole())) {
            return forbidden();
        }
        return employeeService.deleteEmployee(id);
    }
}
