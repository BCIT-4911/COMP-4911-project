package com.corejsf.api;

import java.io.IOException;

import com.corejsf.Entity.Employee;
import com.corejsf.Service.JwtUtil;

import jakarta.annotation.Priority;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * A filter for JWT that will authenticate a request if it is going to anything
 * under /api other then the login page.
 * Returns 401 unauthorized if the token is invalid or there is something wrong
 * with it.
 * sets the authenticated employee in the request so that other controllers can
 * use it after.
 * 
 * NOTE for controllers:
 * to get the authenticated employee from a request you can do this:
 * Employee employee = (Employee)
 * requestContext.getProperty(JwtAuthFilter.AUTHENTICATED_EMPLOYEE);
 * 
 * @Author Russell
 * @verson 1.0
 */
@Provider
@Priority(1000) // Authentication priority to make sure it runs before other things
public class JwtAuthFilter implements ContainerRequestFilter {

    
    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    public static final String AUTHENTICATED_EMPLOYEE = "authenticatedEmployee";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String requestPath = requestContext.getUriInfo().getPath();
        //added filter so that login page can be access without token
        if (requestPath.equals("auth/login") || requestPath.endsWith("/auth/login")) {
            return;
        }
        System.out.println("Filter path: " + requestPath);

        // If the request is for the login page for not for an api/* page then return and let the request carry on
        if (requestPath.startsWith("auth/login")) {
         return;
        }

    
        // Get the auth header from the request and then get the token from it.
        String authHeader = requestContext.getHeaderString("Authorization");
        String token;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring("Bearer ".length());
        } else {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }

        // Get the empId from the token
        Integer empId;
        try {
            JwtUtil.JwtClaims claims = JwtUtil.validateToken(token);
            empId = claims.empId();
        } catch (IllegalArgumentException e) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }

        // fetch the employee from the empId.
        Employee employee = em.find(Employee.class, empId);
        if (employee == null) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }

        // set the employee in the request
        requestContext.setProperty(AUTHENTICATED_EMPLOYEE, employee);

    }

}