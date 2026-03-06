package com.corejsf.Service;

import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import com.corejsf.DTO.TimesheetResponseDTO;
import com.corejsf.Entity.Timesheet;
import com.corejsf.Entity.TimesheetRow;

@Stateless
public class TimesheetService {

    @Inject
    private TimesheetRowService timesheetRowService;

    /**
     * Converts a Timesheet entity + its rows into a TimesheetResponseDTO.
     * Navigates relationships to populate empName, approverName, etc.
     */
    public TimesheetResponseDTO toResponseDTO(Timesheet ts, List<TimesheetRow> rows) {
        TimesheetResponseDTO dto = new TimesheetResponseDTO();
        dto.setTsId(ts.getTsId());
        dto.setEmpId(ts.getEmployee().getEmpId());
        dto.setEmpName(ts.getEmployee().getEmpFirstName() + " " + ts.getEmployee().getEmpLastName());
        dto.setWeekEnding(ts.getWeekEnding());
        dto.setApproved(ts.getApprovalStatus());
        if (ts.getApprover() != null) {
            dto.setApproverId(ts.getApprover().getEmpId());
            dto.setApproverName(ts.getApprover().getEmpFirstName() + " " + ts.getApprover().getEmpLastName());
        }
        dto.setReturnComment(ts.getReturnComment());
        dto.setRows(timesheetRowService.toResponseDTOList(rows));
        return dto;
    }
}  
