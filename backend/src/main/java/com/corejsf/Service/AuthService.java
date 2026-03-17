package com.corejsf.Service;

import org.mindrot.jbcrypt.BCrypt;

import com.corejsf.Entity.Employee;
import com.corejsf.Entity.SystemRole;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@Stateless
public class AuthService {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    /**
     * Authenticates by empId and password. Returns signed JWT on success.
     *
     * @throws WebApplicationException with 401 if credentials are invalid
     */
    public String authenticate(int empId, String rawPassword) {
        Employee employee = em.find(Employee.class, empId);
        if (employee == null) {
            throw new WebApplicationException(
                    Response.status(401).entity("Invalid credentials").build());
        }

        String storedHash = employee.getEmpPassword();
        if (storedHash == null || !BCrypt.checkpw(rawPassword, storedHash)) {
            throw new WebApplicationException(
                    Response.status(401).entity("Invalid credentials").build());
        }

        SystemRole role = employee.getSystemRole() != null
                ? employee.getSystemRole()
                : SystemRole.EMPLOYEE;

        return JwtUtil.generateToken(employee.getEmpId(), role);
    }
}
