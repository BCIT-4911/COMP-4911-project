# What’s in the demo database

Loaded at startup by [`EmptyDbSeeder.java`](../backend/src/main/java/com/corejsf/seed/EmptyDbSeeder.java): if work package **`A`** is missing, it inserts the base dataset, then **`seedCrossReportDemoData()`** always runs and ensures projects, work packages, assignments, and timesheets match the demo (including **`LABOR-1`** and cross-report rows).

Everyone can use password **`password`** unless you’ve changed it.

---

## What `EmptyDbSeeder` actually seeds

### Labor grades

- **E1** 80.00, **E2** 90.00, **E3** 96.00, **M1** 118.00, **M2** 132.00, **M3** 148.00, **X1** 170.00, **X2** 185.00  
- **RateHistory** (only): **E1** 75.00 (2025-01-01–2025-12-31), 80.00 (2026-01-01–open); **M1** 108.00 (2025-01-01–2025-12-31), 118.00 (2026-01-01–open)

### Employees (each has own `EmployeeESignature`, vacation/sick **40.00**, expected weekly hours **40.0**)

| Name | System role | Supervisor | Labor grade |
|------|-------------|------------|-------------|
| Wile Coyote | ADMIN | — | X2 |
| Elmer Fudd | OPERATIONS_MANAGER | — | M3 |
| Road Runner | HR | Wile | X1 |
| Bugs Bunny | EMPLOYEE | Elmer | E2 |
| Daffy Duck | EMPLOYEE | Bugs | E2 |
| Tweety Bird | EMPLOYEE | Bugs | E1 |
| Sylvester Cat | EMPLOYEE | Bugs | E3 |
| Marvin Martian | EMPLOYEE | Elmer | M1 |
| Marcus Aurelius | EMPLOYEE | Wile | E2 |
| Elena Fisher | EMPLOYEE | Wile | M1 |
| James Holden | EMPLOYEE | Wile | M2 |
| Sarah Connor | EMPLOYEE | Wile | E3 |

On a **fresh** DB, **Wile** is typically **emp id 1**.

### Projects

| ID | Name | Type | PM | Dates | Markup | BAC |
|----|------|------|-----|-------|--------|-----|
| PROJ-1 | Demo Project | INTERNAL | Bugs Bunny | 2026-01-01 → 2026-04-30 | 10% | 10,000 |
| PROJ-2 | Demo External Project | EXTERNAL | Marvin Martian | 2026-01-03 → 2026-04-30 | 10% | 15,000 |
| LABOR-1 | Labor Report Demo | INTERNAL | Wile Coyote | 2023-10-01 → 2026-06-30 | 10% | 250,000 |

All three are **OPEN**. Descriptions: **PROJ-1** — “Seed data for Earned Value report”; **PROJ-2** — “Seed Data for Earned Value Report”; **LABOR-1** — “Screenshot-aligned labor report demo dataset”. Created/modified timestamps use “now” where the seeder sets them.

### Work packages

**PROJ-1**

- **A** — summary, RE **Elmer**, plan 2026-01-01–2026-04-30, BAC 5000, % complete 25  
- **A.WP-1** — leaf, RE **Daffy**, 2026-01-01–2026-01-31, BAC 1500, % 15  
- **A.WP-2** — leaf, RE **Sylvester**, 2026-02-01–2026-02-14, BAC 1000, % 50  
- **A.WP-3** — leaf, RE **Elmer**, 2026-01-15–2026-04-15, BAC 2500, % 35  
- **C** — summary, RE **Elmer**, plan 2026-02-01–2026-04-30, BAC 4000, % 20  
- **C.WP-1** — leaf, RE **Bugs**, 2026-02-01–2026-03-31, BAC 1200, % 10  
- **C.WP-2** — leaf, RE **Elmer**, 2026-03-01–2026-04-20, BAC 900, % 5  

**PROJ-2**

- **B** — summary, RE **Marvin**, 2026-01-03–2026-04-30, BAC 6000, % 40  
- **B.WP-1** — leaf, RE **Marvin**, 2026-01-03–2026-01-31, BAC 1800, % 100  
- **B.WP-2** — leaf, RE **Road**, 2026-02-01–2026-02-20, BAC 2200, % 65  
- **B.WP-3** — leaf, RE **Marvin**, 2026-02-21–2026-04-30, BAC 2000, % 25  

**LABOR-1** (all lowest-level, no parent summary in seed)

- **LABOR-1.WP-1** Foundations — Section A, RE **Marcus**, plan 2026-01-01–2026-02-15, BAC 55,000, % 42  
- **LABOR-1.WP-2** Electrical Wiring — Level 4, RE **Elena**, 2026-01-10–2026-02-28, BAC 70,000, % 58  
- **LABOR-1.WP-3** HVAC Duct Installation, RE **James**, 2026-02-01–2026-03-15, BAC 60,000, % 47  
- **LABOR-1.WP-4** Plumbing — Master Bath, RE **Sarah**, 2026-02-10–2026-03-20, BAC 65,000, % 63  

(Initial create for LABOR-1 leaves also used 2023-10 plan window and BAC 10,000 until `updateWorkPackageForEarnedValue` runs.)

### Project assignments

- **PROJ-1:** Elmer MEMBER, Bugs PM, Daffy MEMBER, Tweety MEMBER, Sylvester MEMBER  
- **PROJ-2:** Marvin PM, Road MEMBER  
- **LABOR-1:** Marcus, Elena, James, Sarah MEMBER  

### Work package assignments

- **PROJ-1:** **A** & **C** — Elmer RE; **A.WP-1** — Daffy RE, Bugs RE; **A.WP-2** — Sylvester RE, Tweety MEMBER; **A.WP-3** — Elmer RE; **C.WP-1** — Bugs RE; **C.WP-2** — Elmer RE  
- **PROJ-2:** **B.WP-1** — Marvin RE; **B.WP-2** — Road RE; **B.WP-3** — Marvin MEMBER  
- **LABOR-1:** each of **LABOR-1.WP-1…4** — Marcus / Elena / James / Sarah as **MEMBER** respectively  

Seeder deletes **Tweety MEMBER** on **C.WP-2** if it exists (legacy cleanup).

### Timesheets

Each seeded timesheet has **one row**; all hours are on **Monday** only.

**PROJ-1**

| Employee | Week ending | Status | Approver | WP | Grade | Mon hours |
|----------|-------------|--------|----------|-----|-------|-----------|
| Daffy | 2026-04-11 | SUBMITTED | Bugs | A.WP-1 | E2 | 32.0 |
| Daffy | 2026-04-04 | APPROVED | Wile | A.WP-1 | E2 | 24.0 |
| Sylvester | 2026-04-11 | APPROVED | Wile | A.WP-2 | E3 | 28.0 |
| Tweety | 2026-04-11 | SUBMITTED | Bugs | A.WP-2 | E1 | 18.0 |
| Tweety | 2026-04-18 | DRAFT | — | A.WP-2 | E1 | 8.0 |

**PROJ-2**

| Employee | Week ending | Status | Approver | WP | Grade | Mon hours |
|----------|-------------|--------|----------|-----|-------|-----------|
| Marvin | 2026-04-11 | APPROVED | Wile | B.WP-1 | M1 | 14.0 |
| Road | 2026-04-11 | APPROVED | Wile | B.WP-2 | X1 | 22.0 |
| Marvin | 2026-04-25 | SUBMITTED | Elmer | B.WP-3 | M1 | 10.0 |
| Road | 2026-04-04 | RETURNED | Wile | B.WP-2 | X1 | 6.0 |
| Marvin | 2026-04-18 | DRAFT | — | B.WP-1 | M1 | 4.0 |

**RETURNED** rows get return comment: `Demo seed: please correct and resubmit.`

**LABOR-1 — historical week 2023-10-27**

| Employee | Status | Approver | WP | Grade | Mon hours |
|----------|--------|----------|-----|-------|-----------|
| Marcus | APPROVED | Wile | LABOR-1.WP-1 | E2 | 40.0 |
| Elena | SUBMITTED | Wile | LABOR-1.WP-2 | M1 | 45.5 |
| James | APPROVED | Wile | LABOR-1.WP-3 | M2 | 38.0 |
| Sarah | APPROVED | Wile | LABOR-1.WP-4 | E3 | 42.0 |

**LABOR-1 — week 2026-04-11**

| Employee | Status | Approver | WP | Grade | Mon hours |
|----------|--------|----------|-----|-------|-----------|
| Marcus | APPROVED | Wile | LABOR-1.WP-1 | E2 | 36.0 |
| Elena | APPROVED | Wile | LABOR-1.WP-2 | M1 | 30.0 |
| James | SUBMITTED | Wile | LABOR-1.WP-3 | M2 | 34.0 |
| Sarah | APPROVED | Wile | LABOR-1.WP-4 | E3 | 41.0 |


