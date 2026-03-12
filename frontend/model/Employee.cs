namespace frontend.model;

public class Employee
{
    public int empId { get; set; }
    public string empFirstName { get; set; }
    public string empLastName { get; set; }
    public string systemRole { get; set; }
    public decimal vacationSickBalance { get; set; }
    public decimal expectedWeeklyHours { get; set; }
}