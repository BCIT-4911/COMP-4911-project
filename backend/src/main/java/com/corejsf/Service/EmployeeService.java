package com.corejsf.Service;

import com.corejsf.DTO.employee.EmployeeManagerUpdateDto;
import jakarta.ws.rs.BadRequestException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.Hibernate;
import org.mindrot.jbcrypt.BCrypt;

import com.corejsf.DTO.employee.EmployeeCreateDTO;
import com.corejsf.DTO.employee.EmployeeSelfUpdateDto;

import com.corejsf.Entity.Employee;
import com.corejsf.Entity.EmployeeESignature;
import com.corejsf.Entity.LaborGrade;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
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

    // These strings are for creation error message
    private final static String EMPLOYEE_FIRST_NAME_ERROR = "The employee's new first name cannot be null or blank";
    private final static String EMPLOYEE_LAST_NAME_ERROR = "The employee's new last name cannot be null or blank";
    private final static String EMPLOYEE_PASSWORD_ERROR = "The employee's new password cannot be null or blank";
    private final static String EMPLOYEE_SUPERVISOR_ID_ERROR = "The employee's new supervisor ID cannot be null or less than 1.";
    private final static String EMPLOYEE_SUPERVISOR_ID_NOT_ERROR_PREFIX = "The employee's new supervisor ID cannot be found: ";
    private final static String EMPLOYEE_LABOUR_GRADE_ID_ERROR = "The employee's new labour grade cannot be null or less than 1.";
    private final static String EMPLOYEE_LABOUR_GRADE_NOT_ID_FOUND_ERROR_PREFIX = "The employee's new labour grade ID cannot be found: ";
    private final static String EMPLOYEE_SYSTEM_ROLE_ERROR = "The employee's system role cannot be null";

    /**
     * Retrieves all employees.
     *
     * @return a list of all employees.
     */
    public List<Employee> getAllEmployees() {
        List<Employee> results = em.createQuery(
                "SELECT e FROM Employee e ORDER BY e.empId", Employee.class)
                .getResultList();
        results.replaceAll(e -> (Employee) Hibernate.unproxy(e));
        return results;
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
        return (Employee) Hibernate.unproxy(emp);
    }

    /**
     * Creates a new employee with field validation.
     *
     * @param dto the data transfer object containing new employee details.
     * @return the created employee.
     */
    public Employee createEmployee(final EmployeeCreateDTO dto)
    {
        if (dto.firstName() == null || dto.firstName().isBlank())
        {
            throw new BadRequestException(EMPLOYEE_FIRST_NAME_ERROR);
        }

        if (dto.lastName() == null || dto.lastName().isBlank())
        {
            throw new BadRequestException(EMPLOYEE_LAST_NAME_ERROR);
        }

        if (dto.password() == null || dto.password().isBlank())
        {
            throw new BadRequestException(EMPLOYEE_PASSWORD_ERROR);
        }

        if (dto.supervisorId() == null || dto.supervisorId() < 1)
        {
            throw new BadRequestException(EMPLOYEE_SUPERVISOR_ID_ERROR);
        }

        if (dto.laborGradeId() == null || dto.laborGradeId() < 1)
        {
            throw new BadRequestException(EMPLOYEE_LABOUR_GRADE_ID_ERROR);
        }

        final Employee supervisor = em.find(Employee.class, dto.supervisorId());

        if (supervisor == null)
        {
            throw new BadRequestException(EMPLOYEE_SUPERVISOR_ID_NOT_ERROR_PREFIX + dto.supervisorId());
        }

        final LaborGrade laborGrade = em.find(LaborGrade.class, dto.laborGradeId());

        if (laborGrade == null)
        {
            throw new BadRequestException(EMPLOYEE_LABOUR_GRADE_NOT_ID_FOUND_ERROR_PREFIX + dto.laborGradeId());
        }

        if (dto.systemRole() == null)
        {
            throw new BadRequestException(EMPLOYEE_SYSTEM_ROLE_ERROR);
        }

        EmployeeESignature sig = new EmployeeESignature();
        sig.setSignatureData(new byte[]{0x00});
        sig.setSignedAt(LocalDateTime.now());

        Employee newEmp = new Employee();

        em.persist(sig);

        newEmp.setEmpFirstName(dto.firstName());
        newEmp.setEmpLastName(dto.lastName());
        newEmp.setEmpPassword(BCrypt.hashpw(dto.password(), BCrypt.gensalt()));
        newEmp.setLaborGrade(laborGrade);
        newEmp.setSupervisor(supervisor);
        newEmp.setVacationSickBalance(new BigDecimal(0));
        newEmp.setExpectedWeeklyHours(new BigDecimal(40));
        newEmp.setSystemRole(dto.systemRole());
        newEmp.setESignature(sig);

        em.persist(newEmp);

        return newEmp;
    }

    /**
     * This update employee function validates all requested fields before processing them to fulfill ACID rules
     *
     * @param id The employee ID
     * @param dto An EmployeeManagerUpdateDto class which contains the personal details of the employee. The manager
     *            can skip updating certain fields using null value in the data transfer object.
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

        if (dto.password() != null && dto.password().isBlank())
        {
            throw new BadRequestException("The new password cannot be blank (filled only with space character).");
        }

        LaborGrade newLaborGrade = null;

        if (dto.laborGradeId() != null)
        {
            if (dto.laborGradeId() > 0)
            {
                if ((newLaborGrade = em.find(LaborGrade.class, dto.laborGradeId())) == null)
                {
                    throw new BadRequestException("The new labour grade under id " + dto.laborGradeId() + "could not " +
                                                  "be found.");
                }
            }
            else
            {
                throw new BadRequestException("The new labour grade id should not be equal to or smaller than 0.");
            }
        }

        Employee newSupervisor = null;

        if (dto.supervisorId() != null)
        {
            if (dto.supervisorId() > 0)
            {
                if ((newSupervisor = em.find(Employee.class, dto.supervisorId())) == null)
                {
                    throw new BadRequestException("The new supervisor with id " + dto.laborGradeId() + " could not be" +
                                                  " found.");
                }
            }
            else
            {
                throw new BadRequestException("The new supervisor id should not be equal to or smaller than 0.");
            }
        }

        if (dto.firstName() != null) {
            emp.setEmpFirstName(dto.firstName().trim());
        }

        if (dto.lastName() != null) {
            emp.setEmpLastName(dto.lastName().trim());
        }

        if (dto.password() != null) {
            emp.setEmpPassword(BCrypt.hashpw(dto.password().trim(), BCrypt.gensalt()));
        }

        if (newSupervisor != null) {
            emp.setSupervisor(newSupervisor);
        }

        if (newLaborGrade != null) {
            emp.setLaborGrade(newLaborGrade);
        }

        if (dto.systemRole() != null) {
            emp.setSystemRole(dto.systemRole());
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

        try {
            em.remove(emp);
        } catch (final Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error deleting employee " + id + ": "
                            + ex.getClass().getName() + " - " + ex.getMessage()).build();
        }

        return Response.status(Response.Status.OK).entity("The employee with ID " + id  + " was successfully deleted.")
                       .build();

    }
}
