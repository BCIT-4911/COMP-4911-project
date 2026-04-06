package com.corejsf.Service;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ejb.Stateless;

import com.corejsf.DTO.TimesheetRowResponseDTO;
import com.corejsf.Entity.TimesheetRow;

@Stateless
public class TimesheetRowService {

    public TimesheetRowResponseDTO toResponseDTO(TimesheetRow row) {
        TimesheetRowResponseDTO dto = new TimesheetRowResponseDTO();
        dto.setTsRowId(row.getTsRowId());
        dto.setWpId(row.getWorkPackage().getWpId());
        dto.setWpName(row.getWorkPackage().getWpName());
        dto.setLaborGradeId(row.getLaborGrade().getLaborGradeId());
        dto.setLaborGradeCode(row.getLaborGrade().getGradeCode());
        dto.setMonday(row.getMonday());
        dto.setTuesday(row.getTuesday());
        dto.setWednesday(row.getWednesday());
        dto.setThursday(row.getThursday());
        dto.setFriday(row.getFriday());
        dto.setSaturday(row.getSaturday());
        dto.setSunday(row.getSunday());
        return dto;
    }

    public List<TimesheetRowResponseDTO> toResponseDTOList(List<TimesheetRow> rows) {
        return rows.stream()
                   .map(this::toResponseDTO)
                   .collect(Collectors.toList());
    }
}
