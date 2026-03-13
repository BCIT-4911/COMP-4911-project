using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages;

public class IndexModel : PageModel
{
    private readonly IConfiguration _config;
    private readonly IHttpClientFactory _httpClientFactory;

    public string? HelloFromApi { get; private set; }

    public IndexModel(IConfiguration config, IHttpClientFactory httpClientFactory)
    {
        _config = config;
        _httpClientFactory = httpClientFactory;
    }

    public async Task<IActionResult> OnGetAsync()
    {
        var token = HttpContext.Session.GetString("JWT");

        if (string.IsNullOrWhiteSpace(token))
        {
            return RedirectToPage("/Login");
        }

        var apiBaseUrl = _config["ApiBaseUrl"];

        var client = _httpClientFactory.CreateClient();
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
