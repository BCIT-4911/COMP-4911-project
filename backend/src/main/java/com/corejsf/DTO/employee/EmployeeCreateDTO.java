package com.corejsf.DTO.employee;

import java.io.Serializable;

import com.corejsf.Entity.SystemRole;

/**
 * Inbound DTO for creating an employee. Team Onboarding implements the full
 * registration flow.
 */
public record EmployeeCreateDTO(
        String firstName,
        String lastName,
        String password,
        Integer laborGradeId,
        Integer supervisorId,
        SystemRole systemRole
        ) implements Serializable {

}
