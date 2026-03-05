package com.corejsf.Service;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ejb.Stateless;

import com.corejsf.DTO.LaborGradeDTO;
import com.corejsf.Entity.LaborGrade;

@Stateless
public class LaborGradeService {

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
