using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages;

public class EmployeesModel : PageModel
{
    private readonly IConfiguration _config;

    public string ApiBaseUrl { get; private set; } = "";

    public EmployeesModel(IConfiguration config)
    {
        _config = config;
    }

    public void OnGet()
    {
        ApiBaseUrl = _config["ApiBaseUrl"] ?? "";
    }
}
