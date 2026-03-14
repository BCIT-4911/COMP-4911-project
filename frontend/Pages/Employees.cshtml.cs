using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using frontend.model;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages;

/**
 * Page model for the Employees page.
 * Provides methods for retrieving and creating employee records.
 */
public class EmployeesModel : PageModel
{
    private readonly IConfiguration _config;
    private readonly IHttpClientFactory _httpClientFactory;

    [BindProperty]
    public EmployeeDto employeeDto { get; set; } = new();

    public EmployeesModel(IConfiguration config, IHttpClientFactory httpClientFactory)
    {
        _config = config;
        _httpClientFactory = httpClientFactory;
    }

    /**
     * Handles GET requests to the Employees page.
     * Retrieves all employees and labor grades from the API.
     * @return The page to display.
     */
    public async Task<IActionResult> OnGetAsync()
    {
        var token = HttpContext.Session.GetString("JWT");
        if (string.IsNullOrWhiteSpace(token))
            return RedirectToPage("/Login");

        var role = HttpContext.Session.GetString("SystemRole");
        if (role != "HR")
            return RedirectToPage("/Index");

        var client = _httpClientFactory.CreateClient();
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
        var employeeResponse = await client.GetAsync(_config["ApiBaseUrl"] + "/api/employees");
        var laborGradeResponse = await client.GetAsync(_config["ApiBaseUrl"] + "/api/labor-grades");
        if (!employeeResponse.IsSuccessStatusCode)
            return Page();
        var employeeContent = await employeeResponse.Content.ReadAsStringAsync();
        var laborGradeContent = await laborGradeResponse.Content.ReadAsStringAsync();
        var laborGrades = JsonSerializer.Deserialize<List<LaborGrade>>(laborGradeContent);
        var employees = JsonSerializer.Deserialize<List<Employee>>(employeeContent);
        ViewData["Employees"] = employees;
        ViewData["LaborGrades"] = laborGrades;

        return Page();
    }

    /**
     * Handles POST requests to the Employees page.
     * Creates a new employee using the provided data.
     * Returns the page to display.
     */
    public async Task<IActionResult> OnPostAsync()
    {
        var token = HttpContext.Session.GetString("JWT");
        if (string.IsNullOrWhiteSpace(token))
            return RedirectToPage("/Login");

        var role = HttpContext.Session.GetString("SystemRole");
        if (role != "HR")
            return RedirectToPage("/Index");

        var client = _httpClientFactory.CreateClient();
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
        var response = await client.PostAsync(_config["ApiBaseUrl"] + "/api/employees", new StringContent(JsonSerializer.Serialize(employeeDto), Encoding.UTF8, "application/json"));
        if (!response.IsSuccessStatusCode)
            return Page();
        return RedirectToPage("/Employees");
    }
}
