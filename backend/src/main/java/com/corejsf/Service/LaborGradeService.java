package com.corejsf.Service;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.NotFoundException;

import com.corejsf.DTO.LaborGradeDTO;
import com.corejsf.Entity.LaborGrade;

@Stateless
public class LaborGradeService {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    public List<LaborGradeDTO> getAllLaborGrades() {
        List<LaborGrade> grades = em.createQuery(
                "SELECT lg FROM LaborGrade lg ORDER BY lg.laborGradeId", LaborGrade.class)
                .getResultList();
        return toDTOList(grades);
    }

    public LaborGradeDTO getLaborGrade(int id) {
        LaborGrade lg = em.find(LaborGrade.class, id);
        if (lg == null) {
            throw new NotFoundException("LaborGrade with id " + id + " not found.");
        }
        return toDTO(lg);
    }

    public LaborGradeDTO toDTO(LaborGrade lg) {
        LaborGradeDTO dto = new LaborGradeDTO();
        dto.setLaborGradeId(lg.getLaborGradeId());
        dto.setGradeCode(lg.getGradeCode());
        dto.setChargeRate(lg.getChargeRate());
        return dto;
    }

    public List<LaborGradeDTO> toDTOList(List<LaborGrade> list) {
        return list.stream()
                   .map(this::toDTO)
                   .collect(Collectors.toList());
    }
}
