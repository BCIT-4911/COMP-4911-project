using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages;

public class ProjectsModel : PageModel
{
    private readonly IConfiguration _config;

    public string ApiBaseUrl { get; private set; } = "";
    public string JwtToken { get; private set; } = "";

    public ProjectsModel(IConfiguration config)
    {
        _config = config;
    }

    public IActionResult OnGet()
    {
        var token = HttpContext.Session.GetString("JWT");
        if (string.IsNullOrWhiteSpace(token))
            return RedirectToPage("/Login");

        ApiBaseUrl = _config["ApiBaseUrl"] ?? "";
        JwtToken = token;
        return Page();
    }
}
