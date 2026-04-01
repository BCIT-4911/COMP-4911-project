package com.corejsf.seed;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.corejsf.Entity.Employee;
import com.corejsf.Entity.EmployeeESignature;
import com.corejsf.Entity.LaborGrade;
import com.corejsf.Entity.Project;
import com.corejsf.Entity.ProjectAssignment;
import com.corejsf.Entity.ProjectRole;
import com.corejsf.Entity.ProjectStatus;
import com.corejsf.Entity.ProjectType;
import com.corejsf.Entity.Timesheet;
import com.corejsf.Entity.TimesheetRow;
import com.corejsf.Entity.TimesheetStatus;
import com.corejsf.Entity.SystemRole;
import com.corejsf.Entity.WorkPackage;
import com.corejsf.Entity.WorkPackageAssignment;
import com.corejsf.Entity.WorkPackageStatus;
import com.corejsf.Entity.WorkPackageType;
import com.corejsf.Entity.WpRole;
import com.corejsf.Entity.RateHistory;

import jakarta.annotation.PostConstruct;
import org.mindrot.jbcrypt.BCrypt;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Startup
@Singleton
public class EmptyDbSeeder {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    @PostConstruct
    public void seed() {
        boolean hasBaseSeed = em.find(WorkPackage.class, "A") != null;
        if (!hasBaseSeed) {
            System.out.println("[Seeder] Empty DB detected. Seeding minimum dataset...");

        LaborGrade lg = em.find(LaborGrade.class, 1);
        if (lg == null) {
            lg = new LaborGrade();
            lg.setGradeCode("E1");
            lg.setChargeRate(new BigDecimal("85.00"));
            em.persist(lg);
        }

            createRateHistory(lg, "75.00", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
            createRateHistory(lg, "85.00", LocalDate.of(2026, 1, 1), null);

            EmployeeESignature sig = em.find(EmployeeESignature.class, 1);
            if (sig == null) {
                sig = new EmployeeESignature();
                sig.setSignatureData(new byte[] { 0x00 });
                sig.setSignedAt(LocalDateTime.now());
                em.persist(sig);
            }

            // Admin is always first employee (id 1 on fresh DB). No find(100) - ensures deterministic order.
            Employee admin = new Employee();
            admin.setEmpFirstName("Wile");
            admin.setEmpLastName("Coyote");
            admin.setEmpPassword(BCrypt.hashpw("password", BCrypt.gensalt()));
            admin.setSystemRole(SystemRole.OPERATIONS_MANAGER);
            admin.setESignature(sig);
            admin.setLaborGrade(lg);
            admin.setSupervisor(null);
            admin.setVacationSickBalance(new BigDecimal("40.00"));
            admin.setExpectedWeeklyHours(new BigDecimal("40.0"));
            em.persist(admin);

            Employee roadRunner = createEmployee("Road", "Runner", SystemRole.HR, admin, lg);
            Employee bugsBunny = createEmployee("Bugs", "Bunny", SystemRole.EMPLOYEE, admin, lg);
            Employee daffyDuck = createEmployee("Daffy", "Duck", SystemRole.EMPLOYEE, bugsBunny, lg);
            Employee tweetyBird = createEmployee("Tweety", "Bird", SystemRole.EMPLOYEE, bugsBunny, lg);
            Employee sylvesterCat = createEmployee("Sylvester", "Cat", SystemRole.EMPLOYEE, bugsBunny, lg);

        

            Project proj = em.find(Project.class, "PROJ-1");
            if (proj == null) {
                proj = new Project();
                proj.setProjId("PROJ-1");
                proj.setProjType(ProjectType.INTERNAL);  
                proj.setProjectManager(admin);              
                proj.setProjName("Demo Project");
                proj.setDescription("Seed data for Earned Value report");
                proj.setStatus(ProjectStatus.OPEN);      
                proj.setStartDate(LocalDate.of(2026, 1, 1));
                proj.setEndDate(LocalDate.of(2026, 3, 31));
                proj.setCreatedDate(LocalDateTime.now());
                proj.setModifiedDate(LocalDateTime.now());
                proj.setCreatedBy(admin);
                proj.setModifiedBy(admin);
                proj.setMarkupRate(new BigDecimal("10.00"));
                proj.setBac(new BigDecimal("10000.00"));

                em.persist(proj);
            }


            Employee marvinMartian = createEmployee("Marvin", "Martian", SystemRole.EMPLOYEE, admin, lg);

            // Project 2 for Seed cases
            Project proj2 = em.find(Project.class, "PROJ-2");
            if(proj2 == null) {
                proj2 = new Project();
                proj2.setProjId("PROJ-2");
                proj2.setProjType(ProjectType.EXTERNAL);
                proj2.setProjectManager(marvinMartian);
                proj2.setProjName("Demo External Project");
                proj2.setDescription("Seed Data for Earnved Value Report");
                proj2.setStatus(ProjectStatus.OPEN);
                proj2.setStartDate(LocalDate.of(2026, 1, 3));
                proj2.setEndDate(LocalDate.of(2026, 3, 3));
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
            parent.setPlanEndDate(LocalDate.of(2026, 3, 31));

            parent.setResponsibleEmployee(admin); 
            parent.setBac(new BigDecimal("5000.00"));
            parent.setPercentComplete(new BigDecimal("25.00"));

            parent.setCreatedDate(LocalDateTime.now());
            parent.setModifiedDate(LocalDateTime.now());
            parent.setCreatedBy(admin);
            parent.setModifiedBy(admin);

            em.persist(parent);

            createChild("A.WP-1", "Procure Anvil", proj, parent, admin,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                    new BigDecimal("1500.00"), new BigDecimal("0.00"));

            createChild("A.WP-2", "Paint Fake Tunnel", proj, parent, admin,
                    LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 14),
                    new BigDecimal("1000.00"), new BigDecimal("50.00"));

            createChild("A.WP-3", "Build Road", proj, parent, admin,
                    LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 1),
                    new BigDecimal("2500.00"), new BigDecimal("35.00"));


            ProjectAssignment paBugs = new ProjectAssignment();
            paBugs.setEmployee(bugsBunny);
            paBugs.setProject(proj);
            paBugs.setAssignmentDate(LocalDate.now());
            paBugs.setProjectRole(ProjectRole.PM);
            em.persist(paBugs);

            proj.setProjectManager(bugsBunny);
            em.merge(proj);

            // Assign WP-level employees to the project so they can navigate to it
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

            proj2.setProjectManager(marvinMartian);
            proj2.setProjName("Demo External Project");
            proj2.setDescription("Seed Data for Earnved Value Report");
            proj2.setStatus(ProjectStatus.OPEN);
            proj2.setStartDate(LocalDate.of(2026, 1, 3));
            proj2.setEndDate(LocalDate.of(2026, 3, 3));
            proj2.setCreatedDate(LocalDateTime.now());
            proj2.setModifiedDate(LocalDateTime.now());
            proj2.setCreatedBy(admin);
            proj2.setModifiedBy(admin);
            proj2.setMarkupRate(new BigDecimal("10.00"));
            proj2.setBac(new BigDecimal("15000.00"));

            WorkPackage wp1 = em.find(WorkPackage.class, "A.WP-1");
            WorkPackage wp2 = em.find(WorkPackage.class, "A.WP-2");
            if (wp1 != null) {
                WorkPackageAssignment wpaDaffy = new WorkPackageAssignment();
                wpaDaffy.setEmployee(daffyDuck);
                wpaDaffy.setWorkPackage(wp1);
                wpaDaffy.setAssignmentDate(LocalDate.now());
                wpaDaffy.setWpRole(WpRole.RE);
                em.persist(wpaDaffy);
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

            System.out.println("[Seeder] Seed complete: LaborGrade + Signature + Employee + Project + A + children + HR/PM/RE/MEMBER.");
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

    private void seedCrossReportDemoData() {
        Employee admin = em.find(Employee.class, 1);
        if (admin == null) {
            return;
        }

        LaborGrade baseGrade = findOrCreateLaborGrade("E1", "85.00");
        LaborGrade foremanTwo = findOrCreateLaborGrade("F2", "120.00");
        LaborGrade journeyman = findOrCreateLaborGrade("JM", "105.00");
        LaborGrade specialist = findOrCreateLaborGrade("SP", "110.00");
        LaborGrade foremanOne = findOrCreateLaborGrade("F1", "115.00");

        ensureRateHistory(baseGrade, "75.00", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        ensureRateHistory(baseGrade, "85.00", LocalDate.of(2026, 1, 1), null);

        Employee bugsBunny = findOrCreateEmployee("Bugs", "Bunny", SystemRole.EMPLOYEE, admin, baseGrade);
        Employee daffyDuck = findOrCreateEmployee("Daffy", "Duck", SystemRole.EMPLOYEE, bugsBunny, baseGrade);
        Employee tweetyBird = findOrCreateEmployee("Tweety", "Bird", SystemRole.EMPLOYEE, bugsBunny, baseGrade);
        Employee sylvesterCat = findOrCreateEmployee("Sylvester", "Cat", SystemRole.EMPLOYEE, bugsBunny, baseGrade);
        Employee marvinMartian = findOrCreateEmployee("Marvin", "Martian", SystemRole.EMPLOYEE, admin, baseGrade);
        Employee roadRunner = findOrCreateEmployee("Road", "Runner", SystemRole.HR, admin, baseGrade);

        ensureProjectOneDemo(admin, bugsBunny, daffyDuck, tweetyBird, sylvesterCat, baseGrade);
        ensureProjectTwoDemo(admin, marvinMartian, roadRunner, baseGrade);
        ensureLaborProjectDemo(admin, foremanTwo, journeyman, specialist, foremanOne);
    }

    private void ensureProjectOneDemo(Employee admin,
                                      Employee bugsBunny,
                                      Employee daffyDuck,
                                      Employee tweetyBird,
                                      Employee sylvesterCat,
                                      LaborGrade baseGrade) {
        Project proj = em.find(Project.class, "PROJ-1");
        if (proj == null) {
            return;
        }

        assignProjectMember(proj, bugsBunny, ProjectRole.PM);
        proj.setProjectManager(bugsBunny);
        em.merge(proj);

        assignProjectMember(proj, daffyDuck);
        assignProjectMember(proj, tweetyBird);
        assignProjectMember(proj, sylvesterCat);

        WorkPackage parent = findOrCreateSummaryWorkPackage(
                "A", "Control Account A", proj, admin,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31),
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
                "A.WP-3", "Build Road", proj, parent, tweetyBird,
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 1),
                new BigDecimal("2500.00"), new BigDecimal("35.00"));

        assignWorkPackageMember(wp1, daffyDuck, WpRole.RE);
        assignWorkPackageMember(wp2, sylvesterCat, WpRole.RE);
        assignWorkPackageMember(wp3, tweetyBird, WpRole.RE);

        LocalDate weekEnding = LocalDate.of(2026, 3, 8);
        findOrCreateTimesheetWithSingleRow(daffyDuck, admin, weekEnding, TimesheetStatus.APPROVED, wp1, baseGrade, "32.0");
        findOrCreateTimesheetWithSingleRow(sylvesterCat, admin, weekEnding, TimesheetStatus.APPROVED, wp2, baseGrade, "28.0");
        findOrCreateTimesheetWithSingleRow(tweetyBird, admin, weekEnding, TimesheetStatus.SUBMITTED, wp3, baseGrade, "18.0");
    }

    private void ensureProjectTwoDemo(Employee admin,
                                      Employee marvinMartian,
                                      Employee roadRunner,
                                      LaborGrade baseGrade) {
        Project proj2 = em.find(Project.class, "PROJ-2");
        if (proj2 == null) {
            return;
        }

        assignProjectMember(proj2, marvinMartian, ProjectRole.PM);
        proj2.setProjectManager(marvinMartian);
        em.merge(proj2);

        assignProjectMember(proj2, roadRunner);

        WorkPackage parent = findOrCreateSummaryWorkPackage(
                "B", "Control Account B", proj2, marvinMartian,
                LocalDate.of(2026, 1, 3), LocalDate.of(2026, 3, 3),
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
                LocalDate.of(2026, 2, 21), LocalDate.of(2026, 3, 3),
                new BigDecimal("2000.00"), new BigDecimal("25.00"));

        assignWorkPackageMember(wp1, marvinMartian, WpRole.RE);
        assignWorkPackageMember(wp2, roadRunner, WpRole.RE);
        assignWorkPackageMember(wp3, marvinMartian, WpRole.MEMBER);

        LocalDate weekEnding = LocalDate.of(2026, 3, 8);
        findOrCreateTimesheetWithSingleRow(marvinMartian, admin, weekEnding, TimesheetStatus.APPROVED, wp1, baseGrade, "14.0");
        findOrCreateTimesheetWithSingleRow(roadRunner, admin, weekEnding, TimesheetStatus.APPROVED, wp2, baseGrade, "22.0");
        findOrCreateTimesheetWithSingleRow(marvinMartian, admin, weekEnding, TimesheetStatus.SUBMITTED, wp3, baseGrade, "10.0");
    }

    private void ensureLaborProjectDemo(Employee admin,
                                        LaborGrade foremanTwo,
                                        LaborGrade journeyman,
                                        LaborGrade specialist,
                                        LaborGrade foremanOne) {
        seedLaborReportDemo();

        Employee marcus = findOrCreateEmployee("Marcus", "Aurelius", SystemRole.EMPLOYEE, admin, foremanTwo);
        Employee elena = findOrCreateEmployee("Elena", "Fisher", SystemRole.EMPLOYEE, admin, journeyman);
        Employee james = findOrCreateEmployee("James", "Holden", SystemRole.EMPLOYEE, admin, specialist);
        Employee sarah = findOrCreateEmployee("Sarah", "Connor", SystemRole.EMPLOYEE, admin, foremanOne);

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

        LocalDate currentWeek = LocalDate.of(2026, 3, 8);
        findOrCreateTimesheetWithSingleRow(marcus, admin, currentWeek, TimesheetStatus.APPROVED, foundations, foremanTwo, "36.0");
        findOrCreateTimesheetWithSingleRow(elena, admin, currentWeek, TimesheetStatus.APPROVED, electrical, journeyman, "30.0");
        findOrCreateTimesheetWithSingleRow(james, admin, currentWeek, TimesheetStatus.SUBMITTED, hvac, specialist, "34.0");
        findOrCreateTimesheetWithSingleRow(sarah, admin, currentWeek, TimesheetStatus.APPROVED, plumbing, foremanOne, "41.0");
    }

    private void seedLaborReportDemo() {
        Employee admin = em.find(Employee.class, 1);
        if (admin == null) {
            return;
        }

        LaborGrade foremanTwo = findOrCreateLaborGrade("F2", "120.00");
        LaborGrade journeyman = findOrCreateLaborGrade("JM", "105.00");
        LaborGrade specialist = findOrCreateLaborGrade("SP", "110.00");
        LaborGrade foremanOne = findOrCreateLaborGrade("F1", "115.00");

        Employee marcus = findOrCreateEmployee("Marcus", "Aurelius", SystemRole.EMPLOYEE, admin, foremanTwo);
        Employee elena = findOrCreateEmployee("Elena", "Fisher", SystemRole.EMPLOYEE, admin, journeyman);
        Employee james = findOrCreateEmployee("James", "Holden", SystemRole.EMPLOYEE, admin, specialist);
        Employee sarah = findOrCreateEmployee("Sarah", "Connor", SystemRole.EMPLOYEE, admin, foremanOne);

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
            project.setEndDate(LocalDate.of(2023, 12, 31));
            project.setCreatedDate(LocalDateTime.now());
            project.setModifiedDate(LocalDateTime.now());
            project.setCreatedBy(admin);
            project.setModifiedBy(admin);
            project.setMarkupRate(new BigDecimal("10.00"));
            project.setBac(new BigDecimal("250000.00"));
            em.persist(project);
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

        LocalDate weekEnding = LocalDate.of(2023, 10, 27);
        findOrCreateTimesheetWithSingleRow(marcus, admin, weekEnding, TimesheetStatus.APPROVED, foundations, foremanTwo, "40.0");
        findOrCreateTimesheetWithSingleRow(elena, admin, weekEnding, TimesheetStatus.SUBMITTED, electrical, journeyman, "45.5");
        findOrCreateTimesheetWithSingleRow(james, admin, weekEnding, TimesheetStatus.APPROVED, hvac, specialist, "38.0");
        findOrCreateTimesheetWithSingleRow(sarah, admin, weekEnding, TimesheetStatus.APPROVED, plumbing, foremanOne, "42.0");

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

    private Employee findOrCreateEmployee(String firstName, String lastName, SystemRole role, Employee supervisor, LaborGrade laborGrade) {
        List<Employee> existing = em.createQuery(
                "SELECT e FROM Employee e WHERE e.empFirstName = :firstName AND e.empLastName = :lastName",
                Employee.class)
                .setParameter("firstName", firstName)
                .setParameter("lastName", lastName)
                .getResultList();
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return createEmployee(firstName, lastName, role, supervisor, laborGrade);
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
            timesheet.setReturnComment(null);
            timesheet.setStatus(status);
            timesheet.setSignature(employee.getESignature());
            em.persist(timesheet);
        } else {
            timesheet = existing.get(0);
            timesheet.setApprover(approver);
            timesheet.setStatus(status);
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
