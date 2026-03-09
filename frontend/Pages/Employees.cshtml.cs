using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages;

public class EmployeesModel : PageModel
{
    private readonly IConfiguration _config;

    public EmployeesModel(IConfiguration config)
    {
        _config = config;
    }

    public IActionResult OnGet()
    {
        var token = HttpContext.Session.GetString("JWT");
        if (string.IsNullOrWhiteSpace(token))
            return RedirectToPage("/Login");

        var role = HttpContext.Session.GetString("SystemRole");
        if (role != "HR")
            return RedirectToPage("/Index");

        return Page();
    }
}
