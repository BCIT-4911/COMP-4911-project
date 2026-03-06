using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages.Timesheets
{
    public class IndexModel : PageModel
    {
        private readonly IConfiguration _config;

        public string ApiBaseUrl { get; private set; } = "";

        [BindProperty(SupportsGet = true)]
        public int? EmpId { get; set; }

        public IndexModel(IConfiguration config)
        {
            _config = config;
        }

        public void OnGet()
        {
            ApiBaseUrl = _config["ApiBaseUrl"] ?? "";
        }
    }
}
