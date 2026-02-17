package com.corejsf.Service;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ejb.Stateless;

import com.corejsf.DTO.TimesheetRowDTO;
import com.corejsf.Entity.TimesheetRow;

@Stateless
public class TimesheetRowService {

    public TimesheetRowDTO toDTO(TimesheetRow row) {
        TimesheetRowDTO dto = new TimesheetRowDTO();
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

    public List<TimesheetRowDTO> toDTOList(List<TimesheetRow> rows) {
        return rows.stream()
                   .map(this::toDTO)
                   .collect(Collectors.toList());
    }
}
