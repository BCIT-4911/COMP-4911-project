package com.corejsf.api;

import java.util.List;

import com.corejsf.DTO.LoginRequestDTO;
import com.corejsf.DTO.LoginResponseDTO;
import com.corejsf.Service.AuthService;
import com.corejsf.Service.RebacService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    private AuthService authService;

    @Inject
    private RebacService rebacService;

    @Inject
    private AuthContext authContext;

    @POST
    @Path("/login")
    public Response login(LoginRequestDTO request) {
        if (request == null || request.getEmpId() == null || request.getPassword() == null) {
            return Response.status(400).entity("empId and password are required").build();
        }

        String token = authService.authenticate(request.getEmpId(), request.getPassword());

        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(token);
        return Response.ok(response).build();
    }

    @GET
    @Path("/can-access-approver-dashboard")
    public Response canAccessApproverDashboard() {
        boolean allowed = rebacService.canAccessApproverDashboard(authContext.getEmpId());
        return Response.ok(new AccessResponse(allowed)).build();
    }

    @GET
    @Path("/direct-reports")
    public Response getDirectReports() {
        List<Integer> employeeIds = rebacService.getDirectReportEmpIds(authContext.getEmpId());
        return Response.ok(new DirectReportsResponse(employeeIds)).build();
    }

    public static class AccessResponse {
        public boolean allowed;

        public AccessResponse(boolean allowed) {
            this.allowed = allowed;
        }
    }

    public static class DirectReportsResponse {
        public List<Integer> employeeIds;

        public DirectReportsResponse(List<Integer> employeeIds) {
            this.employeeIds = employeeIds;
        }
    }
}
