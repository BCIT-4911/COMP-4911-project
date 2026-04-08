package com.corejsf.seed;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

import com.corejsf.Entity.Employee;
import com.corejsf.Entity.EmployeeESignature;
import com.corejsf.Entity.LaborGrade;
import com.corejsf.Entity.Project;
import com.corejsf.Entity.ProjectAssignment;
import com.corejsf.Entity.ProjectRole;
import com.corejsf.Entity.ProjectStatus;
import com.corejsf.Entity.ProjectType;
import com.corejsf.Entity.RateHistory;
import com.corejsf.Entity.SystemRole;
import com.corejsf.Entity.Timesheet;
import com.corejsf.Entity.TimesheetRow;
import com.corejsf.Entity.TimesheetStatus;
import com.corejsf.Entity.WorkPackage;
import com.corejsf.Entity.WorkPackageAssignment;
import com.corejsf.Entity.WorkPackageStatus;
import com.corejsf.Entity.WorkPackageType;
import com.corejsf.Entity.WpRole;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Startup
@Singleton
public class EmptyDbSeeder {

    private static final LocalDate DEMO_WEEK_ENDING = LocalDate.of(2026, 4, 11);
    private static final LocalDate DEMO_PRIOR_WEEK = LocalDate.of(2026, 4, 4);
    /** Extra week for seeded DRAFT timesheet (dashboard demo). */
    private static final LocalDate DEMO_DRAFT_WEEK = LocalDate.of(2026, 4, 18);
    /** Second week on PROJ-2 so Marvin can have SUBMITTED (wp3) without colliding with APPROVED (wp1) same timesheet. */
    private static final LocalDate DEMO_SECOND_WEEK = LocalDate.of(2026, 4, 25);
    private static final LocalDate PROJ1_PLAN_END = LocalDate.of(2026, 4, 30);
    private static final LocalDate PROJ2_PLAN_END = LocalDate.of(2026, 4, 30);
    private static final LocalDate LABOR1_PROJECT_END = LocalDate.of(2026, 6, 30);

    private record DemoLaborGrades(
            LaborGrade e1,
            LaborGrade e2,
            LaborGrade e3,
            LaborGrade m1,
            LaborGrade m2,
            LaborGrade m3,
            LaborGrade x1,
            LaborGrade x2) {
    }

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    @PostConstruct
    public void seed() {
        boolean hasBaseSeed = em.find(WorkPackage.class, "A") != null;
        if (!hasBaseSeed) {
            System.out.println("[Seeder] Empty DB detected. Seeding minimum dataset...");

            DemoLaborGrades grades = ensureDemoLaborGrades();

            EmployeeESignature sig = em.find(EmployeeESignature.class, 1);
            if (sig == null) {
                sig = new EmployeeESignature();
                sig.setSignatureData(new byte[] { 0x00 });
                sig.setSignedAt(LocalDateTime.now());
                em.persist(sig);
            }

            Employee admin = new Employee();
            admin.setEmpFirstName("Wile");
            admin.setEmpLastName("Coyote");
            admin.setEmpPassword(BCrypt.hashpw("password", BCrypt.gensalt()));
            admin.setSystemRole(SystemRole.ADMIN);
            admin.setESignature(sig);
            admin.setLaborGrade(grades.x2());
            admin.setSupervisor(null);
            admin.setVacationSickBalance(new BigDecimal("40.00"));
            admin.setExpectedWeeklyHours(new BigDecimal("40.0"));
            em.persist(admin);

            Employee elmerFudd = createEmployee("Elmer", "Fudd", SystemRole.OPERATIONS_MANAGER, null, grades.m3());
            Employee roadRunner = createEmployee("Road", "Runner", SystemRole.HR, admin, grades.x1());
            Employee bugsBunny = createEmployee("Bugs", "Bunny", SystemRole.EMPLOYEE, elmerFudd, grades.e2());
            Employee daffyDuck = createEmployee("Daffy", "Duck", SystemRole.EMPLOYEE, bugsBunny, grades.e2());
            Employee tweetyBird = createEmployee("Tweety", "Bird", SystemRole.EMPLOYEE, bugsBunny, grades.e1());
            Employee sylvesterCat = createEmployee("Sylvester", "Cat", SystemRole.EMPLOYEE, bugsBunny, grades.e3());
            Employee marvinMartian = createEmployee("Marvin", "Martian", SystemRole.EMPLOYEE, elmerFudd, grades.m1());

            Project proj = em.find(Project.class, "PROJ-1");
            if (proj == null) {
                proj = new Project();
                proj.setProjId("PROJ-1");
                proj.setProjType(ProjectType.INTERNAL);
                proj.setProjectManager(bugsBunny);
                proj.setProjName("Demo Project");
                proj.setDescription("Seed data for Earned Value report");
                proj.setStatus(ProjectStatus.OPEN);
                proj.setStartDate(LocalDate.of(2026, 1, 1));
                proj.setEndDate(PROJ1_PLAN_END);
                proj.setCreatedDate(LocalDateTime.now());
                proj.setModifiedDate(LocalDateTime.now());
                proj.setCreatedBy(elmerFudd);
                proj.setModifiedBy(elmerFudd);
                proj.setMarkupRate(new BigDecimal("10.00"));
                proj.setBac(new BigDecimal("10000.00"));

                em.persist(proj);
            }

            Project proj2 = em.find(Project.class, "PROJ-2");
            if (proj2 == null) {
                proj2 = new Project();
                proj2.setProjId("PROJ-2");
                proj2.setProjType(ProjectType.EXTERNAL);
                proj2.setProjectManager(marvinMartian);
                proj2.setProjName("Demo External Project");
                proj2.setDescription("Seed Data for Earned Value Report");
                proj2.setStatus(ProjectStatus.OPEN);
                proj2.setStartDate(LocalDate.of(2026, 1, 3));
                proj2.setEndDate(PROJ2_PLAN_END);
                proj2.setCreatedDate(LocalDateTime.now());
                proj2.setModifiedDate(LocalDateTime.now());
                proj2.setCreatedBy(admin);
                proj2.setModifiedBy(admin);
                proj2.setMarkupRate(new BigDecimal("10.00"));
                proj2.setBac(new BigDecimal("15000.00"));

                em.persist(proj2);
            }

            WorkPackage parent = new WorkPackage();
            parent.setWpId("A");
            parent.setWpName("Control Account A");
            parent.setDescription(" Parent summary WP used as Control Account");
            parent.setProject(proj);
            parent.setParentWorkPackage(null);

            parent.setWpType(WorkPackageType.SUMMARY);
            parent.setStatus(WorkPackageStatus.OPEN_FOR_CHARGES);

            parent.setStructureLocked(false);
            parent.setBudgetedEffort(new BigDecimal("0.00"));
            parent.setBcws(new BigDecimal("0.00"));

            parent.setPlanStartDate(LocalDate.of(2026, 1, 1));
            parent.setPlanEndDate(PROJ1_PLAN_END);

            parent.setResponsibleEmployee(elmerFudd);
            parent.setBac(new BigDecimal("5000.00"));
            parent.setPercentComplete(new BigDecimal("25.00"));

            parent.setCreatedDate(LocalDateTime.now());
            parent.setModifiedDate(LocalDateTime.now());
            parent.setCreatedBy(elmerFudd);
            parent.setModifiedBy(elmerFudd);

            em.persist(parent);

            createChild("A.WP-1", "Procure Anvil", proj, parent, daffyDuck,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                    new BigDecimal("1500.00"), new BigDecimal("0.00"));

            createChild("A.WP-2", "Paint Fake Tunnel", proj, parent, sylvesterCat,
                    LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 14),
                    new BigDecimal("1000.00"), new BigDecimal("50.00"));

            createChild("A.WP-3", "Build Road", proj, parent, elmerFudd,
                    LocalDate.of(2026, 1, 15), LocalDate.of(2026, 4, 15),
                    new BigDecimal("2500.00"), new BigDecimal("35.00"));

            WorkPackage parentC = new WorkPackage();
            parentC.setWpId("C");
            parentC.setWpName("Control Account C");
            parentC.setDescription("Second summary control account on PROJ-1");
            parentC.setProject(proj);
            parentC.setParentWorkPackage(null);
            parentC.setWpType(WorkPackageType.SUMMARY);
            parentC.setStatus(WorkPackageStatus.OPEN_FOR_CHARGES);
            parentC.setStructureLocked(false);
            parentC.setBudgetedEffort(BigDecimal.ZERO);
            parentC.setBcws(BigDecimal.ZERO);
            parentC.setPlanStartDate(LocalDate.of(2026, 2, 1));
            parentC.setPlanEndDate(PROJ1_PLAN_END);
            parentC.setResponsibleEmployee(elmerFudd);
            parentC.setBac(new BigDecimal("4000.00"));
            parentC.setPercentComplete(new BigDecimal("20.00"));
            parentC.setCreatedDate(LocalDateTime.now());
            parentC.setModifiedDate(LocalDateTime.now());
            parentC.setCreatedBy(elmerFudd);
            parentC.setModifiedBy(elmerFudd);
            em.persist(parentC);

            createChild("C.WP-1", "Integration Cutover", proj, parentC, bugsBunny,
                    LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31),
                    new BigDecimal("1200.00"), new BigDecimal("10.00"));
            createChild("C.WP-2", "Documentation & Closeout", proj, parentC, elmerFudd,
                    LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 20),
                    new BigDecimal("900.00"), new BigDecimal("5.00"));

            ProjectAssignment paElmer = new ProjectAssignment();
            paElmer.setEmployee(elmerFudd);
            paElmer.setProject(proj);
            paElmer.setAssignmentDate(LocalDate.now());
            paElmer.setProjectRole(ProjectRole.MEMBER);
            em.persist(paElmer);

            ProjectAssignment paBugs = new ProjectAssignment();
            paBugs.setEmployee(bugsBunny);
            paBugs.setProject(proj);
            paBugs.setAssignmentDate(LocalDate.now());
            paBugs.setProjectRole(ProjectRole.PM);
            em.persist(paBugs);

            proj.setProjectManager(bugsBunny);
            em.merge(proj);

            ProjectAssignment paDaffy = new ProjectAssignment();
            paDaffy.setEmployee(daffyDuck);
            paDaffy.setProject(proj);
            paDaffy.setAssignmentDate(LocalDate.now());
            paDaffy.setProjectRole(ProjectRole.MEMBER);
            em.persist(paDaffy);

            ProjectAssignment paTweety = new ProjectAssignment();
            paTweety.setEmployee(tweetyBird);
            paTweety.setProject(proj);
            paTweety.setAssignmentDate(LocalDate.now());
            paTweety.setProjectRole(ProjectRole.MEMBER);
            em.persist(paTweety);

            ProjectAssignment paSylvester = new ProjectAssignment();
            paSylvester.setEmployee(sylvesterCat);
            paSylvester.setProject(proj);
            paSylvester.setAssignmentDate(LocalDate.now());
            paSylvester.setProjectRole(ProjectRole.MEMBER);
            em.persist(paSylvester);

            ProjectAssignment paMarvin = new ProjectAssignment();
            paMarvin.setEmployee(marvinMartian);
            paMarvin.setProject(proj2);
            paMarvin.setAssignmentDate(LocalDate.now());
            paMarvin.setProjectRole(ProjectRole.PM);
            em.persist(paMarvin);

            ProjectAssignment paRoadProj2 = new ProjectAssignment();
            paRoadProj2.setEmployee(roadRunner);
            paRoadProj2.setProject(proj2);
            paRoadProj2.setAssignmentDate(LocalDate.now());
            paRoadProj2.setProjectRole(ProjectRole.MEMBER);
            em.persist(paRoadProj2);

            proj2.setProjectManager(marvinMartian);
            proj2.setProjName("Demo External Project");
            proj2.setDescription("Seed Data for Earned Value Report");
            proj2.setStatus(ProjectStatus.OPEN);
            proj2.setStartDate(LocalDate.of(2026, 1, 3));
            proj2.setEndDate(PROJ2_PLAN_END);
            proj2.setCreatedDate(LocalDateTime.now());
            proj2.setModifiedDate(LocalDateTime.now());
            proj2.setCreatedBy(admin);
            proj2.setModifiedBy(admin);
            proj2.setMarkupRate(new BigDecimal("10.00"));
            proj2.setBac(new BigDecimal("15000.00"));
            em.merge(proj2);

            WorkPackageAssignment wpaElmerParent = new WorkPackageAssignment();
            wpaElmerParent.setEmployee(elmerFudd);
            wpaElmerParent.setWorkPackage(parent);
            wpaElmerParent.setAssignmentDate(LocalDate.now());
            wpaElmerParent.setWpRole(WpRole.RE);
            em.persist(wpaElmerParent);

            WorkPackageAssignment wpaElmerParentC = new WorkPackageAssignment();
            wpaElmerParentC.setEmployee(elmerFudd);
            wpaElmerParentC.setWorkPackage(parentC);
            wpaElmerParentC.setAssignmentDate(LocalDate.now());
            wpaElmerParentC.setWpRole(WpRole.RE);
            em.persist(wpaElmerParentC);

            WorkPackage wp1 = em.find(WorkPackage.class, "A.WP-1");
            WorkPackage wp2 = em.find(WorkPackage.class, "A.WP-2");
            WorkPackage wp3 = em.find(WorkPackage.class, "A.WP-3");
            WorkPackage wpC1 = em.find(WorkPackage.class, "C.WP-1");
            WorkPackage wpC2 = em.find(WorkPackage.class, "C.WP-2");
            if (wp1 != null) {
                WorkPackageAssignment wpaDaffy = new WorkPackageAssignment();
                wpaDaffy.setEmployee(daffyDuck);
                wpaDaffy.setWorkPackage(wp1);
                wpaDaffy.setAssignmentDate(LocalDate.now());
                wpaDaffy.setWpRole(WpRole.RE);
                em.persist(wpaDaffy);
                WorkPackageAssignment wpaBugsWp1 = new WorkPackageAssignment();
                wpaBugsWp1.setEmployee(bugsBunny);
                wpaBugsWp1.setWorkPackage(wp1);
                wpaBugsWp1.setAssignmentDate(LocalDate.now());
                wpaBugsWp1.setWpRole(WpRole.RE);
                em.persist(wpaBugsWp1);
            }
            if (wp2 != null) {
                WorkPackageAssignment wpaSylvester = new WorkPackageAssignment();
                wpaSylvester.setEmployee(sylvesterCat);
                wpaSylvester.setWorkPackage(wp2);
                wpaSylvester.setAssignmentDate(LocalDate.now());
                wpaSylvester.setWpRole(WpRole.RE);
                em.persist(wpaSylvester);
            }
            if (wp2 != null) {
                WorkPackageAssignment wpaTweety = new WorkPackageAssignment();
                wpaTweety.setEmployee(tweetyBird);
                wpaTweety.setWorkPackage(wp2);
                wpaTweety.setAssignmentDate(LocalDate.now());
                wpaTweety.setWpRole(WpRole.MEMBER);
                em.persist(wpaTweety);
            }
            if (wp3 != null) {
                WorkPackageAssignment wpaElmerWp3 = new WorkPackageAssignment();
                wpaElmerWp3.setEmployee(elmerFudd);
                wpaElmerWp3.setWorkPackage(wp3);
                wpaElmerWp3.setAssignmentDate(LocalDate.now());
                wpaElmerWp3.setWpRole(WpRole.RE);
                em.persist(wpaElmerWp3);
            }
            if (wpC1 != null) {
                WorkPackageAssignment wpaBugsC1 = new WorkPackageAssignment();
                wpaBugsC1.setEmployee(bugsBunny);
                wpaBugsC1.setWorkPackage(wpC1);
                wpaBugsC1.setAssignmentDate(LocalDate.now());
                wpaBugsC1.setWpRole(WpRole.RE);
                em.persist(wpaBugsC1);
            }
            if (wpC2 != null) {
                WorkPackageAssignment wpaElmerC2 = new WorkPackageAssignment();
                wpaElmerC2.setEmployee(elmerFudd);
                wpaElmerC2.setWorkPackage(wpC2);
                wpaElmerC2.setAssignmentDate(LocalDate.now());
                wpaElmerC2.setWpRole(WpRole.RE);
                em.persist(wpaElmerC2);
            }

            System.out.println("[Seeder] Seed complete: demo labor grades, employees, PROJ-1 (A+C), PROJ-2, assignments.");
        } else {
            System.out.println("[Seeder] Base seed already present, ensuring labor report demo data.");
        }

        seedCrossReportDemoData();
    }

    private Employee createEmployee(String firstName, String lastName, SystemRole role, Employee supervisor, LaborGrade lg) {
        EmployeeESignature sig = new EmployeeESignature();
        sig.setSignatureData(new byte[] { 0x00 });
        sig.setSignedAt(LocalDateTime.now());
        em.persist(sig);

        Employee emp = new Employee();
        emp.setEmpFirstName(firstName);
        emp.setEmpLastName(lastName);
        emp.setEmpPassword(BCrypt.hashpw("password", BCrypt.gensalt()));
        emp.setSystemRole(role);
        emp.setESignature(sig);
        emp.setLaborGrade(lg);
        emp.setSupervisor(supervisor);
        emp.setVacationSickBalance(new BigDecimal("40.00"));
        emp.setExpectedWeeklyHours(new BigDecimal("40.0"));
        em.persist(emp);
        return emp;
    }

    private void createChild(
            final String wpId,
            final String name,
            final Project proj,
            final WorkPackage parent,
            final Employee emp,
            final LocalDate start,
            final LocalDate end,
            final BigDecimal bac,
            final BigDecimal percentComplete
    ) {
        WorkPackage child = new WorkPackage();
        child.setWpId(wpId);
        child.setWpName(name);
        child.setDescription(name);
        child.setProject(proj);
        child.setParentWorkPackage(parent);

        child.setWpType(WorkPackageType.LOWEST_LEVEL);
        child.setStatus(WorkPackageStatus.OPEN_FOR_CHARGES);

        child.setStructureLocked(false);
        child.setBudgetedEffort(new BigDecimal("0.00"));
        child.setBcws(new BigDecimal("0.00"));

        child.setPlanStartDate(start);
        child.setPlanEndDate(end);

        child.setResponsibleEmployee(emp);
        child.setBac(bac);
        child.setPercentComplete(percentComplete);

        child.setCreatedDate(LocalDateTime.now());
        child.setModifiedDate(LocalDateTime.now());
        child.setCreatedBy(emp);
        child.setModifiedBy(emp);

        em.persist(child);
    }

    private void createRateHistory(
            final LaborGrade laborGrade,
            final String chargeRate,
            final LocalDate startDate,
            final LocalDate endDate) {
        RateHistory rateHistory = new RateHistory();
        rateHistory.setLaborGrade(laborGrade);
        rateHistory.setChargeRate(new BigDecimal(chargeRate));
        rateHistory.setStartDate(startDate);
        rateHistory.setEndDate(endDate);
        em.persist(rateHistory);
    }

    private DemoLaborGrades ensureDemoLaborGrades() {
        LaborGrade e1 = findOrCreateLaborGrade("E1", "80.00");
        LaborGrade e2 = findOrCreateLaborGrade("E2", "90.00");
        LaborGrade e3 = findOrCreateLaborGrade("E3", "96.00");
        LaborGrade m1 = findOrCreateLaborGrade("M1", "118.00");
        LaborGrade m2 = findOrCreateLaborGrade("M2", "132.00");
        LaborGrade m3 = findOrCreateLaborGrade("M3", "148.00");
        LaborGrade x1 = findOrCreateLaborGrade("X1", "170.00");
        LaborGrade x2 = findOrCreateLaborGrade("X2", "185.00");

        ensureRateHistory(e1, "75.00", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        ensureRateHistory(e1, "80.00", LocalDate.of(2026, 1, 1), null);
        ensureRateHistory(m1, "108.00", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        ensureRateHistory(m1, "118.00", LocalDate.of(2026, 1, 1), null);

        return new DemoLaborGrades(e1, e2, e3, m1, m2, m3, x1, x2);
    }

    private void ensureSeedEmployeeProfile(Employee employee,
                                           SystemRole role,
                                           Employee supervisor,
                                           LaborGrade laborGrade) {
        boolean dirty = false;
        if (employee.getSystemRole() != role) {
            employee.setSystemRole(role);
            dirty = true;
        }
        Integer wantSupId = supervisor == null ? null : supervisor.getEmpId();
        Integer haveSupId = employee.getSupervisor() == null ? null : employee.getSupervisor().getEmpId();
        if (!java.util.Objects.equals(wantSupId, haveSupId)) {
            employee.setSupervisor(supervisor);
            dirty = true;
        }
        if (laborGrade != null && (employee.getLaborGrade() == null
                || !laborGrade.getLaborGradeId().equals(employee.getLaborGrade().getLaborGradeId()))) {
            employee.setLaborGrade(laborGrade);
            dirty = true;
        }
        if (dirty) {
            em.merge(employee);
        }
    }

    private Employee findOrCreateAndAlignEmployee(String firstName,
                                                  String lastName,
                                                  SystemRole role,
                                                  Employee supervisor,
                                                  LaborGrade laborGrade) {
        List<Employee> existing = em.createQuery(
                "SELECT e FROM Employee e WHERE e.empFirstName = :firstName AND e.empLastName = :lastName",
                Employee.class)
                .setParameter("firstName", firstName)
                .setParameter("lastName", lastName)
                .getResultList();
        if (!existing.isEmpty()) {
            Employee e = existing.get(0);
            ensureSeedEmployeeProfile(e, role, supervisor, laborGrade);
            return e;
        }
        return createEmployee(firstName, lastName, role, supervisor, laborGrade);
    }

    private void seedCrossReportDemoData() {
        Employee admin = em.find(Employee.class, 1);
        if (admin == null) {
            return;
        }

        DemoLaborGrades grades = ensureDemoLaborGrades();
        ensureSeedEmployeeProfile(admin, SystemRole.ADMIN, null, grades.x2());

        Employee elmerFudd = findOrCreateAndAlignEmployee(
                "Elmer", "Fudd", SystemRole.OPERATIONS_MANAGER, null, grades.m3());
        Employee roadRunner = findOrCreateAndAlignEmployee(
                "Road", "Runner", SystemRole.HR, admin, grades.x1());
        Employee bugsBunny = findOrCreateAndAlignEmployee(
                "Bugs", "Bunny", SystemRole.EMPLOYEE, elmerFudd, grades.e2());
        Employee daffyDuck = findOrCreateAndAlignEmployee(
                "Daffy", "Duck", SystemRole.EMPLOYEE, bugsBunny, grades.e2());
        Employee tweetyBird = findOrCreateAndAlignEmployee(
                "Tweety", "Bird", SystemRole.EMPLOYEE, bugsBunny, grades.e1());
        Employee sylvesterCat = findOrCreateAndAlignEmployee(
                "Sylvester", "Cat", SystemRole.EMPLOYEE, bugsBunny, grades.e3());
        Employee marvinMartian = findOrCreateAndAlignEmployee(
                "Marvin", "Martian", SystemRole.EMPLOYEE, elmerFudd, grades.m1());

        ensureProjectOneDemo(admin, elmerFudd, bugsBunny, daffyDuck, tweetyBird, sylvesterCat, grades);
        ensureProjectTwoDemo(admin, elmerFudd, marvinMartian, roadRunner, grades);
        ensureLaborProjectDemo(admin, grades);
    }

    private void ensureProjectOneDemo(Employee admin,
                                      Employee elmerFudd,
                                      Employee bugsBunny,
                                      Employee daffyDuck,
                                      Employee tweetyBird,
                                      Employee sylvesterCat,
                                      DemoLaborGrades grades) {
        Project proj = em.find(Project.class, "PROJ-1");
        if (proj == null) {
            return;
        }

        proj.setEndDate(PROJ1_PLAN_END);
        proj.setStartDate(LocalDate.of(2026, 1, 1));
        assignProjectMember(proj, bugsBunny, ProjectRole.PM);
        proj.setProjectManager(bugsBunny);
        assignProjectMember(proj, elmerFudd);
        assignProjectMember(proj, daffyDuck);
        assignProjectMember(proj, tweetyBird);
        assignProjectMember(proj, sylvesterCat);
        em.merge(proj);

        WorkPackage parent = findOrCreateSummaryWorkPackage(
                "A", "Control Account A", proj, elmerFudd,
                LocalDate.of(2026, 1, 1), PROJ1_PLAN_END,
                new BigDecimal("5000.00"), new BigDecimal("25.00"));

        WorkPackage wp1 = findOrCreateChildWorkPackage(
                "A.WP-1", "Procure Anvil", proj, parent, daffyDuck,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                new BigDecimal("1500.00"), new BigDecimal("15.00"));
        WorkPackage wp2 = findOrCreateChildWorkPackage(
                "A.WP-2", "Paint Fake Tunnel", proj, parent, sylvesterCat,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 14),
                new BigDecimal("1000.00"), new BigDecimal("50.00"));
        WorkPackage wp3 = findOrCreateChildWorkPackage(
                "A.WP-3", "Build Road", proj, parent, elmerFudd,
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 4, 15),
                new BigDecimal("2500.00"), new BigDecimal("35.00"));

        WorkPackage parentC = findOrCreateSummaryWorkPackage(
                "C", "Control Account C", proj, elmerFudd,
                LocalDate.of(2026, 2, 1), PROJ1_PLAN_END,
                new BigDecimal("4000.00"), new BigDecimal("20.00"));
        WorkPackage wpC1 = findOrCreateChildWorkPackage(
                "C.WP-1", "Integration Cutover", proj, parentC, bugsBunny,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31),
                new BigDecimal("1200.00"), new BigDecimal("10.00"));
        WorkPackage wpC2 = findOrCreateChildWorkPackage(
                "C.WP-2", "Documentation & Closeout", proj, parentC, elmerFudd,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 20),
                new BigDecimal("900.00"), new BigDecimal("5.00"));

        // Older seeds assigned Tweety MEMBER on C.WP-2; remove so ReBAC "member on A.WP-2 only" stays valid.
        deleteWorkPackageAssignmentIfExists("C.WP-2", tweetyBird, WpRole.MEMBER);

        assignWorkPackageMember(parent, elmerFudd, WpRole.RE);
        assignWorkPackageMember(parentC, elmerFudd, WpRole.RE);

        assignWorkPackageMember(wp1, daffyDuck, WpRole.RE);
        assignWorkPackageMember(wp1, bugsBunny, WpRole.RE);
        assignWorkPackageMember(wp2, sylvesterCat, WpRole.RE);
        assignWorkPackageMember(wp2, tweetyBird, WpRole.MEMBER);
        assignWorkPackageMember(wp3, elmerFudd, WpRole.RE);
        assignWorkPackageMember(wpC1, bugsBunny, WpRole.RE);
        assignWorkPackageMember(wpC2, elmerFudd, WpRole.RE);

        findOrCreateTimesheetWithSingleRow(daffyDuck, bugsBunny, DEMO_WEEK_ENDING,
                TimesheetStatus.SUBMITTED, wp1, grades.e2(), "32.0");
        findOrCreateTimesheetWithSingleRow(daffyDuck, admin, DEMO_PRIOR_WEEK,
                TimesheetStatus.APPROVED, wp1, grades.e2(), "24.0");
        findOrCreateTimesheetWithSingleRow(sylvesterCat, admin, DEMO_WEEK_ENDING,
                TimesheetStatus.APPROVED, wp2, grades.e3(), "28.0");
        findOrCreateTimesheetWithSingleRow(tweetyBird, bugsBunny, DEMO_WEEK_ENDING,
                TimesheetStatus.SUBMITTED, wp2, grades.e1(), "18.0");
        findOrCreateTimesheetWithSingleRow(tweetyBird, null, DEMO_DRAFT_WEEK,
                TimesheetStatus.DRAFT, wp2, grades.e1(), "8.0");
    }

    private void ensureProjectTwoDemo(Employee admin,
                                      Employee elmerFudd,
                                      Employee marvinMartian,
                                      Employee roadRunner,
                                      DemoLaborGrades grades) {
        Project proj2 = em.find(Project.class, "PROJ-2");
        if (proj2 == null) {
            return;
        }

        proj2.setEndDate(PROJ2_PLAN_END);
        assignProjectMember(proj2, marvinMartian, ProjectRole.PM);
        proj2.setProjectManager(marvinMartian);
        em.merge(proj2);

        assignProjectMember(proj2, roadRunner);

        WorkPackage parent = findOrCreateSummaryWorkPackage(
                "B", "Control Account B", proj2, marvinMartian,
                LocalDate.of(2026, 1, 3), PROJ2_PLAN_END,
                new BigDecimal("6000.00"), new BigDecimal("40.00"));

        WorkPackage wp1 = findOrCreateChildWorkPackage(
                "B.WP-1", "Survey Site", proj2, parent, marvinMartian,
                LocalDate.of(2026, 1, 3), LocalDate.of(2026, 1, 31),
                new BigDecimal("1800.00"), new BigDecimal("100.00"));
        WorkPackage wp2 = findOrCreateChildWorkPackage(
                "B.WP-2", "Pour Concrete", proj2, parent, roadRunner,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 20),
                new BigDecimal("2200.00"), new BigDecimal("65.00"));
        WorkPackage wp3 = findOrCreateChildWorkPackage(
                "B.WP-3", "Final Inspection", proj2, parent, marvinMartian,
                LocalDate.of(2026, 2, 21), PROJ2_PLAN_END,
                new BigDecimal("2000.00"), new BigDecimal("25.00"));

        assignWorkPackageMember(wp1, marvinMartian, WpRole.RE);
        assignWorkPackageMember(wp2, roadRunner, WpRole.RE);
        assignWorkPackageMember(wp3, marvinMartian, WpRole.MEMBER);

        findOrCreateTimesheetWithSingleRow(marvinMartian, admin, DEMO_WEEK_ENDING,
                TimesheetStatus.APPROVED, wp1, grades.m1(), "14.0");
        findOrCreateTimesheetWithSingleRow(roadRunner, admin, DEMO_WEEK_ENDING,
                TimesheetStatus.APPROVED, wp2, grades.x1(), "22.0");
        findOrCreateTimesheetWithSingleRow(marvinMartian, elmerFudd, DEMO_SECOND_WEEK,
                TimesheetStatus.SUBMITTED, wp3, grades.m1(), "10.0");
        findOrCreateTimesheetWithSingleRow(roadRunner, admin, DEMO_PRIOR_WEEK,
                TimesheetStatus.RETURNED, wp2, grades.x1(), "6.0");
        findOrCreateTimesheetWithSingleRow(marvinMartian, null, DEMO_DRAFT_WEEK,
                TimesheetStatus.DRAFT, wp1, grades.m1(), "4.0");
    }

    private void ensureLaborProjectDemo(Employee admin, DemoLaborGrades grades) {
        seedLaborReportDemo(grades);

        Employee marcus = findOrCreateAndAlignEmployee(
                "Marcus", "Aurelius", SystemRole.EMPLOYEE, admin, grades.e2());
        Employee elena = findOrCreateAndAlignEmployee(
                "Elena", "Fisher", SystemRole.EMPLOYEE, admin, grades.m1());
        Employee james = findOrCreateAndAlignEmployee(
                "James", "Holden", SystemRole.EMPLOYEE, admin, grades.m2());
        Employee sarah = findOrCreateAndAlignEmployee(
                "Sarah", "Connor", SystemRole.EMPLOYEE, admin, grades.e3());

        Project project = em.find(Project.class, "LABOR-1");
        if (project == null) {
            return;
        }

        WorkPackage foundations = findOrCreateLeafWorkPackage("LABOR-1.WP-1", "Foundations - Section A", project, marcus);
        WorkPackage electrical = findOrCreateLeafWorkPackage("LABOR-1.WP-2", "Electrical Wiring - Level 4", project, elena);
        WorkPackage hvac = findOrCreateLeafWorkPackage("LABOR-1.WP-3", "HVAC Duct Installation", project, james);
        WorkPackage plumbing = findOrCreateLeafWorkPackage("LABOR-1.WP-4", "Plumbing - Master Bath", project, sarah);

        updateWorkPackageForEarnedValue(foundations, marcus, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 15), "55000.00", "42.00");
        updateWorkPackageForEarnedValue(electrical, elena, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 28), "70000.00", "58.00");
        updateWorkPackageForEarnedValue(hvac, james, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 15), "60000.00", "47.00");
        updateWorkPackageForEarnedValue(plumbing, sarah, LocalDate.of(2026, 2, 10), LocalDate.of(2026, 3, 20), "65000.00", "63.00");

        findOrCreateTimesheetWithSingleRow(marcus, admin, DEMO_WEEK_ENDING,
                TimesheetStatus.APPROVED, foundations, grades.e2(), "36.0");
        findOrCreateTimesheetWithSingleRow(elena, admin, DEMO_WEEK_ENDING,
                TimesheetStatus.APPROVED, electrical, grades.m1(), "30.0");
        findOrCreateTimesheetWithSingleRow(james, admin, DEMO_WEEK_ENDING,
                TimesheetStatus.SUBMITTED, hvac, grades.m2(), "34.0");
        findOrCreateTimesheetWithSingleRow(sarah, admin, DEMO_WEEK_ENDING,
                TimesheetStatus.APPROVED, plumbing, grades.e3(), "41.0");
    }

    private void seedLaborReportDemo(DemoLaborGrades grades) {
        Employee admin = em.find(Employee.class, 1);
        if (admin == null) {
            return;
        }

        Employee marcus = findOrCreateAndAlignEmployee(
                "Marcus", "Aurelius", SystemRole.EMPLOYEE, admin, grades.e2());
        Employee elena = findOrCreateAndAlignEmployee(
                "Elena", "Fisher", SystemRole.EMPLOYEE, admin, grades.m1());
        Employee james = findOrCreateAndAlignEmployee(
                "James", "Holden", SystemRole.EMPLOYEE, admin, grades.m2());
        Employee sarah = findOrCreateAndAlignEmployee(
                "Sarah", "Connor", SystemRole.EMPLOYEE, admin, grades.e3());

        Project project = em.find(Project.class, "LABOR-1");
        if (project == null) {
            project = new Project();
            project.setProjId("LABOR-1");
            project.setProjType(ProjectType.INTERNAL);
            project.setProjectManager(admin);
            project.setProjName("Labor Report Demo");
            project.setDescription("Screenshot-aligned labor report demo dataset");
            project.setStatus(ProjectStatus.OPEN);
            project.setStartDate(LocalDate.of(2023, 10, 1));
            project.setEndDate(LABOR1_PROJECT_END);
            project.setCreatedDate(LocalDateTime.now());
            project.setModifiedDate(LocalDateTime.now());
            project.setCreatedBy(admin);
            project.setModifiedBy(admin);
            project.setMarkupRate(new BigDecimal("10.00"));
            project.setBac(new BigDecimal("250000.00"));
            em.persist(project);
        } else {
            project.setEndDate(LABOR1_PROJECT_END);
            project.setProjectManager(admin);
            em.merge(project);
        }

        assignProjectMember(project, marcus);
        assignProjectMember(project, elena);
        assignProjectMember(project, james);
        assignProjectMember(project, sarah);

        WorkPackage foundations = findOrCreateLeafWorkPackage("LABOR-1.WP-1", "Foundations - Section A", project, admin);
        WorkPackage electrical = findOrCreateLeafWorkPackage("LABOR-1.WP-2", "Electrical Wiring - Level 4", project, admin);
        WorkPackage hvac = findOrCreateLeafWorkPackage("LABOR-1.WP-3", "HVAC Duct Installation", project, admin);
        WorkPackage plumbing = findOrCreateLeafWorkPackage("LABOR-1.WP-4", "Plumbing - Master Bath", project, admin);

        assignWorkPackageMember(foundations, marcus);
        assignWorkPackageMember(electrical, elena);
        assignWorkPackageMember(hvac, james);
        assignWorkPackageMember(plumbing, sarah);

        LocalDate weekEndingHistorical = LocalDate.of(2023, 10, 27);
        findOrCreateTimesheetWithSingleRow(marcus, admin, weekEndingHistorical,
                TimesheetStatus.APPROVED, foundations, grades.e2(), "40.0");
        findOrCreateTimesheetWithSingleRow(elena, admin, weekEndingHistorical,
                TimesheetStatus.SUBMITTED, electrical, grades.m1(), "45.5");
        findOrCreateTimesheetWithSingleRow(james, admin, weekEndingHistorical,
                TimesheetStatus.APPROVED, hvac, grades.m2(), "38.0");
        findOrCreateTimesheetWithSingleRow(sarah, admin, weekEndingHistorical,
                TimesheetStatus.APPROVED, plumbing, grades.e3(), "42.0");

        System.out.println("[Seeder] Labor report demo dataset ensured.");
    }

    private LaborGrade findOrCreateLaborGrade(String code, String chargeRate) {
        List<LaborGrade> existing = em.createQuery(
                "SELECT lg FROM LaborGrade lg WHERE lg.gradeCode = :code", LaborGrade.class)
                .setParameter("code", code)
                .getResultList();
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        LaborGrade laborGrade = new LaborGrade();
        laborGrade.setGradeCode(code);
        laborGrade.setChargeRate(new BigDecimal(chargeRate));
        em.persist(laborGrade);
        return laborGrade;
    }

    private void ensureRateHistory(LaborGrade laborGrade,
                                   String chargeRate,
                                   LocalDate startDate,
                                   LocalDate endDate) {
        Long count = em.createQuery(
                "SELECT COUNT(rh) FROM RateHistory rh " +
                "WHERE rh.laborGrade = :laborGrade " +
                "  AND rh.startDate = :startDate " +
                "  AND ((:endDate IS NULL AND rh.endDate IS NULL) OR rh.endDate = :endDate)",
                Long.class)
                .setParameter("laborGrade", laborGrade)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getSingleResult();

        if (count != null && count > 0) {
            return;
        }

        createRateHistory(laborGrade, chargeRate, startDate, endDate);
    }

    private WorkPackage findOrCreateLeafWorkPackage(String wpId, String name, Project project, Employee responsibleEmployee) {
        WorkPackage existing = em.find(WorkPackage.class, wpId);
        if (existing != null) {
            return existing;
        }

        WorkPackage workPackage = new WorkPackage();
        workPackage.setWpId(wpId);
        workPackage.setWpName(name);
        workPackage.setDescription(name);
        workPackage.setProject(project);
        workPackage.setParentWorkPackage(null);
        workPackage.setWpType(WorkPackageType.LOWEST_LEVEL);
        workPackage.setStatus(WorkPackageStatus.OPEN_FOR_CHARGES);
        workPackage.setStructureLocked(false);
        workPackage.setBudgetedEffort(new BigDecimal("40.00"));
        workPackage.setBcws(BigDecimal.ZERO);
        workPackage.setPlanStartDate(LocalDate.of(2023, 10, 1));
        workPackage.setPlanEndDate(LocalDate.of(2023, 10, 31));
        workPackage.setResponsibleEmployee(responsibleEmployee);
        workPackage.setBac(new BigDecimal("10000.00"));
        workPackage.setPercentComplete(BigDecimal.ZERO);
        workPackage.setCreatedDate(LocalDateTime.now());
        workPackage.setModifiedDate(LocalDateTime.now());
        workPackage.setCreatedBy(responsibleEmployee);
        workPackage.setModifiedBy(responsibleEmployee);
        em.persist(workPackage);
        return workPackage;
    }

    private WorkPackage findOrCreateSummaryWorkPackage(String wpId,
                                                       String name,
                                                       Project project,
                                                       Employee responsibleEmployee,
                                                       LocalDate startDate,
                                                       LocalDate endDate,
                                                       BigDecimal bac,
                                                       BigDecimal percentComplete) {
        WorkPackage existing = em.find(WorkPackage.class, wpId);
        if (existing != null) {
            updateSummaryWorkPackage(existing, project, responsibleEmployee, startDate, endDate, bac, percentComplete);
            return existing;
        }

        WorkPackage workPackage = new WorkPackage();
        workPackage.setWpId(wpId);
        workPackage.setWpName(name);
        workPackage.setDescription(name);
        workPackage.setProject(project);
        workPackage.setParentWorkPackage(null);
        workPackage.setWpType(WorkPackageType.SUMMARY);
        workPackage.setStatus(WorkPackageStatus.OPEN_FOR_CHARGES);
        workPackage.setStructureLocked(false);
        workPackage.setBudgetedEffort(BigDecimal.ZERO);
        workPackage.setBcws(BigDecimal.ZERO);
        workPackage.setCreatedDate(LocalDateTime.now());
        workPackage.setModifiedDate(LocalDateTime.now());
        workPackage.setCreatedBy(responsibleEmployee);
        workPackage.setModifiedBy(responsibleEmployee);
        updateSummaryWorkPackage(workPackage, project, responsibleEmployee, startDate, endDate, bac, percentComplete);
        em.persist(workPackage);
        return workPackage;
    }

    private WorkPackage findOrCreateChildWorkPackage(String wpId,
                                                     String name,
                                                     Project project,
                                                     WorkPackage parent,
                                                     Employee responsibleEmployee,
                                                     LocalDate startDate,
                                                     LocalDate endDate,
                                                     BigDecimal bac,
                                                     BigDecimal percentComplete) {
        WorkPackage existing = em.find(WorkPackage.class, wpId);
        if (existing != null) {
            updateChildWorkPackage(existing, project, parent, responsibleEmployee, startDate, endDate, bac, percentComplete);
            return existing;
        }

        WorkPackage workPackage = new WorkPackage();
        workPackage.setWpId(wpId);
        workPackage.setWpName(name);
        workPackage.setDescription(name);
        workPackage.setCreatedDate(LocalDateTime.now());
        workPackage.setCreatedBy(responsibleEmployee);
        updateChildWorkPackage(workPackage, project, parent, responsibleEmployee, startDate, endDate, bac, percentComplete);
        em.persist(workPackage);
        return workPackage;
    }

    private void updateSummaryWorkPackage(WorkPackage workPackage,
                                          Project project,
                                          Employee responsibleEmployee,
                                          LocalDate startDate,
                                          LocalDate endDate,
                                          BigDecimal bac,
                                          BigDecimal percentComplete) {
        workPackage.setProject(project);
        workPackage.setParentWorkPackage(null);
        workPackage.setWpType(WorkPackageType.SUMMARY);
        workPackage.setStatus(WorkPackageStatus.OPEN_FOR_CHARGES);
        workPackage.setStructureLocked(false);
        workPackage.setBudgetedEffort(BigDecimal.ZERO);
        workPackage.setBcws(BigDecimal.ZERO);
        workPackage.setPlanStartDate(startDate);
        workPackage.setPlanEndDate(endDate);
        workPackage.setResponsibleEmployee(responsibleEmployee);
        workPackage.setBac(bac);
        workPackage.setPercentComplete(percentComplete);
        workPackage.setModifiedDate(LocalDateTime.now());
        workPackage.setModifiedBy(responsibleEmployee);
    }

    private void updateChildWorkPackage(WorkPackage workPackage,
                                        Project project,
                                        WorkPackage parent,
                                        Employee responsibleEmployee,
                                        LocalDate startDate,
                                        LocalDate endDate,
                                        BigDecimal bac,
                                        BigDecimal percentComplete) {
        workPackage.setProject(project);
        workPackage.setParentWorkPackage(parent);
        workPackage.setWpType(WorkPackageType.LOWEST_LEVEL);
        workPackage.setStatus(WorkPackageStatus.OPEN_FOR_CHARGES);
        workPackage.setStructureLocked(false);
        workPackage.setBudgetedEffort(new BigDecimal("40.00"));
        workPackage.setBcws(BigDecimal.ZERO);
        workPackage.setPlanStartDate(startDate);
        workPackage.setPlanEndDate(endDate);
        workPackage.setResponsibleEmployee(responsibleEmployee);
        workPackage.setBac(bac);
        workPackage.setPercentComplete(percentComplete);
        workPackage.setModifiedDate(LocalDateTime.now());
        workPackage.setModifiedBy(responsibleEmployee);
    }

    private void updateWorkPackageForEarnedValue(WorkPackage workPackage,
                                                 Employee responsibleEmployee,
                                                 LocalDate startDate,
                                                 LocalDate endDate,
                                                 String bac,
                                                 String percentComplete) {
        workPackage.setPlanStartDate(startDate);
        workPackage.setPlanEndDate(endDate);
        workPackage.setResponsibleEmployee(responsibleEmployee);
        workPackage.setBac(new BigDecimal(bac));
        workPackage.setPercentComplete(new BigDecimal(percentComplete));
        workPackage.setStatus(WorkPackageStatus.OPEN_FOR_CHARGES);
        workPackage.setModifiedDate(LocalDateTime.now());
        workPackage.setModifiedBy(responsibleEmployee);
    }

    private void assignProjectMember(Project project, Employee employee) {
        assignProjectMember(project, employee, ProjectRole.MEMBER);
    }

    private void assignProjectMember(Project project, Employee employee, ProjectRole role) {
        Long count = em.createQuery(
                "SELECT COUNT(pa) FROM ProjectAssignment pa WHERE pa.project.projId = :projId AND pa.employee.empId = :empId",
                Long.class)
                .setParameter("projId", project.getProjId())
                .setParameter("empId", employee.getEmpId())
                .getSingleResult();
        if (count != null && count > 0) {
            return;
        }

        ProjectAssignment assignment = new ProjectAssignment();
        assignment.setEmployee(employee);
        assignment.setProject(project);
        assignment.setAssignmentDate(LocalDate.now());
        assignment.setProjectRole(role);
        em.persist(assignment);
    }

    private void deleteWorkPackageAssignmentIfExists(String wpId, Employee employee, WpRole role) {
        em.createQuery(
                "DELETE FROM WorkPackageAssignment wpa WHERE wpa.workPackage.wpId = :wpId "
                        + "AND wpa.employee.empId = :empId AND wpa.wpRole = :role")
                .setParameter("wpId", wpId)
                .setParameter("empId", employee.getEmpId())
                .setParameter("role", role)
                .executeUpdate();
    }

    private void assignWorkPackageMember(WorkPackage workPackage, Employee employee) {
        assignWorkPackageMember(workPackage, employee, WpRole.MEMBER);
    }

    private void assignWorkPackageMember(WorkPackage workPackage, Employee employee, WpRole role) {
        Long count = em.createQuery(
                "SELECT COUNT(wpa) FROM WorkPackageAssignment wpa WHERE wpa.workPackage.wpId = :wpId AND wpa.employee.empId = :empId",
                Long.class)
                .setParameter("wpId", workPackage.getWpId())
                .setParameter("empId", employee.getEmpId())
                .getSingleResult();
        if (count != null && count > 0) {
            return;
        }

        WorkPackageAssignment assignment = new WorkPackageAssignment();
        assignment.setEmployee(employee);
        assignment.setWorkPackage(workPackage);
        assignment.setAssignmentDate(LocalDate.now());
        assignment.setWpRole(role);
        em.persist(assignment);
    }

    private void findOrCreateTimesheetWithSingleRow(Employee employee,
                                                    Employee approver,
                                                    LocalDate weekEnding,
                                                    TimesheetStatus status,
                                                    WorkPackage workPackage,
                                                    LaborGrade laborGrade,
                                                    String totalHours) {
        List<Timesheet> existing = em.createQuery(
                "SELECT t FROM Timesheet t WHERE t.employee.empId = :empId AND t.weekEnding = :weekEnding",
                Timesheet.class)
                .setParameter("empId", employee.getEmpId())
                .setParameter("weekEnding", weekEnding)
                .getResultList();

        Timesheet timesheet;
        if (existing.isEmpty()) {
            timesheet = new Timesheet();
            timesheet.setEmployee(employee);
            timesheet.setWeekEnding(weekEnding);
            timesheet.setApprover(approver);
            if (status == TimesheetStatus.RETURNED) {
                timesheet.setReturnComment("Demo seed: please correct and resubmit.");
            } else {
                timesheet.setReturnComment(null);
            }
            timesheet.setStatus(status);
            timesheet.setSignature(employee.getESignature());
            em.persist(timesheet);
        } else {
            timesheet = existing.get(0);
            timesheet.setApprover(approver);
            timesheet.setStatus(status);
            if (status == TimesheetStatus.RETURNED) {
                timesheet.setReturnComment("Demo seed: please correct and resubmit.");
            } else {
                timesheet.setReturnComment(null);
            }
        }

        Long rowCount = em.createQuery(
                "SELECT COUNT(tr) FROM TimesheetRow tr WHERE tr.timesheet.tsId = :tsId AND tr.workPackage.wpId = :wpId",
                Long.class)
                .setParameter("tsId", timesheet.getTsId())
                .setParameter("wpId", workPackage.getWpId())
                .getSingleResult();
        if (rowCount != null && rowCount > 0) {
            return;
        }

        TimesheetRow row = new TimesheetRow();
        row.setTimesheet(timesheet);
        row.setWorkPackage(workPackage);
        row.setLaborGrade(laborGrade);
        row.setMonday(new BigDecimal(totalHours));
        row.setTuesday(BigDecimal.ZERO);
        row.setWednesday(BigDecimal.ZERO);
        row.setThursday(BigDecimal.ZERO);
        row.setFriday(BigDecimal.ZERO);
        row.setSaturday(BigDecimal.ZERO);
        row.setSunday(BigDecimal.ZERO);
        em.persist(row);
    }
}
