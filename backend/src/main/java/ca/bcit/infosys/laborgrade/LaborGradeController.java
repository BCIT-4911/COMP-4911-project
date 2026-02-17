package ca.bcit.infosys.laborgrade;

import java.util.List;

import com.corejsf.DTO.LaborGradeDTO;
import com.corejsf.Entity.LaborGrade;
import com.corejsf.Service.LaborGradeService;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * REST controller for labor grades.
 * Read-only lookup endpoint used by the timesheet frontend to populate dropdowns.
 */
@Stateless
@Path("/labor-grades")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LaborGradeController {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    @Inject
    private LaborGradeService laborGradeService;

    /**
     * Gets all labor grades.
     */
    @GET
    public List<LaborGradeDTO> getAllLaborGrades() {
        List<LaborGrade> grades = em.createQuery(
                "SELECT lg FROM LaborGrade lg ORDER BY lg.laborGradeId", LaborGrade.class)
                .getResultList();
        return laborGradeService.toDTOList(grades);
    }

    /**
     * Gets a single labor grade by ID.
     */
    @GET
    @Path("/{id}")
    public LaborGradeDTO getLaborGrade(@PathParam("id") int id) {
        LaborGrade lg = em.find(LaborGrade.class, id);
        if (lg == null) {
            throw new NotFoundException("LaborGrade with id " + id + " not found.");
        }
        return laborGradeService.toDTO(lg);
    }
}
