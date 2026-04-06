using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using System.Collections.Generic;
using System.Net.Http.Headers;
using System.Text.Json;

namespace frontend.Pages.Approver
{
    public class IndexModel : PageModel
    {
        private readonly IConfiguration _config;
        private readonly IHttpClientFactory _httpClientFactory;

        public string ApiBaseUrl { get; private set; } = "";
        public string JwtToken { get; private set; } = "";
        public List<int> DirectReportIds { get; private set; } = new();

        [BindProperty(SupportsGet = true)]
        public int? SelectedEmployeeId { get; set; }

        public IndexModel(IConfiguration config, IHttpClientFactory httpClientFactory)
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

            var reportsResponse = await http.GetAsync(ApiBaseUrl + "/api/auth/direct-reports");
            if (!reportsResponse.IsSuccessStatusCode)
            {
                return RedirectToPage("/Index");
            }

            var reportsBody = await reportsResponse.Content.ReadAsStringAsync();
            using var reportsJson = JsonDocument.Parse(reportsBody);
            if (reportsJson.RootElement.TryGetProperty("employeeIds", out var idsElement)
                && idsElement.ValueKind == JsonValueKind.Array)
            {
                foreach (var element in idsElement.EnumerateArray())
                {
                    if (element.TryGetInt32(out var id))
                    {
                        DirectReportIds.Add(id);
                    }
                }
            }

            if (DirectReportIds.Count == 0)
            {
                return RedirectToPage("/Index");
            }

            if (!SelectedEmployeeId.HasValue || !DirectReportIds.Contains(SelectedEmployeeId.Value))
            {
                SelectedEmployeeId = DirectReportIds[0];
            }

            return Page();
        }
    }
}