using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using System.Net.Http.Headers;
using System.Text.Json;

namespace frontend.Pages.Approver
{
    public class ReviewModel : PageModel
    {
        private readonly IConfiguration _config;
        private readonly IHttpClientFactory _httpClientFactory;

        public string ApiBaseUrl { get; private set; } = "";
        public string JwtToken { get; private set; } = "";

        [BindProperty(SupportsGet = true)]
        public int Id { get; set; }

        [BindProperty(SupportsGet = true)]
        public int? ApproverId { get; set; }

        public ReviewModel(IConfiguration config, IHttpClientFactory httpClientFactory)
        {
            _config = config;
            _httpClientFactory = httpClientFactory;
        }

        public async Task<IActionResult> OnGetAsync()
        {
            ApiBaseUrl = _config["ApiBaseUrl"] ?? "";

            var token = HttpContext.Session.GetString("JWT");
            if (string.IsNullOrWhiteSpace(token))
            {
                return RedirectToPage("/Login");
            }

            JwtToken = token;

            var http = _httpClientFactory.CreateClient();
            http.DefaultRequestHeaders.Authorization =
                new AuthenticationHeaderValue("Bearer", token);

            var response = await http.GetAsync(ApiBaseUrl + "/api/auth/can-access-approver-dashboard");
            if (!response.IsSuccessStatusCode)
            {
                return RedirectToPage("/Index");
            }

            var body = await response.Content.ReadAsStringAsync();
            using var json = JsonDocument.Parse(body);
            bool allowed = json.RootElement.TryGetProperty("allowed", out var allowedProp)
                && allowedProp.GetBoolean();

            if (!allowed)
            {
                return RedirectToPage("/Index");
            }

            if (!ApproverId.HasValue)
            {
                var empId = HttpContext.Session.GetString("EmpId");
                if (int.TryParse(empId, out var parsedEmpId))
                {
                    ApproverId = parsedEmpId;
                }
            }

            return Page();
        }
    }
}