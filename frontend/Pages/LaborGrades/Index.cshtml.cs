using System.Net.Http.Headers;
using System.Text.Json;
using frontend.model;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages.LaborGrades;

/**
 * Page model for the Labor Grades List page.
 * Provides methods for retrieving and deleting labor grade records.
 * Only accessible to ADMIN and OPERATIONS_MANAGER roles.
 */
public class IndexModel : PageModel
{
    private readonly IConfiguration _config;
    private readonly IHttpClientFactory _httpClientFactory;

    public IndexModel(IConfiguration config, IHttpClientFactory httpClientFactory)
    {
        _config = config;
        _httpClientFactory = httpClientFactory;
    }

    [TempData]
    public string? ErrorMessage { get; set; }

    /**
     * Handles GET requests to the Labor Grades List page.
     * Retrieves all labor grades from the API.
     */
    public async Task<IActionResult> OnGetAsync()
    {
        var token = HttpContext.Session.GetString("JWT");
        if (string.IsNullOrWhiteSpace(token))
            return RedirectToPage("/Login");

        var role = HttpContext.Session.GetString("SystemRole");
        if (role != "ADMIN" && role != "OPERATIONS_MANAGER")
            return RedirectToPage("/Index");

        var client = _httpClientFactory.CreateClient();
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
        var response = await client.GetAsync(_config["ApiBaseUrl"] + "/api/labor-grades");
        if (!response.IsSuccessStatusCode)
            return Page();

        var content = await response.Content.ReadAsStringAsync();
        var laborGrades = JsonSerializer.Deserialize<List<LaborGrade>>(content);
        ViewData["LaborGrades"] = laborGrades;

        return Page();
    }

    /**
     * Handles POST requests to delete a labor grade.
     * Calls DELETE /api/labor-grades/{id} on the backend.
     * Displays an error message if the labor grade is in use.
     */
    public async Task<IActionResult> OnPostDeleteAsync(int id)
    {
        var token = HttpContext.Session.GetString("JWT");
        if (string.IsNullOrWhiteSpace(token))
            return RedirectToPage("/Login");

        var role = HttpContext.Session.GetString("SystemRole");
        if (role != "ADMIN" && role != "OPERATIONS_MANAGER")
            return RedirectToPage("/Index");

        var client = _httpClientFactory.CreateClient();
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
        var response = await client.DeleteAsync(_config["ApiBaseUrl"] + $"/api/labor-grades/{id}");

        if (!response.IsSuccessStatusCode)
        {
            var errorContent = await response.Content.ReadAsStringAsync();
            ErrorMessage = !string.IsNullOrWhiteSpace(errorContent)
                ? errorContent
                : "Failed to delete labor grade. It may be in use.";
        }

        return RedirectToPage("/LaborGrades/Index");
    }
}
