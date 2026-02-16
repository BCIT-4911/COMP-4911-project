using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages;

public class ProjectsModel : PageModel
{
    private readonly IConfiguration _config;

    public string ApiBaseUrl { get; private set; } = "";

    public ProjectsModel(IConfiguration config)
    {
        _config = config;
    }

    public void OnGet()
    {
        ApiBaseUrl = _config["ApiBaseUrl"] ?? "";
    }
}
