package com.corejsf.api;

import java.util.List;

import com.corejsf.DTO.LaborGradeDTO;
import com.corejsf.Service.LaborGradeService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Resource class for managing labor grade data.
 * Provides endpoints for retrieving labor grade information.
 */
@Path("/labor-grades")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LaborGradeResource {

    @Inject
    private LaborGradeService laborGradeService;

    /**
     * Retrieves all labor grades.
     * @return a list of all labor grades.
     */
    @GET
    public List<LaborGradeDTO> getAll() {
        return laborGradeService.getAllLaborGrades();
    }

    /**
     * Retrieves a labor grade by ID.
     * @param id the ID of the labor grade to retrieve.
     * @return the labor grade with the specified ID.
     * @throws NotFoundException if the labor grade with the specified ID is not found.
     */
    @GET
    @Path("/{id}")
    public LaborGradeDTO get(@PathParam("id") int id) {
        return laborGradeService.getLaborGrade(id);
    }
}
