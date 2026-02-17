package com.corejsf.Service;

import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import com.corejsf.DTO.TimesheetDTO;
import com.corejsf.Entity.Timesheet;
import com.corejsf.Entity.TimesheetRow;

@Stateless
public class TimesheetService {

    @Inject
    private TimesheetRowService timesheetRowService;

    /**
     * Converts a Timesheet entity + its rows into a TimesheetDTO.
     * Navigates relationships to populate empName, approverName, etc.
     */
    public TimesheetDTO toDTO(Timesheet ts, List<TimesheetRow> rows) {
        TimesheetDTO dto = new TimesheetDTO();
        dto.setTsId(ts.getTsId());
        dto.setEmpId(ts.getEmployee().getEmpId());
        dto.setEmpName(ts.getEmployee().getEmpFirstName() + " " + ts.getEmployee().getEmpLastName());
        dto.setWeekEnding(ts.getWeekEnding());
        dto.setAproved(ts.getApprovalStatus());
        if (ts.getApprover() != null) {
            dto.setApproverId(ts.getApprover().getEmpId());
            dto.setApproverName(ts.getApprover().getEmpFirstName() + " " + ts.getApprover().getEmpLastName());
        }
        dto.setReturnComment(ts.getReturnComment());
        dto.setRows(timesheetRowService.toDTOList(rows));
        return dto;
    }
}  
