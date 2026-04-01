package com.corejsf.DTO;

import com.corejsf.Entity.SystemRole;

/**
 * Inbound DTO for updating an employee by HR manager.
 */
public record EmployeeManagerUpdateDto(
    String firstName,
    String lastName,
    String password,
    Integer laborGradeId,
    Integer supervisorId,
    SystemRole systemRole
) {}
