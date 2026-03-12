namespace frontend.model;

/**
 * Model class for an employee DTO.
 * Represents an employee in the system.
 */
public class EmployeeDto
{
    public string? firstName { get; set; }
    public string? lastName { get; set; }
    public string? password { get; set; }
    public int laborGradeId { get; set; }
    public int supervisorId { get; set; }
    public string? systemRole { get; set; }
}