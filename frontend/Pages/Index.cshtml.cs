using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages;

public class IndexModel : PageModel
{
    private readonly IConfiguration _config;

    public string? HelloFromApi { get; private set; }

    public IndexModel(IConfiguration config)
    {
        _config = config;
    }

    public async Task OnGetAsync()
    {
        var apiBaseUrl = _config["ApiBaseUrl"];

        using var client = new HttpClient();

        HelloFromApi = await client.GetStringAsync(apiBaseUrl + "/api/greet");
    }
}
