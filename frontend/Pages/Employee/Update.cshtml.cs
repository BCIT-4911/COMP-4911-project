using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using frontend.model;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages.Employee;

/**
 * Page model for the Update Employee page.
 * Provides methods for retrieving and updating an employee record.
 */
public class UpdateModel : PageModel
{
    private readonly IConfiguration _config;
    private readonly IHttpClientFactory _httpClientFactory;

    [BindProperty]
    public EmployeeDto employeeDto { get; set; } = new();

    public int EmpId { get; set; }

    public string? ErrorMessage { get; set; }

    public UpdateModel(IConfiguration config, IHttpClientFactory httpClientFactory)
    {
        _config = config;
        _httpClientFactory = httpClientFactory;
    }

    /**
     * Handles GET requests to the Update Employee page.
     * Retrieves the employee by ID and populates the form.
     */
    public async Task<IActionResult> OnGetAsync(int id)
    {
        var token = HttpContext.Session.GetString("JWT");
        if (string.IsNullOrWhiteSpace(token))
            return RedirectToPage("/Login");

        var role = HttpContext.Session.GetString("SystemRole");
        if (role != "HR" && role != "ADMIN")
            return RedirectToPage("/Index");

        EmpId = id;

        var client = _httpClientFactory.CreateClient();
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);

        var empResponse = await client.GetAsync(_config["ApiBaseUrl"] + $"/api/employees/{id}");
        if (empResponse.IsSuccessStatusCode)
        {
            var empContent = await empResponse.Content.ReadAsStringAsync();
            var employee = JsonSerializer.Deserialize<model.Employee>(empContent);
            if (employee != null)
            {
                employeeDto.firstName = employee.empFirstName;
                employeeDto.lastName = employee.empLastName;
                employeeDto.systemRole = employee.systemRole;
                employeeDto.supervisorId = employee.supervisorId ?? 0;
                employeeDto.laborGradeId = employee.laborGradeId ?? 0;
            }
        }

        var employeeResponse = await client.GetAsync(_config["ApiBaseUrl"] + "/api/employees");
        if (employeeResponse.IsSuccessStatusCode)
        {
            var employeeContent = await employeeResponse.Content.ReadAsStringAsync();
            ViewData["Employees"] = JsonSerializer.Deserialize<List<model.Employee>>(employeeContent);
        }
        else
        {
            ViewData["Employees"] = new List<model.Employee>();
        }

        var laborGradeResponse = await client.GetAsync(_config["ApiBaseUrl"] + "/api/labor-grades");
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
     * Handles POST requests to update an existing employee.
     * Sends updated employee data to the API and redirects to the Employee List.
     */
    public async Task<IActionResult> OnPostAsync(int id)
    {
        var token = HttpContext.Session.GetString("JWT");
        if (string.IsNullOrWhiteSpace(token))
            return RedirectToPage("/Login");

        var role = HttpContext.Session.GetString("SystemRole");
        if (role != "HR" && role != "ADMIN")
            return RedirectToPage("/Index");

        if (string.IsNullOrWhiteSpace(employeeDto.password))
            employeeDto.password = null;

        var client = _httpClientFactory.CreateClient();
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
        var response = await client.PutAsync(
            _config["ApiBaseUrl"] + $"/api/employees/{id}",
            new StringContent(JsonSerializer.Serialize(employeeDto), Encoding.UTF8, "application/json"));

        if (!response.IsSuccessStatusCode)
        {
            var errorBody = await response.Content.ReadAsStringAsync();
            ErrorMessage = string.IsNullOrWhiteSpace(errorBody)
                ? "Failed to update employee."
                : errorBody;
            EmpId = id;
            return await OnGetAsync(id);
        }

        TempData["UpdateSuccess"] = $"Updated employee (ID {id})";
        return RedirectToPage("/Employee/Index");
    }
}
