package com.corejsf.api;

import com.corejsf.DTO.EmployeeManagerUpdateDto;
import com.corejsf.DTO.EmployeeSelfUpdateDto;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import java.util.List;

import com.corejsf.DTO.EmployeeCreateDTO;
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
     * Only accessible to users with employee management permissions.
     * 
     * @param dto the data transfer object containing new employee details.
     * @return a Response containing the created employee with CREATED status.
     */
    @POST
    public Response create(EmployeeCreateDTO dto) {
        if (!rebacService.canManageEmployees(authContext.getSystemRole())) {
            return forbidden();
        }
        Employee created = employeeService.createEmployee(dto);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    /**
     * Updates an existing employee.
     * Only accessible to users with employee management permissions, or the employee himself / herself.
     *
     * @param dto The data transfer object containing new employee details.
     *            Fields with null data / smaller than or equal to 0 will be ignored for update.
     * @return The updated employee object (if updated successfully) and corresponding HttpStatusCode
     */
    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") final int id, final EmployeeManagerUpdateDto dto)
    {
        if (!rebacService.canManageEmployees(authContext.getSystemRole()))
        {
            return forbidden();
        }

        Employee updated = null;

        try
        {
            updated = employeeService.updateEmployee(id, dto);
        }
        catch (NotFoundException ex)
        {
            return Response.status(Response.Status.NOT_FOUND).entity(ex.getMessage()).build();
        }
        catch (BadRequestException ex)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
        }
        catch (InternalServerErrorException ex)
        {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
        catch (Exception ex)
        {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unknown error has occured when " +
                                                                                 "updating the employee: " +
                                                                                 ex.getClass().getName() + " - " +
                                                                                 ex.getMessage()).build();
        }

        if (updated == null)
        {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unknown error has occured when " +
                                                                                 "updating the employee: Please " +
                                                                                 "contact system administrator for a " +
                                                                                 "fix").build();
        }
        else
        {
            return Response.ok().entity(updated).build();
        }
    }

    /**
     * An employee updates his / her own password.
     * Mote: This resource end-point will heavily depend on whether the injected authContext was legitimate
     *
     * @param dto An EmployeeSelfUpdateDto object that contains the new password.
     * @return An HTTP Status Code with content encoded in the acceptable format arranged by Jakarta EE
     *         Initial status codes (Mar 31, 2026) include "Success", "Unauthorized", and "No-Content (204)"
     */
    @POST
    @Path("/employee-self-update-password")
    public Response selfUpdatePassword(final EmployeeSelfUpdateDto dto)
    {
        final int empId = authContext.getEmpId();

        return employeeService.employeeSelfUpdatePassword(empId, dto);
    }

    /**
     * A RESTFul resource endpoint which allows managers to delete an employee after their role is verified.
     * @param id The ID of the employee to be deleted.
     * @return The HTTP status code with a specific message to tell the deletion was success or not.
     */
    @DELETE
    @Path("/{id}")
    public Response deleteEmployee(@PathParam("id") final int id)
    {
        if (!rebacService.canManageEmployees(authContext.getSystemRole()))
        {
            return forbidden();
        }

        return employeeService.deleteEmployee(id);
    }
}
