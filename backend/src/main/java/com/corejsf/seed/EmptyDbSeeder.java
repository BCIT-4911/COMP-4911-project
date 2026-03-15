package com.corejsf.seed;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.corejsf.Entity.Employee;
import com.corejsf.Entity.EmployeeESignature;
import com.corejsf.Entity.LaborGrade;
import com.corejsf.Entity.Project;
import com.corejsf.Entity.ProjectAssignment;
import com.corejsf.Entity.ProjectRole;
import com.corejsf.Entity.ProjectStatus;
import com.corejsf.Entity.ProjectType;
import com.corejsf.Entity.SystemRole;
import com.corejsf.Entity.WorkPackage;
import com.corejsf.Entity.WorkPackageAssignment;
import com.corejsf.Entity.WorkPackageStatus;
import com.corejsf.Entity.WorkPackageType;
import com.corejsf.Entity.WpRole;
import com.corejsf.Entity.Timesheet;
import com.corejsf.Entity.TimesheetRow;
import com.corejsf.Entity.TimesheetStatus;

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

        if (em.find(WorkPackage.class, "A") != null) {
            System.out.println("[Seeder] A exists, skipping.");
            return;
        }

        System.out.println("[Seeder] Empty DB detected. Seeding minimum dataset...");


        LaborGrade lg = em.find(LaborGrade.class, 1);
        if (lg == null) {
            lg = new LaborGrade();
            lg.setGradeCode("E1");
            lg.setChargeRate(new BigDecimal("85.00"));
            em.persist(lg);
        }

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

        createChild("CA-1.WP-1", "Procure Anvil", proj, parent, admin,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                new BigDecimal("1500.00"), new BigDecimal("0.00"));

        createChild("CA-1.WP-2", "Paint Fake Tunnel", proj, parent, admin,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 14),
                new BigDecimal("1000.00"), new BigDecimal("50.00"));

        createChild("CA-1.WP-3", "Build Road", proj, parent, admin,
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 1),
                new BigDecimal("3000.00"), new BigDecimal("35.00"));


        ProjectAssignment paBugs = new ProjectAssignment();
        paBugs.setEmployee(bugsBunny);
        paBugs.setProject(proj);
        paBugs.setAssignmentDate(LocalDate.now());
        paBugs.setProjectRole(ProjectRole.PM);
        em.persist(paBugs);

        proj.setProjectManager(bugsBunny);
        em.merge(proj);

        ProjectAssignment paMarvin = new ProjectAssignment();
        paMarvin.setEmployee(marvinMartian);
        paMarvin.setProject(proj2);
        paMarvin.setAssignmentDate(LocalDate.now());
        paMarvin.setProjectRole(ProjectRole.PM);
        em.persist(paMarvin);

        proj2.setProjectManager(marvinMartian);
        em.merge(proj2);

        WorkPackage wp1 = em.find(WorkPackage.class, "CA-1.WP-1");
        WorkPackage wp2 = em.find(WorkPackage.class, "CA-1.WP-2");
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


        //Timesheet seeding with 4 Statuses
        LocalDate weekEnding = LocalDate.of(2026, 1, 11);

    // ---------- DRAFT ----------
        Timesheet tsDraft = new Timesheet();
        tsDraft.setEmployee(daffyDuck);
        tsDraft.setWeekEnding(weekEnding);
        tsDraft.setApprover(null);
        tsDraft.setReturnComment(null);
        tsDraft.setStatus(TimesheetStatus.DRAFT);
        tsDraft.setSignature(null);
        em.persist(tsDraft);

        TimesheetRow tsDraftRow = new TimesheetRow();
        tsDraftRow.setWorkPackage(wp1);
        tsDraftRow.setLaborGrade(daffyDuck.getLaborGrade());
        tsDraftRow.setMonday(new BigDecimal("8.0"));
        tsDraftRow.setTuesday(new BigDecimal("8.0"));
        tsDraftRow.setTimesheet(tsDraft);
        tsDraftRow.setWednesday(new BigDecimal("8.0"));
        tsDraftRow.setThursday(new BigDecimal("8.0"));
        tsDraftRow.setFriday(new BigDecimal("8.0"));
        tsDraftRow.setSaturday(new BigDecimal("0.0"));
        tsDraftRow.setSunday(new BigDecimal("0.0"));
        em.persist(tsDraftRow);


        // ---------- SUBMITTED ----------
        Timesheet tsSubmitted = new Timesheet();
        tsSubmitted.setEmployee(tweetyBird);
        tsSubmitted.setWeekEnding(weekEnding);
        tsSubmitted.setApprover(bugsBunny);
        tsSubmitted.setReturnComment(null);
        tsSubmitted.setStatus(TimesheetStatus.SUBMITTED);
        tsSubmitted.setSignature(tweetyBird.getESignature());
        em.persist(tsSubmitted);

        TimesheetRow tsSubmittedRow = new TimesheetRow();
        tsSubmittedRow.setTimesheet(tsSubmitted);
        tsSubmittedRow.setWorkPackage(wp2);
        tsSubmittedRow.setLaborGrade(tweetyBird.getLaborGrade());
        tsSubmittedRow.setMonday(new BigDecimal("8.0"));
        tsSubmittedRow.setTuesday(new BigDecimal("8.0"));
        tsSubmittedRow.setWednesday(new BigDecimal("8.0"));
        tsSubmittedRow.setThursday(new BigDecimal("8.0"));
        tsSubmittedRow.setFriday(new BigDecimal("4.0"));
        tsSubmittedRow.setSaturday(new BigDecimal("0.0"));
        tsSubmittedRow.setSunday(new BigDecimal("0.0"));
        em.persist(tsSubmittedRow);


// ---------- APPROVED ----------
        Timesheet tsApproved = new Timesheet();
        tsApproved.setEmployee(sylvesterCat);
        tsApproved.setWeekEnding(weekEnding);
        tsApproved.setApprover(bugsBunny);
        tsApproved.setReturnComment(null);
        tsApproved.setStatus(TimesheetStatus.APPROVED);
        tsApproved.setSignature(sylvesterCat.getESignature());
        em.persist(tsApproved);

        TimesheetRow tsApprovedRow = new TimesheetRow();
        tsApprovedRow.setTimesheet(tsApproved);
        tsApprovedRow.setWorkPackage(wp2);
        tsApprovedRow.setLaborGrade(sylvesterCat.getLaborGrade());
        tsApprovedRow.setMonday(new BigDecimal("8.0"));
        tsApprovedRow.setTuesday(new BigDecimal("8.0"));
        tsApprovedRow.setWednesday(new BigDecimal("6.0"));
        tsApprovedRow.setThursday(new BigDecimal("8.0"));
        tsApprovedRow.setFriday(new BigDecimal("8.0"));
        tsApprovedRow.setSaturday(new BigDecimal("0.0"));
        tsApprovedRow.setSunday(new BigDecimal("0.0"));
        em.persist(tsApprovedRow);


// ---------- RETURNED ----------
        Timesheet tsReturned = new Timesheet();
        tsReturned.setEmployee(daffyDuck);
        tsReturned.setWeekEnding(LocalDate.of(2026, 1, 18));
        tsReturned.setApprover(bugsBunny);
        tsReturned.setReturnComment("Please correct hours for Wednesday.");
        tsReturned.setStatus(TimesheetStatus.RETURNED);
        tsReturned.setSignature(daffyDuck.getESignature());
        em.persist(tsReturned);

        TimesheetRow tsReturnedRow = new TimesheetRow();
        tsReturnedRow.setTimesheet(tsReturned);
        tsReturnedRow.setWorkPackage(wp1);
        tsReturnedRow.setLaborGrade(daffyDuck.getLaborGrade());
        tsReturnedRow.setMonday(new BigDecimal("8.0"));
        tsReturnedRow.setTuesday(new BigDecimal("8.0"));
        tsReturnedRow.setWednesday(new BigDecimal("12.0"));
        tsReturnedRow.setThursday(new BigDecimal("8.0"));
        tsReturnedRow.setFriday(new BigDecimal("8.0"));
        tsReturnedRow.setSaturday(new BigDecimal("0.0"));
        tsReturnedRow.setSunday(new BigDecimal("0.0"));
        em.persist(tsReturnedRow);

        System.out.println("[Seeder] Seed complete: LaborGrade + Signature + Employee + Project + A + children + HR/PM/RE/MEMBER.");
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
}
