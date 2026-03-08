package com.corejsf.api;

import java.io.IOException;

import com.corejsf.Service.JwtUtil;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * A filter for JWT that will authenticate a request if it is going to anything
 * under /api other then the login page.
 * Returns 401 unauthorized if the token is invalid or there is something wrong
 * with it.
 * Sets the authenticated empId and systemRole in the request so that other
 * controllers can use them.
 *
 * NOTE for controllers:
 * To get the authenticated empId: (Integer) requestContext.getProperty(JwtAuthFilter.AUTHENTICATED_EMP_ID)
 * To get the authenticated systemRole: (SystemRole) requestContext.getProperty(JwtAuthFilter.AUTHENTICATED_SYSTEM_ROLE)
 *
 * @Author Russell
 * @version 1.0
 */
@Provider
@Priority(1000)
public class JwtAuthFilter implements ContainerRequestFilter {

    public static final String AUTHENTICATED_EMP_ID = "authenticatedEmpId";
    public static final String AUTHENTICATED_SYSTEM_ROLE = "authenticatedSystemRole";

    private static final String AUTH_LOGIN_PATH = "auth/login";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if ("OPTIONS".equals(requestContext.getMethod())) {
            return;
        }

        String requestPath = requestContext.getUriInfo().getPath();
        if (requestPath != null && requestPath.contains(AUTH_LOGIN_PATH)) {
            return;
        }

        String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }

        String token = authHeader.substring("Bearer ".length()).trim();

        try {
            JwtUtil.JwtClaims claims = JwtUtil.validateToken(token);
            requestContext.setProperty(AUTHENTICATED_EMP_ID, claims.empId());
            requestContext.setProperty(AUTHENTICATED_SYSTEM_ROLE, claims.systemRole());
        } catch (Exception e) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }
}
