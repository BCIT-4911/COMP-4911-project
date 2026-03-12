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
        newEmp.setESignature(sig);
        em.persist(newEmp);
        return newEmp;
    }
}
