using Microsoft.AspNetCore.Mvc.RazorPages;

namespace frontend.Pages.EarnedValue;

public class IndexModel : PageModel
{
    public int MonthCount { get; set; } = 6;

    // Filters (stubbed)
    public int SelectedProjectId { get; set; } = 1;
    public int SelectedControlAccountId { get; set; } = 1;

    public List<Option> Projects { get; set; } = new();
    public List<Option> ControlAccounts { get; set; } = new();

    // Header info (stubbed)
    public string ControlAccountTitle { get; set; } = "Roadrunner";
    public string ControlAccountManager { get; set; } = "Wile E. Coyote";
    public decimal BAC { get; set; } = 10000;

    public List<WorkPackageVm> WorkPackages { get; set; } = new();

    public Dictionary<int, decimal> TotalBCWSByMonth { get; set; } = new();
    public Dictionary<int, decimal> TotalBCWPByMonth { get; set; } = new();
    public Dictionary<int, decimal> SVByMonth { get; set; } = new();
    public Dictionary<int, decimal> CVByMonth { get; set; } = new();

    public void OnGet()
    {
        Projects = new() { new(1, "Demo Project") };
        ControlAccounts = new() { new(1, "Control Account A") };

        // Demo data (replace with real backend later)
        WorkPackages = new List<WorkPackageVm>
        {
            new(1,"Procure Anvil","0/100")
            {
                BCWS = new() { [5]=1500 },
                BCWP = new() { } // none earned yet
            },
            new(2,"Paint Fake Tunnel","50/50")
            {
                BCWS = new() { [3]=500, [4]=500 },
                BCWP = new() { [4]=500 } // example earned
            },
            new(3,"Build Road","Units Complete")
            {
                BCWS = new() { [1]=600,[2]=600,[3]=600,[4]=600,[5]=600 },
                BCWP = new() { [1]=600,[2]=600,[3]=300 } // example partial earned
            },
            new(4,"Build ASM","Milestones")
            {
                BCWS = new() { [2]=1000,[3]=1000,[4]=1000 },
                BCWP = new() { [2]=1000 } // first milestone achieved
            },
            new(5,"Install ASM","% Complete")
            {
                BCWS = new() { [4]=500,[5]=500,[6]=500 },
                BCWP = new() { [4]=250 } // 50% of first slice
            },
        };

        // Totals by month
        for (int m = 1; m <= MonthCount; m++)
        {
            TotalBCWSByMonth[m] = WorkPackages.Sum(wp => wp.BCWS.TryGetValue(m, out var v) ? v : 0);
            TotalBCWPByMonth[m] = WorkPackages.Sum(wp => wp.BCWP.TryGetValue(m, out var v) ? v : 0);

            SVByMonth[m] = TotalBCWPByMonth[m] - TotalBCWSByMonth[m];

            // CV placeholder (ACWP not wired yet)
            var acwp = 0m;
            CVByMonth[m] = TotalBCWPByMonth[m] - acwp;
        }
    }

    public record Option(int Id, string Name);

    public class WorkPackageVm
    {
        public int Number { get; }
        public string Description { get; }
        public string EvMethod { get; }

        public Dictionary<int, decimal> BCWS { get; set; } = new();
        public Dictionary<int, decimal> BCWP { get; set; } = new();

        public WorkPackageVm(int number, string desc, string evMethod)
        {
            Number = number;
            Description = desc;
            EvMethod = evMethod;
        }
    }
}