using System.Globalization;
using System.Net.Http.Headers;
using System.Text.Json;
using System.Text.Json.Serialization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages.EarnedValue;

public class IndexModel : PageModel
{
    private readonly IConfiguration _config;
    private readonly IHttpClientFactory _httpClientFactory;

    public IndexModel(IConfiguration config, IHttpClientFactory httpClientFactory)
    {
        _config = config;
        _httpClientFactory = httpClientFactory;
    }

    [BindProperty(SupportsGet = true)]
    public string SelectedProjectId { get; set; } = "";

    [BindProperty(SupportsGet = true)]
    public string SelectedWorkPackageId { get; set; } = "";

    public string ApiBaseUrl { get; private set; } = "";
    public string ProjectName { get; private set; } = "";
    public string ErrorMessage { get; private set; } = "";
    public bool HasReport => Report != null;
    public bool HasWorkPackageFilter => WorkPackageOptions.Count > 0;

    public List<ProjectOptionVm> Projects { get; private set; } = new();
    public List<WorkPackageOptionVm> WorkPackageOptions { get; private set; } = new();
    public MonthlyReportVm? Report { get; private set; }
    public MonthlyReportVm? DisplayReport => Report?.FilterByWorkPackage(SelectedWorkPackageId);

    public async Task<IActionResult> OnGetAsync()
    {
        var token = HttpContext.Session.GetString("JWT");
        if (string.IsNullOrWhiteSpace(token))
        {
            return RedirectToPage("/Login");
        }

        ApiBaseUrl = _config["ApiBaseUrl"] ?? "http://localhost:8080/Project";
        var client = _httpClientFactory.CreateClient();
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);

        Projects = await LoadProjectsAsync(client);

        if (string.IsNullOrWhiteSpace(SelectedProjectId))
        {
            return Page();
        }

        var selectedProject = Projects.FirstOrDefault(p => p.Id == SelectedProjectId);
        ProjectName = selectedProject?.Name ?? SelectedProjectId;

        var asOf = DateTime.Today.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture);
        var reportUrl = $"{ApiBaseUrl}/api/earned-value/projects/{Uri.EscapeDataString(SelectedProjectId)}/monthly-report?asOf={asOf}";

        try
        {
            var response = await client.GetAsync(reportUrl);
            if (!response.IsSuccessStatusCode)
            {
                ErrorMessage = $"Unable to load report ({(int)response.StatusCode}).";
                return Page();
            }

            var json = await response.Content.ReadAsStringAsync();
            var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
            var dto = JsonSerializer.Deserialize<MonthlyEvReportDto>(json, options);

            if (dto == null)
            {
                ErrorMessage = "The report response was empty.";
                return Page();
            }

            ProjectName = string.IsNullOrWhiteSpace(dto.ProjectName) ? ProjectName : dto.ProjectName;
            Report = MapReport(dto);
            WorkPackageOptions = Report.WorkPackages
                .Select(wp => new WorkPackageOptionVm(wp.WpId, wp.Description))
                .OrderBy(wp => wp.Name)
                .ToList();
        }
        catch
        {
            ErrorMessage = "Unable to connect to the report service.";
        }

        return Page();
    }

    public async Task<IActionResult> OnGetWorkPackageModalAsync(string wpId, string? asOf = null)
    {
        var token = HttpContext.Session.GetString("JWT");
        if (string.IsNullOrWhiteSpace(token))
        {
            return Unauthorized();
        }

        if (string.IsNullOrWhiteSpace(wpId))
        {
            return BadRequest(new { message = "A work package is required." });
        }

        ApiBaseUrl = _config["ApiBaseUrl"] ?? "http://localhost:8080/Project";
        var client = _httpClientFactory.CreateClient();
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);

        var effectiveAsOf = string.IsNullOrWhiteSpace(asOf)
            ? DateTime.Today.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture)
            : asOf;
        var modalUrl =
            $"{ApiBaseUrl}/api/earned-value/workpackages/{Uri.EscapeDataString(wpId)}/monthly-performance?asOf={Uri.EscapeDataString(effectiveAsOf)}";

        try
        {
            var response = await client.GetAsync(modalUrl);
            if (!response.IsSuccessStatusCode)
            {
                return StatusCode((int)response.StatusCode, new { message = "Unable to load work package details." });
            }

            var json = await response.Content.ReadAsStringAsync();
            var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
            var dto = JsonSerializer.Deserialize<WorkPackageMonthlyPerformanceDto>(json, options);

            if (dto == null)
            {
                return StatusCode(502, new { message = "The work package detail response was empty." });
            }

            return new JsonResult(dto);
        }
        catch
        {
            return StatusCode(503, new { message = "Unable to connect to the work package detail service." });
        }
    }

    private async Task<List<ProjectOptionVm>> LoadProjectsAsync(HttpClient client)
    {
        var response = await client.GetAsync($"{ApiBaseUrl}/api/projects");
        if (!response.IsSuccessStatusCode)
        {
            return new List<ProjectOptionVm>();
        }

        var json = await response.Content.ReadAsStringAsync();
        var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
        var projects = JsonSerializer.Deserialize<List<ProjectOptionDto>>(json, options) ?? new List<ProjectOptionDto>();

        return projects
            .Where(p => !string.IsNullOrWhiteSpace(p.ProjId))
            .Select(p => new ProjectOptionVm(
                p.ProjId!,
                string.IsNullOrWhiteSpace(p.ProjName) ? p.ProjId! : p.ProjName!))
            .OrderBy(p => p.Name)
            .ToList();
    }

    private static MonthlyReportVm MapReport(MonthlyEvReportDto dto)
    {
        var rows = (dto.WorkPackages ?? new List<MonthlyWorkPackageDto>())
            .Select(wp => new WorkPackageRowVm(
                wp.WpId ?? "",
                wp.WpName ?? "",
                InferEvMethod(wp.WpId),
                wp.Bac,
                wp.Bcws,
                wp.Bcwp,
                wp.Acwp,
                wp.Etc,
                wp.Sv,
                wp.Cv))
            .ToList();

        return new MonthlyReportVm(
            dto.ProjectBac,
            dto.ProjectBcws,
            dto.ProjectBcwp,
            dto.ProjectAcwp,
            rows);
    }

    private static string InferEvMethod(string? wpId)
    {
        return wpId switch
        {
            "A.WP-1" => "Percent Complete",
            "A.WP-2" => "0 / 100",
            "A.WP-3" => "Units Completed",
            _ => "Tracked Value"
        };
    }

    public record ProjectOptionVm(string Id, string Name);
    public record WorkPackageOptionVm(string Id, string Name);

    public record MonthlyReportVm(
        decimal ProjectBac,
        decimal ProjectBcws,
        decimal ProjectBcwp,
        decimal ProjectAcwp,
        List<WorkPackageRowVm> WorkPackages)
    {
        public decimal TotalSv => WorkPackages.Sum(wp => wp.Sv);
        public decimal TotalCv => WorkPackages.Sum(wp => wp.Cv);

        public MonthlyReportVm FilterByWorkPackage(string? wpId)
        {
            if (string.IsNullOrWhiteSpace(wpId))
            {
                return this;
            }

            var filteredRows = WorkPackages
                .Where(wp => string.Equals(wp.WpId, wpId, StringComparison.OrdinalIgnoreCase))
                .ToList();

            if (filteredRows.Count == 0)
            {
                return this;
            }

            return new MonthlyReportVm(
                filteredRows.Sum(wp => wp.Bac),
                filteredRows.Sum(wp => wp.Bcws),
                filteredRows.Sum(wp => wp.Bcwp),
                filteredRows.Sum(wp => wp.Acwp),
                filteredRows);
        }
    }

    public record WorkPackageRowVm(
        string WpId,
        string Description,
        string EvMethod,
        decimal Bac,
        decimal Bcws,
        decimal Bcwp,
        decimal Acwp,
        decimal Etc,
        decimal Sv,
        decimal Cv);

    public class ProjectOptionDto
    {
        [JsonPropertyName("project_id")]
        public string? ProjId { get; set; }

        [JsonPropertyName("project_name")]
        public string? ProjName { get; set; }
    }

    public class MonthlyEvReportDto
    {
        public string? ProjectId { get; set; }
        public string? ProjectName { get; set; }
        public decimal ProjectBac { get; set; }
        public decimal ProjectBcws { get; set; }
        public decimal ProjectBcwp { get; set; }
        public decimal ProjectAcwp { get; set; }
        public List<MonthlyWorkPackageDto>? WorkPackages { get; set; }
    }

    public class MonthlyWorkPackageDto
    {
        public string? WpId { get; set; }
        public string? WpName { get; set; }
        public decimal Bac { get; set; }
        public decimal Bcws { get; set; }
        public decimal Bcwp { get; set; }
        public decimal Acwp { get; set; }
        public decimal Etc { get; set; }
        public decimal Sv { get; set; }
        public decimal Cv { get; set; }
    }

    public class WorkPackageMonthlyPerformanceDto
    {
        public string? WpId { get; set; }
        public string? WpName { get; set; }
        public string? ProjectId { get; set; }
        public string? AsOfDate { get; set; }
        public decimal Bac { get; set; }
        public decimal Etc { get; set; }
        public decimal Eac { get; set; }
        public decimal Vac { get; set; }
        public List<string>? Months { get; set; }
        public List<decimal>? BcwsByMonth { get; set; }
        public List<decimal>? BcwpByMonth { get; set; }
        public List<decimal>? AcwpByMonth { get; set; }
        public List<decimal>? SvByMonth { get; set; }
        public List<decimal>? CvByMonth { get; set; }
    }
}
