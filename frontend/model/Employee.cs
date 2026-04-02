namespace frontend.model;

/**
 * Model class for an employee.
 * Represents an employee in the system.
 */
public class Employee
{
    public int empId { get; set; }
    public string? empFirstName { get; set; }
    public string? empLastName { get; set; }
    public string? systemRole { get; set; }
    public decimal vacationSickBalance { get; set; }
    public decimal expectedWeeklyHours { get; set; }

    [System.Text.Json.Serialization.JsonPropertyName("supervisor_id")]
    public int? supervisorId { get; set; }

    [System.Text.Json.Serialization.JsonPropertyName("labor_grade_id")]
    public int? laborGradeId { get; set; }
}