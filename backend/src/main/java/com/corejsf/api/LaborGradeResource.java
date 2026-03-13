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

@Path("/labor-grades")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LaborGradeResource {

    @Inject
    private LaborGradeService laborGradeService;

    @GET
    public List<LaborGradeDTO> getAll() {
        return laborGradeService.getAllLaborGrades();
    }

    @GET
    @Path("/{id}")
    public LaborGradeDTO get(@PathParam("id") int id) {
        return laborGradeService.getLaborGrade(id);
    }
}
