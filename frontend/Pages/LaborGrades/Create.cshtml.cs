using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using frontend.model;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages.LaborGrades;

/**
 * Page model for the Labor Grade Create/Edit page.
 * Handles both creating new and updating existing labor grades.
 * Only accessible to ADMIN and OPERATIONS_MANAGER roles.
 */
public class CreateModel : PageModel
{
    private readonly IConfiguration _config;
    private readonly IHttpClientFactory _httpClientFactory;

    public CreateModel(IConfiguration config, IHttpClientFactory httpClientFactory)
    {
        _config = config;
        _httpClientFactory = httpClientFactory;
    }

    [BindProperty(SupportsGet = true)]
    public int? LaborGradeId { get; set; }

    [BindProperty]
    public string? GradeCode { get; set; }

    [BindProperty]
    public decimal ChargeRate { get; set; }

    public string? ErrorMessage { get; set; }

    // Alias used by the route: /LaborGrades/Create?id=3
    [BindProperty(SupportsGet = true, Name = "id")]
    public int? Id
    {
        get => LaborGradeId;
        set => LaborGradeId = value;
    }

    /**
     * Handles GET requests.
     * If an ID is provided, loads the existing labor grade for editing.
     */
    public async Task<IActionResult> OnGetAsync()
    {
        var token = HttpContext.Session.GetString("JWT");
        if (string.IsNullOrWhiteSpace(token))
            return RedirectToPage("/Login");

        var role = HttpContext.Session.GetString("SystemRole");
        if (role != "ADMIN" && role != "OPERATIONS_MANAGER")
            return RedirectToPage("/Index");

        if (LaborGradeId.HasValue)
        {
            var client = _httpClientFactory.CreateClient();
            client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
            var response = await client.GetAsync(_config["ApiBaseUrl"] + $"/api/labor-grades/{LaborGradeId.Value}");

            if (response.IsSuccessStatusCode)
            {
                var content = await response.Content.ReadAsStringAsync();
                var lg = JsonSerializer.Deserialize<LaborGrade>(content);
                if (lg != null)
                {
                    GradeCode = lg.gradeCode;
                    ChargeRate = lg.chargeRate;
                }
            }
            else
            {
                ErrorMessage = "Failed to load labor grade.";
            }
        }

        return Page();
    }

    /**
     * Handles POST requests.
     * Creates a new labor grade or updates an existing one based on whether LaborGradeId is set.
     */
    public async Task<IActionResult> OnPostAsync()
    {
        var token = HttpContext.Session.GetString("JWT");
        if (string.IsNullOrWhiteSpace(token))
            return RedirectToPage("/Login");

        var role = HttpContext.Session.GetString("SystemRole");
        if (role != "ADMIN" && role != "OPERATIONS_MANAGER")
            return RedirectToPage("/Index");

        if (string.IsNullOrWhiteSpace(GradeCode))
        {
            ErrorMessage = "Grade Code is required.";
            return Page();
        }

        if (GradeCode.Length > 2)
        {
            ErrorMessage = "Grade Code must be at most 2 characters.";
            return Page();
        }

        var client = _httpClientFactory.CreateClient();
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);

        var payload = new
        {
            gradeCode = GradeCode,
            chargeRate = ChargeRate
        };

        var json = JsonSerializer.Serialize(payload);
        var httpContent = new StringContent(json, Encoding.UTF8, "application/json");

        HttpResponseMessage response;

        if (LaborGradeId.HasValue)
        {
            // Update existing
            response = await client.PutAsync(
                _config["ApiBaseUrl"] + $"/api/labor-grades/{LaborGradeId.Value}",
                httpContent);
        }
        else
        {
            // Create new
            response = await client.PostAsync(
                _config["ApiBaseUrl"] + "/api/labor-grades",
                httpContent);
        }

        if (!response.IsSuccessStatusCode)
        {
            ErrorMessage = "Failed to save labor grade. Please try again.";
            return Page();
        }

        return RedirectToPage("/LaborGrades/Index");
    }
}
