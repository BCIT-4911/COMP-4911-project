using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages;

public class EmployeesModel : PageModel
{
    private readonly IConfiguration _config;
    private readonly IHttpClientFactory _httpClientFactory;

    public string? ApiBaseUrl { get; private set; }
    public string? JwtToken { get; private set; }

    public EmployeesModel(IConfiguration config, IHttpClientFactory httpClientFactory)
    {
        _config = config;
        _httpClientFactory = httpClientFactory;
    }

    public IActionResult OnGet()
    {
        var token = HttpContext.Session.GetString("JWT");
        if (string.IsNullOrWhiteSpace(token))
            return RedirectToPage("/Login");

        var role = HttpContext.Session.GetString("SystemRole");
        if (role != "HR")
            return RedirectToPage("/Index");

        ApiBaseUrl = _config["ApiBaseUrl"] ?? "";
        JwtToken = token;
        return Page();
    }
}
