using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages;

public class WorkPackagesModel : PageModel
{
    private readonly IConfiguration _config;

    public string ApiBaseUrl { get; private set; } = "";

    [BindProperty(SupportsGet = true)]
    public string ProjectId { get; set; } = "";

    public WorkPackagesModel(IConfiguration config)
    {
        _config = config;
    }

    public void OnGet()
    {
        ApiBaseUrl = _config["ApiBaseUrl"] ?? "";
    }
}
