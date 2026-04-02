using System.Net.Http.Headers;
using System.Text.Json;
using frontend.model;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages.Employee;

/**
 * Page model for the Employee List page.
 * Provides methods for retrieving and deleting employee records.
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

    /**
     * Handles GET requests to the Employee List page.
     * Retrieves all employees and labor grades from the API.
     */
    public async Task<IActionResult> OnGetAsync()
    {
        var token = HttpContext.Session.GetString("JWT");
        if (string.IsNullOrWhiteSpace(token))
            return RedirectToPage("/Login");

        var role = HttpContext.Session.GetString("SystemRole");
        if (role != "HR" && role != "ADMIN")
            return RedirectToPage("/Index");

        var client = _httpClientFactory.CreateClient();
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
        var employeeResponse = await client.GetAsync(_config["ApiBaseUrl"] + "/api/employees");
        if (!employeeResponse.IsSuccessStatusCode)
            return Page();
        var employeeContent = await employeeResponse.Content.ReadAsStringAsync();
        var employees = JsonSerializer.Deserialize<List<model.Employee>>(employeeContent);
        ViewData["Employees"] = employees;

        return Page();
    }

    /**
     * Handles POST requests to delete an employee.
     * Calls DELETE /api/employees/{id} on the backend.
     */
    public async Task<IActionResult> OnPostDeleteAsync(int id)
    {
        var token = HttpContext.Session.GetString("JWT");
        if (string.IsNullOrWhiteSpace(token))
            return RedirectToPage("/Login");

        var role = HttpContext.Session.GetString("SystemRole");
        if (role != "HR" && role != "ADMIN")
            return RedirectToPage("/Index");

        var client = _httpClientFactory.CreateClient();
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
        await client.DeleteAsync(_config["ApiBaseUrl"] + $"/api/employees/{id}");

        return RedirectToPage("/Employee/Index");
    }
}
