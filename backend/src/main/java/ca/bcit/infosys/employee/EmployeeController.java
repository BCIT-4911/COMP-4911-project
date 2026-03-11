package ca.bcit.infosys.employee;

import java.util.List;

import com.corejsf.Entity.Employee;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Stateless
@Path("/employees")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EmployeeController {

    private EntityManager em;

    @GET
    public List<Employee> getAllEmployees() {
        return em.createQuery("SELECT e FROM Employee e", Employee.class).getResultList();
    }

    @GET
    @Path("/{id}")
    public Employee getEmployee(@PathParam("id") int id) {
        return em.find(Employee.class, id);
    }

    @POST
    public Employee createEmployee(Employee employee) {
        em.persist(employee);
        return employee;
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Employee updateEmployee(@PathParam("id") int id, Employee employee) {
        Employee existingEmployee = em.find(Employee.class, id);
        existingEmployee.setEmpFirstName(employee.getEmpFirstName());
        existingEmployee.setEmpLastName(employee.getEmpLastName());
        existingEmployee.setEmpPassword(employee.getEmpPassword());
        existingEmployee.setSystemRole(employee.getSystemRole());
        existingEmployee.setESignature(employee.getESignature());
        existingEmployee.setLaborGrade(employee.getLaborGrade());
        existingEmployee.setSupervisor(employee.getSupervisor());
        existingEmployee.setVacationSickBalance(employee.getVacationSickBalance());
        existingEmployee.setExpectedWeeklyHours(employee.getExpectedWeeklyHours());
        return existingEmployee;
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public void deleteEmployee(@PathParam("id") int id) {
        Employee employee = em.find(Employee.class, id);
        em.remove(employee);
    }
}
