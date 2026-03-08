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

    public async Task<IActionResult> OnGetAsync()
    {
        var token = HttpContext.Session.GetString("JWT");

        if (string.IsNullOrWhiteSpace(token))
        {
            return RedirectToPage("/Login");
        }

        var apiBaseUrl = _config["ApiBaseUrl"];

        using var client = new HttpClient();

        client.DefaultRequestHeaders.Authorization =
            new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", token);

        var response = await client.GetAsync(apiBaseUrl + "/api/greet");

        if (response.StatusCode == System.Net.HttpStatusCode.Unauthorized)
        {
            HttpContext.Session.Clear();
            return RedirectToPage("/Login");
        }

        HelloFromApi = await response.Content.ReadAsStringAsync();

        return Page();
    }
}
