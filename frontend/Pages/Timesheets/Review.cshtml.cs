using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages.Timesheets
{
    public class ReviewModel : PageModel
    {
        private readonly IConfiguration _config;

        public string ApiBaseUrl { get; private set; } = "";
        public string JwtToken { get; private set; } = "";

        [BindProperty(SupportsGet = true)]
        public int Id { get; set; }

        public ReviewModel(IConfiguration config)
        {
            _config = config;
        }

        public IActionResult OnGet()
        {
            var token = HttpContext.Session.GetString("JWT");
            if (string.IsNullOrWhiteSpace(token))
                return RedirectToPage("/Login");

            if (Id <= 0)
                return RedirectToPage("/Timesheets/Index");

            ApiBaseUrl = _config["ApiBaseUrl"] ?? "";
            JwtToken = token;
            return Page();
        }
    }
}
