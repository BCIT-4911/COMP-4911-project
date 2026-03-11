<<<<<<< HEAD
=======
using Microsoft.AspNetCore.Mvc;
>>>>>>> 4c14e097edde2f44752d0f5b5880dd18620fdc3c
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages;

public class EmployeesModel : PageModel
{
    private readonly IConfiguration _config;

<<<<<<< HEAD
    public string ApiBaseUrl { get; private set; } = "";

=======
>>>>>>> 4c14e097edde2f44752d0f5b5880dd18620fdc3c
    public EmployeesModel(IConfiguration config)
    {
        _config = config;
    }

<<<<<<< HEAD
    public void OnGet()
    {
        ApiBaseUrl = _config["ApiBaseUrl"] ?? "";
=======
    public IActionResult OnGet()
    {
        var token = HttpContext.Session.GetString("JWT");
        if (string.IsNullOrWhiteSpace(token))
            return RedirectToPage("/Login");

        var role = HttpContext.Session.GetString("SystemRole");
        if (role != "HR")
            return RedirectToPage("/Index");

        return Page();
>>>>>>> 4c14e097edde2f44752d0f5b5880dd18620fdc3c
    }
}
