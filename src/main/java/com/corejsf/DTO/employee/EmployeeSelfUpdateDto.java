package com.corejsf.DTO.employee;

import java.io.Serializable;

/**
 * DTO for {@link com.corejsf.Entity.Employee}
 */
public record EmployeeSelfUpdateDto(String empPassword) implements Serializable {}