package com.corejsf.api;

import java.io.IOException;

import com.corejsf.Service.JwtUtil;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS request filter that validates JWT bearer tokens for protected API
 * requests.
 *
 * <p>
 * This filter skips authentication for {@code OPTIONS} requests and the
 * login endpoint ({@code auth/login}). For all other requests, it expects an
 * {@code Authorization} header in the form {@code Bearer <token>}.
 *
 * <p>
 * When the token is valid, the authenticated employee ID and system role
 * are stored in the request-scoped {@link AuthContext}. If the token is
 * missing, malformed, or invalid, the request is aborted with
 * {@code 401 Unauthorized}.
 *
 * <p>
 * {@link AuthContext} is used so resource classes can inject authentication
 * data directly without relying on {@code @Context ContainerRequestContext},
 * which can cause CDI/RESTEasy proxy issues in WildFly.
 *
 * @author Russell M.
 * @author Nathan O.
 * @author Lucas L.
 * @version 2.1
 */
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
