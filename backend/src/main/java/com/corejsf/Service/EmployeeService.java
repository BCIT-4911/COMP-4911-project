package com.corejsf.Service;

import java.util.List;

import com.corejsf.DTO.EmployeeCreateDTO;
import com.corejsf.Entity.Employee;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;

@Stateless
public class EmployeeService {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    public List<Employee> getAllEmployees() {
        return em.createQuery("SELECT e FROM Employee e ORDER BY e.empId", Employee.class)
                .getResultList();
    }

    public Employee getEmployee(int id) {
        Employee emp = em.find(Employee.class, id);
        if (emp == null) {
            throw new NotFoundException("Employee with id " + id + " not found.");
        }
        return emp;
    }

    /**
     * Creates a new employee. Skeleton: Team Onboarding implements.
     */
    public Employee createEmployee(EmployeeCreateDTO dto) {
        throw new WebApplicationException("Team Onboarding implements employee registration", 501);
    }
}
