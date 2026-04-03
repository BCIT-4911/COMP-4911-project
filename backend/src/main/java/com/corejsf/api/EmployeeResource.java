package com.corejsf.api;

import java.util.List;

import com.corejsf.DTO.EmployeeCreateDTO;
import com.corejsf.Entity.Employee;
import com.corejsf.Service.EmployeeService;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource for managing employees.
 * Provides endpoints for retrieving and creating employee records.
 */
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

    /**
     * Helper method to return a Forbidden response.
     * @return a Response with FORBIDDEN status.
     */
    private Response forbidden() {
        return Response.status(Response.Status.FORBIDDEN).entity("Access denied").build();
    }

    /**
     * Retrieves all employees.
     * Only accessible to users with employee management permissions.
     * 
     * @return a Response containing the list of all employees.
     */
    @GET
    public Response getAll() {
        if (!rebacService.canManageEmployees(authContext.getSystemRole())) {
            return forbidden();
        }
        List<Employee> list = employeeService.getAllEmployees();
        return Response.ok(list).build();
    }

    /**
     * Retrieves a specific employee by ID.
     * An employee can retrieve their own record, or a manager can retrieve any record.
     * 
     * @param id the unique identifier of the employee to retrieve.
     * @return a Response containing the requested employee.
     */
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

    /**
     * Creates a new employee.
     * Only accessible to ADMIN and HR ({@code canWriteEmployees}).
     * 
     * @param dto the data transfer object containing new employee details.
     * @return a Response containing the created employee with CREATED status.
     */
    @POST
    public Response create(EmployeeCreateDTO dto) {
        if (!rebacService.canWriteEmployees(authContext.getSystemRole())) {
            return forbidden();
        }
        Employee created = employeeService.createEmployee(dto);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    /**
     * Updates an existing employee.
     * Only accessible to ADMIN and HR ({@code canWriteEmployees}).
     * 
     * @param id the unique identifier of the employee to update.
     * @param dto the data transfer object containing updated employee details.
     * @return a Response containing the updated employee with OK status.
     */
    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") int id, EmployeeCreateDTO dto) {
        if (!rebacService.canWriteEmployees(authContext.getSystemRole())) {
            return forbidden();
        }
        Employee updated = employeeService.updateEmployee(id, dto);
        return Response.ok(updated).build();
    }

    /**
     * Deletes an employee by ID.
     * Only accessible to ADMIN and HR ({@code canWriteEmployees}).
     * 
     * @param id the unique identifier of the employee to delete.
     * @return a Response with OK status.
     */
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") int id) {
        if (!rebacService.canWriteEmployees(authContext.getSystemRole())) {
            return forbidden();
        }
        employeeService.deleteEmployee(id);
        return Response.ok().build();
    }
}
