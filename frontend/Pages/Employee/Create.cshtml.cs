using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using frontend.model;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages.Employee;

/**
 * Page model for the Create Employee page.
 * Provides methods for creating a new employee record.
 */
public class CreateModel : PageModel
{
    private readonly IConfiguration _config;
    private readonly IHttpClientFactory _httpClientFactory;

    [BindProperty]
    public EmployeeDto employeeDto { get; set; } = new();

    public CreateModel(IConfiguration config, IHttpClientFactory httpClientFactory)
    {
        _config = config;
        _httpClientFactory = httpClientFactory;
    }

    /**
     * Handles GET requests to the Create Employee page.
     * Retrieves employees and labor grades for the dropdown fields.
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
        var laborGradeResponse = await client.GetAsync(_config["ApiBaseUrl"] + "/api/labor-grades");

        if (employeeResponse.IsSuccessStatusCode)
        {
            var employeeContent = await employeeResponse.Content.ReadAsStringAsync();
            ViewData["Employees"] = JsonSerializer.Deserialize<List<model.Employee>>(employeeContent);
        }
        else
        {
            ViewData["Employees"] = new List<model.Employee>();
        }

        if (laborGradeResponse.IsSuccessStatusCode)
        {
            var laborGradeContent = await laborGradeResponse.Content.ReadAsStringAsync();
            ViewData["LaborGrades"] = JsonSerializer.Deserialize<List<LaborGrade>>(laborGradeContent);
        }
        else
        {
            ViewData["LaborGrades"] = new List<LaborGrade>();
        }

        return Page();
    }

    /**
     * Handles POST requests to create a new employee.
     * Sends employee data to the API and redirects to the Employee List.
     */
    public async Task<IActionResult> OnPostAsync()
    {
        var token = HttpContext.Session.GetString("JWT");
        if (string.IsNullOrWhiteSpace(token))
            return RedirectToPage("/Login");

        var role = HttpContext.Session.GetString("SystemRole");
        if (role != "HR" && role != "ADMIN")
            return RedirectToPage("/Index");

        var client = _httpClientFactory.CreateClient();
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
        var response = await client.PostAsync(
            _config["ApiBaseUrl"] + "/api/employees",
            new StringContent(JsonSerializer.Serialize(employeeDto), Encoding.UTF8, "application/json"));

        if (!response.IsSuccessStatusCode)
        {
            return await OnGetAsync();
        }

        return RedirectToPage("/Employee/Index");
    }
}
