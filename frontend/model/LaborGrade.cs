namespace frontend.model;

/**
 * Model class for a labor grade.
 * Represents a labor grade in the system.
 */
public class LaborGrade
{
    public int laborGradeId { get; set; }
    public string? gradeCode { get; set; }
    public decimal chargeRate { get; set; }
}
