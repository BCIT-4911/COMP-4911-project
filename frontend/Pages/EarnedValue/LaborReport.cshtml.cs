using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using System.Net.Http.Headers;
using System.Text.Json;
using System.Text.Json.Serialization;
using frontend.model;

namespace frontend.Pages.EarnedValue;


/// PageModel for the Weekly Labor Report page.
///
/// Flow:
///   1. OnGetAsync runs on every page load.
///   2. If no weekEnding has been supplied yet we just render the empty filter
///      form (HasResults = false) so the user can pick their filters.
///   3. Once the user fills in at least weekEnding and clicks "View Report",
///      the GET form re-submits with query params and we call the backend API.
///   4. We deserialise the response into ReportSummary + ReportRows and let
///      the .cshtml template render them.
///
/// Backend endpoint used:
///   GET /api/labor-grades/report
///       ?projectId=...   (optional)
///       &employeeId=...  (optional)
///       &weekEnding=YYYY-MM-DD  (required)

public class LaborReportModel : PageModel
{
    private readonly IConfiguration _config;
    private readonly IHttpClientFactory _httpClientFactory;

    // -----------------------------------------------------------------------
    // Bound filter properties — populated from the GET query string
    // -----------------------------------------------------------------------

    // Selected project ID filter. Empty string = All Projects.
    [BindProperty(SupportsGet = true)]
    public string SelectedProjectId { get; set; } = "";

    // Selected employee ID filter. 0 = All Employees.
    [BindProperty(SupportsGet = true)]
    public int? SelectedEmployeeId { get; set; }

    // Week-ending date in YYYY-MM-DD format (required to run report).
    [BindProperty(SupportsGet = true)]
    public string WeekEnding { get; set; } = "";

    // -----------------------------------------------------------------------
    // Dropdown data fetched on every load
    // -----------------------------------------------------------------------

    public List<Project> Projects { get; set; } = new();
    public List<Employee> Employees { get; set; } = new();

    // -----------------------------------------------------------------------
    // Report results — null until the user runs the report
    // -----------------------------------------------------------------------

    // True when a valid report has been loaded from the API.
    public bool HasResults { get; set; } = false;

    public LaborReportSummaryVm? Summary { get; set; }
    public List<LaborReportRowVm> Rows { get; set; } = new();

    // Total hours shown in the TOTALS footer row.
    public decimal TotalHours { get; set; }

    public string? ErrorMessage { get; set; }

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public LaborReportModel(IConfiguration config, IHttpClientFactory httpClientFactory)
    {
        _config = config;
        _httpClientFactory = httpClientFactory;
    }

    // -----------------------------------------------------------------------
    // Page handler
    // -----------------------------------------------------------------------

    public async Task<IActionResult> OnGetAsync()
    {
        var token = HttpContext.Session.GetString("JWT");
        if (string.IsNullOrWhiteSpace(token))
            return RedirectToPage("/Login");

        var apiBase = _config["ApiBaseUrl"] ?? "http://localhost:8080/Project";

        var http = _httpClientFactory.CreateClient();
        http.DefaultRequestHeaders.Authorization =
            new AuthenticationHeaderValue("Bearer", token);

        var jsonOpts = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };

        // ------------------------------------------------------------------
        // Always load the project dropdown so the filter form is populated
        // ------------------------------------------------------------------
        try
        {
            var projRes = await http.GetAsync(apiBase + "/api/projects");
            if (projRes.IsSuccessStatusCode)
            {
                var projJson = await projRes.Content.ReadAsStringAsync();
                Projects = JsonSerializer.Deserialize<List<Project>>(projJson, jsonOpts)
                           ?? new();
            }
        }
        catch
        {
            // Non-fatal — dropdown will be empty, report can still run
        }

        // ------------------------------------------------------------------
        // Always load the employee dropdown
        // ------------------------------------------------------------------
        try
        {
            var empRes = await http.GetAsync(apiBase + "/api/employees");
            if (empRes.IsSuccessStatusCode)
            {
                var empJson = await empRes.Content.ReadAsStringAsync();
                Employees = JsonSerializer.Deserialize<List<Employee>>(empJson, jsonOpts)
                            ?? new();
            }
        }
        catch
        {
            // Non-fatal
        }

        // ------------------------------------------------------------------
        // Only call the report endpoint when the user has provided a weekEnding
        // ------------------------------------------------------------------
        if (string.IsNullOrWhiteSpace(WeekEnding))
            return Page();

        // Build query string for the report API
        var query = $"weekEnding={Uri.EscapeDataString(WeekEnding)}";
        if (!string.IsNullOrWhiteSpace(SelectedProjectId))
            query += $"&projectId={Uri.EscapeDataString(SelectedProjectId)}";
        if (SelectedEmployeeId.HasValue && SelectedEmployeeId.Value > 0)
            query += $"&employeeId={SelectedEmployeeId.Value}";

        var url = apiBase + "/api/labor-grades/report?" + query;

        try
        {
            var res = await http.GetAsync(url);

            if (res.StatusCode == System.Net.HttpStatusCode.Forbidden)
            {
                ErrorMessage = "You do not have permission to view this report.";
                return Page();
            }

            if (res.StatusCode == System.Net.HttpStatusCode.BadRequest)
            {
                ErrorMessage = "Invalid filter parameters. Please check your inputs.";
                return Page();
            }

            if (!res.IsSuccessStatusCode)
            {
                ErrorMessage = $"Could not load report (HTTP {(int)res.StatusCode}).";
                return Page();
            }

            var body = await res.Content.ReadAsStringAsync();
            var dto  = JsonSerializer.Deserialize<LaborReportApiDto>(body, jsonOpts);

            if (dto == null)
            {
                ErrorMessage = "Received an empty response from the server.";
                return Page();
            }

            // Map API DTO → view models
            if (dto.Summary != null)
            {
                Summary = new LaborReportSummaryVm
                {
                    TotalHours           = dto.Summary.TotalHours,
                    OvertimeHours        = dto.Summary.OvertimeHours,
                    PendingApprovalHours = dto.Summary.PendingApprovalHours,
                    ActiveWorkPackages   = dto.Summary.ActiveWorkPackages,
                    HoursChangePercent   = dto.Summary.HoursChangePercent,
                };
            }

            Rows = (dto.Rows ?? new())
                .Select(r => new LaborReportRowVm
                {
                    EmployeeName    = r.EmployeeName    ?? "",
                    WorkPackageName = r.WorkPackageName ?? "",
                    LaborGradeCode  = r.LaborGradeCode  ?? "",
                    Hours           = r.Hours,
                    WeekEnding      = r.WeekEnding,
                    StatusLabel     = r.StatusLabel     ?? r.Status ?? "",
                })
                .ToList();

            TotalHours  = Rows.Sum(r => r.Hours);
            HasResults  = true;
        }
        catch (Exception ex)
        {
            ErrorMessage = "Failed to load report: " + ex.Message;
        }

        return Page();
    }

    // -----------------------------------------------------------------------
    // View models (flat representations for the Razor template)
    // -----------------------------------------------------------------------

    public class LaborReportSummaryVm
    {
        public decimal TotalHours           { get; set; }
        public decimal OvertimeHours        { get; set; }
        public decimal PendingApprovalHours { get; set; }
        public long    ActiveWorkPackages   { get; set; }
        public decimal HoursChangePercent   { get; set; }
    }

    public class LaborReportRowVm
    {
        public string   EmployeeName    { get; set; } = "";
        public string   WorkPackageName { get; set; } = "";
        public string   LaborGradeCode  { get; set; } = "";
        public decimal  Hours           { get; set; }
        public DateTime WeekEnding      { get; set; }
        public string   StatusLabel     { get; set; } = "";
    }

    // -----------------------------------------------------------------------
    // API DTOs — mirror the backend's LaborReportDTO / LaborReportRowDTO
    // -----------------------------------------------------------------------

    private class LaborReportApiDto
    {
        public SummaryApiDto?       Summary        { get; set; }
        public List<RowApiDto>?     Rows           { get; set; }
        public decimal              TotalRowHours  { get; set; }
    }

    private class SummaryApiDto
    {
        public decimal TotalHours           { get; set; }
        public decimal PreviousWeekHours    { get; set; }
        public decimal HoursChangePercent   { get; set; }
        public decimal OvertimeHours        { get; set; }
        public decimal PendingApprovalHours { get; set; }
        public long    ActiveWorkPackages   { get; set; }
    }

    private class RowApiDto
    {
        public string?   EmployeeName    { get; set; }
        public string?   WorkPackageName { get; set; }
        public string?   LaborGradeCode  { get; set; }
        public decimal   Hours           { get; set; }
        public DateTime  WeekEnding      { get; set; }
        public string?   Status          { get; set; }
        public string?   StatusLabel     { get; set; }
    }
}