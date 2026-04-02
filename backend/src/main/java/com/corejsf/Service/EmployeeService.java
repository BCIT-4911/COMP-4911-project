package com.corejsf.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

import com.corejsf.DTO.EmployeeCreateDTO;
import com.corejsf.Entity.Employee;
import com.corejsf.Entity.EmployeeESignature;
import com.corejsf.Entity.LaborGrade;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.NotFoundException;

/**
 * Service class for managing employee data.
 * Provides methods for retrieving and creating employee records.
 */
@Stateless
public class EmployeeService {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    /**
     * Retrieves all employees.
     * @return a list of all employees.
     */
    public List<Employee> getAllEmployees() {
        return em.createQuery("SELECT e FROM Employee e ORDER BY e.empId", Employee.class)
                .getResultList();
    }

    /**
     * Retrieves an employee by ID.
     * @param id the ID of the employee to retrieve.
     * @return the employee with the specified ID.
     * @throws NotFoundException if the employee with the specified ID is not found.
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
     * @param dto the data transfer object containing new employee details.
     * @return the created employee.
     */
    public Employee createEmployee(EmployeeCreateDTO dto) {
        EmployeeESignature sig = new EmployeeESignature();
        Employee newEmp = new Employee();

        sig.setSignatureData(new byte[] { 0x00 });
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
     * Updates an existing employee.
     * @param id the ID of the employee to update.
     * @param dto the data transfer object containing updated employee details.
     * @return the updated employee.
     * @throws NotFoundException if the employee with the specified ID is not found.
     */
    public Employee updateEmployee(int id, EmployeeCreateDTO dto) {
        Employee emp = em.find(Employee.class, id);
        if (emp == null) {
            throw new NotFoundException("Employee with id " + id + " not found.");
        }
        emp.setEmpFirstName(dto.getFirstName());
        emp.setEmpLastName(dto.getLastName());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            emp.setEmpPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        }
        emp.setLaborGrade(em.find(LaborGrade.class, dto.getLaborGradeId()));
        emp.setSupervisor(em.find(Employee.class, dto.getSupervisorId()));
        emp.setSystemRole(dto.getSystemRole());
        em.merge(emp);
        return emp;
    }

    /**
     * Deletes an employee by ID.
     * @param id the ID of the employee to delete.
     * @throws NotFoundException if the employee with the specified ID is not found.
     */
    public void deleteEmployee(int id) {
        Employee emp = em.find(Employee.class, id);
        if (emp == null) {
            throw new NotFoundException("Employee with id " + id + " not found.");
        }

        List<String> reasons = new java.util.ArrayList<>();

        Long subordinateCount = em.createQuery(
                "SELECT COUNT(e) FROM Employee e WHERE e.supervisor.empId = :empId", Long.class)
                .setParameter("empId", id)
                .getSingleResult();
        if (subordinateCount > 0) {
            reasons.add("supervisor of " + subordinateCount + " employee(s)");
        }

        Long projectAssignmentCount = em.createQuery(
                "SELECT COUNT(pa) FROM ProjectAssignment pa WHERE pa.employee.empId = :empId", Long.class)
                .setParameter("empId", id)
                .getSingleResult();
        if (projectAssignmentCount > 0) {
            reasons.add("assigned to " + projectAssignmentCount + " project(s)");
        }

        Long wpAssignmentCount = em.createQuery(
                "SELECT COUNT(wpa) FROM WorkPackageAssignment wpa WHERE wpa.employee.empId = :empId", Long.class)
                .setParameter("empId", id)
                .getSingleResult();
        if (wpAssignmentCount > 0) {
            reasons.add("assigned to " + wpAssignmentCount + " work package(s)");
        }

        Long timesheetCount = em.createQuery(
                "SELECT COUNT(t) FROM Timesheet t WHERE t.employee.empId = :empId", Long.class)
                .setParameter("empId", id)
                .getSingleResult();
        if (timesheetCount > 0) {
            reasons.add("has " + timesheetCount + " timesheet(s)");
        }

        Long pmCount = em.createQuery(
                "SELECT COUNT(p) FROM Project p WHERE p.projectManager.empId = :empId", Long.class)
                .setParameter("empId", id)
                .getSingleResult();
        if (pmCount > 0) {
            reasons.add("project manager of " + pmCount + " project(s)");
        }

        Long reCount = em.createQuery(
                "SELECT COUNT(wp) FROM WorkPackage wp WHERE wp.responsibleEmployee.empId = :empId", Long.class)
                .setParameter("empId", id)
                .getSingleResult();
        if (reCount > 0) {
            reasons.add("responsible engineer on " + reCount + " work package(s)");
        }

        Long timeSheetCount = em.createQuery(
                "SELECT COUNT(ts) FROM Timesheet ts WHERE ts.employee.empId = :empId", Long.class)
                .setParameter("empId", id)
                .getSingleResult();
        if (timeSheetCount > 0) {
            reasons.add("has " + timeSheetCount + " timesheet(s)");
        }

        Long timeSheetApprovalCount = em.createQuery(
                "SELECT COUNT(tsa) FROM TimesheetApproval tsa WHERE tsa.approver.empId = :empId", Long.class)
                .setParameter("empId", id)
                .getSingleResult();
        if (timeSheetApprovalCount > 0) {
            reasons.add("has " + timeSheetApprovalCount + " timesheet approval(s)");
        }

        if (!reasons.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete employee (ID " + id + "): " + String.join(", ", reasons)
                    + ". Remove these associations first.");
        }

        em.remove(emp);
    }
}
