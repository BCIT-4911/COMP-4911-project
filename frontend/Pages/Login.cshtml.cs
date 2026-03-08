using System.ComponentModel.DataAnnotations;
using System.Text;
using System.Text.Json;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages;

public class LoginModel : PageModel
{
    private readonly IConfiguration _config;

    public LoginModel(IConfiguration config)
    {
        _config = config;
    }

    [BindProperty]
    public LoginInput Input { get; set; } = new();

    public string? ErrorMessage { get; set; }

    public async Task<IActionResult> OnPostAsync()
    {
        if (!ModelState.IsValid)
            return Page();

        var apiBaseUrl = _config["ApiBaseUrl"];

        using var client = new HttpClient();

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