package com.corejsf.api;

import java.io.IOException;

import com.corejsf.Service.JwtUtil;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(1000)
public class JwtAuthFilter implements ContainerRequestFilter {

    public static final String AUTHENTICATED_EMP_ID = "authenticatedEmpId";
    public static final String AUTHENTICATED_SYSTEM_ROLE = "authenticatedSystemRole";

    private static final String AUTH_LOGIN_PATH = "auth/login";

    @Inject
    private AuthContext authContext;

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
            authContext.setEmpId(claims.empId());
            authContext.setSystemRole(claims.systemRole());
        } catch (Exception e) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }
}
