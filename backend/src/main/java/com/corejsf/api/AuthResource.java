package com.corejsf.api;

import com.corejsf.DTO.LoginRequestDTO;
import com.corejsf.DTO.LoginResponseDTO;
import com.corejsf.Service.AuthService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
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
}
