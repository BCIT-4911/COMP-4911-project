package com.corejsf.Service;

import com.corejsf.DTO.EmployeeManagerUpdateDto;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

import com.corejsf.DTO.EmployeeCreateDTO;
import com.corejsf.DTO.EmployeeSelfUpdateDto;

import com.corejsf.Entity.Employee;
import com.corejsf.Entity.EmployeeESignature;
import com.corejsf.Entity.LaborGrade;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

/**
 * Service class for managing employee data. Provides methods for retrieving and
 * creating employee records.
 */
@Stateless
public class EmployeeService {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    /**
     * Retrieves all employees.
     *
     * @return a list of all employees.
     */
    public List<Employee> getAllEmployees() {
        return em.createQuery("SELECT e FROM Employee e ORDER BY e.empId", Employee.class)
                .getResultList();
    }

    /**
     * Retrieves an employee by ID.
     *
     * @param id the ID of the employee to retrieve.
     * @return the employee with the specified ID.
     * @throws NotFoundException if the employee with the specified ID is not
     * found.
     */
    public Employee getEmployee(int id) {
        Employee emp = em.find(Employee.class, id);
        if (emp == null) {
            throw new NotFoundException("Employee with id " + id + " not found.");
        }
        return emp;
    }

    /**
     * Creates a new employee.
     *
     * @param dto the data transfer object containing new employee details.
     * @return the created employee.
     */
    public Employee createEmployee(EmployeeCreateDTO dto) {
        EmployeeESignature sig = new EmployeeESignature();
        Employee newEmp = new Employee();

        sig.setSignatureData(new byte[]{0x00});
        sig.setSignedAt(LocalDateTime.now());
        em.persist(sig);

        newEmp.setEmpFirstName(dto.getFirstName());
        newEmp.setEmpLastName(dto.getLastName());
        newEmp.setEmpPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        newEmp.setLaborGrade(em.find(LaborGrade.class, dto.getLaborGradeId()));
        newEmp.setSupervisor(em.find(Employee.class, dto.getSupervisorId()));
        newEmp.setVacationSickBalance(new BigDecimal(0));
        newEmp.setExpectedWeeklyHours(new BigDecimal(40));
        newEmp.setSystemRole(dto.getSystemRole());
        newEmp.setESignature(sig);
        em.persist(newEmp);
        return newEmp;
    }

    /**
     * This update employee function validates all requested fields before processing them to fulfill ACID rules
     *
     * @param id The employee ID
     * @param dto An EmployeeManagerUpdateDto class which contains the personal details of the employee
     * @return An updated Employee record to the resource end point
     */
    public Employee updateEmployee(final int id, final EmployeeManagerUpdateDto dto)
    {
        Employee emp = em.find(Employee.class, id);

        if (emp == null)
        {
            throw new NotFoundException("The specific employee with ID " + id + " cannot be found.");
        }

        if (dto.firstName() != null && dto.firstName().isBlank())
        {
            throw new BadRequestException("The new first name cannot be blank (filled only with space character).");
        }

        if (dto.lastName() != null && dto.lastName().isBlank())
        {
            throw new BadRequestException("The new last name cannot be blank (filled only with space character).");
        }

        LaborGrade newLaborGrade = em.find(LaborGrade.class, dto.laborGradeId());

        if (dto.laborGradeId() > 0 && newLaborGrade  == null)
        {
            throw new BadRequestException("The new labour grade ID " + dto.laborGradeId() + " is invalid.");
        }

        Employee newSupervisor = em.find(Employee.class, dto.supervisorId());

        if (dto.supervisorId() > 0 && newSupervisor == null)
        {
            throw new BadRequestException("The new supervisor ID " + dto.supervisorId() + " is invalid.");
        }

        EntityTransaction transaction = em.getTransaction();

        try
        {
            transaction.begin();

            if(dto.firstName() != null)
            {
                emp.setEmpFirstName(dto.firstName());
            }

            if(dto.lastName() != null)
            {
                emp.setEmpLastName(dto.lastName());
            }

            if(dto.supervisorId() > 0)
            {
                emp.setSupervisor(newSupervisor);
            }

            if(dto.laborGradeId() > 0)
            {
                emp.setLaborGrade(newLaborGrade);
            }

            if(dto.systemRole() != null)
            {
                emp.setSystemRole(dto.systemRole());
            }

            transaction.commit();
        }
        catch(Exception ex)
        {
            transaction.rollback();
            throw new InternalServerErrorException("Unknown error has occurred during the update: " +
                                                   ex.getClass().getName() + " - " +
                                                   ex.getMessage());
        }

        return emp;
    }

    /**
     * Allow an employee to self-update their own information
     *
     * Because the entity manager is only injected into the employee service, but not at the resource end point,
     * the resource end-point would not be able to check if the user id in the auth context actually exists or not.
     * THerefore, the RESTFul response is generated from this method directly upon calling.
     */
    public Response employeeSelfUpdatePassword(final int id, final EmployeeSelfUpdateDto dto) {
        Employee emp = em.find(Employee.class, id);

        if (emp == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (dto.empPassword() == null || dto.empPassword().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Bad Request: The new password cannot be " +
                                                                       "omitted or be blank").build();
        }

        emp.setEmpPassword(BCrypt.hashpw(dto.empPassword().trim(), BCrypt.gensalt()));

        // No-Content means status 204 code with a successful update result
        return Response.noContent().build();
    }

    /**
     * An employee service which can be invoked by a manager to delete an employee.
     *
     * @param id The ID of the employee which is to be deleted.
     * @return The HTTP Response Code with a detailed message to tell if the deletion was successful or not
     */
    public Response deleteEmployee(final int id)
    {
        Employee emp = em.find(Employee.class, id);

        if (emp == null)
        {
            return Response.status(Response.Status.NOT_FOUND).entity("The employee with ID " + id + " cannot be found" +
                                                                     ".").build();
        }

        EntityTransaction transaction = em.getTransaction();

        try
        {
            transaction.begin();
            em.remove(emp);
            transaction.commit();
        }
        catch(final Exception ex)
        {
            transaction.rollback();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unknown error has occurred when " +
                                                                                 "deleting the employee with ID " + id +
                                                                                 ": " + ex.getClass().getName() +
                                                                                 " - " + ex.getMessage()).build();
        }

        return Response.status(Response.Status.OK).entity("The employee with ID " + id  + " was successfully deleted.")
                       .build();

    }
}
