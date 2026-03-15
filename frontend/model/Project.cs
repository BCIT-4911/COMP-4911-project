using System.Text.Json.Serialization;

namespace frontend.model;

public class Project
{
    [JsonPropertyName("project_id")]
    public string ProjectId { get; set; }

    [JsonPropertyName("project_type")]
    public string ProjectType { get; set; }

    [JsonPropertyName("project_name")]
    public string ProjectName { get; set; }

    [JsonPropertyName("project_desc")]
    public string ProjectDesc { get; set; }

    [JsonPropertyName("project_status")]
    public string ProjectStatus { get; set; }

    [JsonPropertyName("start_date")]
    public string StartDate { get; set; }

    [JsonPropertyName("end_date")]
    public string EndDate { get; set; }

    [JsonPropertyName("markup_rate")]
    public decimal? MarkupRate { get; set; }

    [JsonPropertyName("project_manager_id")]
    public int? ProjectManagerId { get; set; }
}