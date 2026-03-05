package com.corejsf.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import com.corejsf.Service.DatabaseTestService;

@Path("/db-test")
public class TestDatabaseResource {

    @Inject
    private DatabaseTestService service;

    @GET
    public String test() {
        return service.testConnection();
    }
}