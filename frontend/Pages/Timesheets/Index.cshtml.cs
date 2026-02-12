using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages.Timesheets
{
    public class IndexModel : PageModel
    {
        public string EmployeeName { get; set; } = "Project Manager";
        public DateTime WeekStart { get; set; } =
            DateTime.Today.AddDays(-(int)DateTime.Today.DayOfWeek); // Sunday

        [TempData]
        public string? FlashMessage { get; set; }

        [TempData]
        public string Status { get; set; } = "Draft";

        public List<WorkPackageOption> WorkPackages { get; set; } = new();
        public List<string> LaborGrades { get; set; } = new() { "Junior", "Intermediate", "Senior" };

        [BindProperty]
        public List<TimesheetRowVm> Rows { get; set; } = new();

        public void OnGet()
        {
            SeedDropdowns();

            if (Rows.Count == 0)
            {
                Rows.Add(new TimesheetRowVm
                {
                    Date = DateTime.Today,
                    Hours = 0,
                    LaborGrade = "Junior",
                    Notes = ""
                });
            }
        }

        public IActionResult OnPostAddRow()
        {
            SeedDropdowns();
            Rows.Add(new TimesheetRowVm { Date = DateTime.Today, LaborGrade = "Junior" });
            return Page();
        }

        public IActionResult OnPostDeleteRow(int rowIndex)
        {
            SeedDropdowns();
            if (rowIndex >= 0 && rowIndex < Rows.Count)
                Rows.RemoveAt(rowIndex);

            return Page();
        }

        public IActionResult OnPostSave()
        {
            SeedDropdowns();
            FlashMessage = "Saved as draft.";
            Status = "Draft";
            return Page();
        }

        public IActionResult OnPostSubmit()
        {
            SeedDropdowns();

            if (Rows.Any(r => r.WorkPackageId == null || r.WorkPackageId <= 0))
            {
                FlashMessage = "Please select a Work Package for each row before submitting.";
                return Page();
            }

            FlashMessage = "Submitted successfully!";
            Status = "Submitted";
            return Page();
        }

        private void SeedDropdowns()
        {
            WorkPackages = new List<WorkPackageOption>
            {
                new(1, "WP-1 Procure Anvil"),
                new(2, "WP-2 Paint Fake Tunnel"),
                new(3, "WP-3 Build Road"),
                new(4, "WP-4 Build ASM"),
                new(5, "WP-5 Install ASM"),
            };
        }

        public record WorkPackageOption(int Id, string Name);

        public class TimesheetRowVm
        {
            public DateTime Date { get; set; }
            public int? WorkPackageId { get; set; }
            public decimal Hours { get; set; }
            public string LaborGrade { get; set; } = "Junior";
            public string? Notes { get; set; }
        }
    }
}