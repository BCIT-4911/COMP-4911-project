namespace frontend.data;

public class WorkPackageApiDto
{
    public string? wpId { get; set; }
    public int number { get; set; }
    public string? description { get; set; }
    public string? evMethod { get; set; }
    public string? type { get; set; }

    public Dictionary<int, decimal>? bcwsByWeek { get; set; }
    public Dictionary<int, decimal>? bcwpByWeek { get; set; }
    public Dictionary<int, decimal>? acwpByWeek { get; set; }

    public decimal totalBcws { get; set; }
    public decimal totalBcwp { get; set; }
    public decimal totalAcwp { get; set; }

}
