using System.ComponentModel.DataAnnotations;
using System.Text;
using System.Text.Json;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages;

public class LoginModel : PageModel
{
    private readonly IConfiguration _config;
    private readonly IHttpClientFactory _httpClientFactory;

    public LoginModel(IConfiguration config, IHttpClientFactory httpClientFactory)
    {
        _config = config;
        _httpClientFactory = httpClientFactory;
    }

    [BindProperty]
    public LoginInput Input { get; set; } = new();

    public string? ErrorMessage { get; set; }

    public async Task<IActionResult> OnPostAsync()
    {
        if (!ModelState.IsValid)
            return Page();

        var apiBaseUrl = _config["ApiBaseUrl"];

        var client = _httpClientFactory.CreateClient();

        var loginData = new
        {
            empId = Input.EmpId,
            password = Input.Password
        };

        var json = JsonSerializer.Serialize(loginData);

        var content = new StringContent(
            json,
            Encoding.UTF8,
            "application/json"
        );

        var response = await client.PostAsync(
            apiBaseUrl + "/api/auth/login",
            content
        );

        if (response.StatusCode == System.Net.HttpStatusCode.Unauthorized)
        {
            ErrorMessage = "Invalid credentials.";
            return Page();
        }

        if (!response.IsSuccessStatusCode)
        {
            ErrorMessage = "Login service unavailable.";
            return Page();
        }

        var body = await response.Content.ReadAsStringAsync();

        var result = JsonSerializer.Deserialize<LoginResponseDto>(
            body,
            new JsonSerializerOptions
            {
                PropertyNameCaseInsensitive = true
            }
        );

        if (result == null || string.IsNullOrEmpty(result.Token))
        {
            ErrorMessage = "Invalid login response.";
            return Page();
        }

        HttpContext.Session.SetString("JWT", result.Token);

        var parts = result.Token.Split('.');
        if (parts.Length >= 2)
        {
            var payloadBase64 = parts[1].Replace('-', '+').Replace('_', '/');
            switch (payloadBase64.Length % 4)
            {
                case 2: payloadBase64 += "=="; break;
                case 3: payloadBase64 += "="; break;
            }
            try
            {
                var payloadBytes = Convert.FromBase64String(payloadBase64);
                var payload = JsonSerializer.Deserialize<JsonElement>(
                    Encoding.UTF8.GetString(payloadBytes));
                var role = payload.TryGetProperty("systemRole", out var roleProp)
                    ? roleProp.GetString() ?? "EMPLOYEE"
                    : "EMPLOYEE";
                var empId = payload.TryGetProperty("empId", out var empIdProp)
                    ? empIdProp.GetRawText()
                    : "";
                HttpContext.Session.SetString("SystemRole", role);
                HttpContext.Session.SetString("EmpId", empId);
            }
            catch { /* ignore decode errors, role/nav will fallback */ }
        }

        return RedirectToPage("/Index");
    }

    public class LoginInput
    {
        [Required]
        public int EmpId { get; set; }

        [Required]
        public string Password { get; set; } = "";
    }

    public class LoginResponseDto
    {
        public string Token { get; set; } = "";
    }
}
