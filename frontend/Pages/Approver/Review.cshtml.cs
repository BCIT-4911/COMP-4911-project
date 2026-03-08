using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages.Approver
{
    public class ReviewModel : PageModel
    {
        private readonly IConfiguration _config;

        public string ApiBaseUrl { get; private set; } = "";

        [BindProperty(SupportsGet = true)]
        public int Id { get; set; }

        [BindProperty(SupportsGet = true)]
        public int? ApproverId { get; set; }

        public ReviewModel(IConfiguration config)
        {
            _config = config;
        }

        public void OnGet()
        {
            ApiBaseUrl = _config["ApiBaseUrl"] ?? "";
        }
    }
}