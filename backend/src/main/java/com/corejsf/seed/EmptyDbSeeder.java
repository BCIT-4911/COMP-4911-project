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

        if (em.find(WorkPackage.class, "CA-1") != null) {
            System.out.println("[Seeder] CA-1 exists, skipping.");
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

        Employee admin = em.find(Employee.class, 100);
        if (admin == null) {
            admin = new Employee();
            admin.setEmpFirstName("Wile");
            admin.setEmpLastName("Coyote");
            admin.setEmpPassword(BCrypt.hashpw("password", BCrypt.gensalt()));

            admin.setSystemRole(SystemRole.OPERATIONS_MANAGER); // enum
            admin.setESignature(sig);
            admin.setLaborGrade(lg);

            admin.setSupervisor(null);
            admin.setVacationSickBalance(new BigDecimal("40.00"));
            admin.setExpectedWeeklyHours(new BigDecimal("40.0"));

            em.persist(admin);
        }

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

        WorkPackage parent = new WorkPackage();
        parent.setWpId("CA-1");
        parent.setWpName("Control Account A");
        parent.setDescription("Summary WP used as Control Account");
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
        parent.setBac(new BigDecimal("0.00"));
        parent.setPercentComplete(new BigDecimal("0.00"));

        parent.setCreatedDate(LocalDateTime.now());
        parent.setModifiedDate(LocalDateTime.now());
        parent.setCreatedBy(admin);
        parent.setModifiedBy(admin);

        em.persist(parent);
        WorkPackageAssignment wpaAdminCa1 = new WorkPackageAssignment();
        wpaAdminCa1.setEmployee(admin);
        wpaAdminCa1.setWorkPackage(parent);
        wpaAdminCa1.setAssignmentDate(LocalDate.now());
        wpaAdminCa1.setWpRole(WpRole.RE);
        em.persist(wpaAdminCa1);

        createChild("WP-1", "Procure Anvil", proj, parent, admin,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                new BigDecimal("1500.00"), new BigDecimal("0.00"));

        createChild("WP-2", "Paint Fake Tunnel", proj, parent, admin,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 14),
                new BigDecimal("1000.00"), new BigDecimal("50.00"));

        createChild("WP-3", "Build Road", proj, parent, admin,
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 1),
                new BigDecimal("3000.00"), new BigDecimal("35.00"));

        Employee roadRunner = createEmployee("Road", "Runner", SystemRole.HR, admin, lg);
        Employee bugsBunny = createEmployee("Bugs", "Bunny", SystemRole.EMPLOYEE, admin, lg);
        Employee daffyDuck = createEmployee("Daffy", "Duck", SystemRole.EMPLOYEE, bugsBunny, lg);
        Employee tweetyBird = createEmployee("Tweety", "Bird", SystemRole.EMPLOYEE, bugsBunny, lg);

        ProjectAssignment paBugs = new ProjectAssignment();
        paBugs.setEmployee(bugsBunny);
        paBugs.setProject(proj);
        paBugs.setAssignmentDate(LocalDate.now());
        paBugs.setProjectRole(ProjectRole.PM);
        em.persist(paBugs);

        proj.setProjectManager(bugsBunny);
        em.merge(proj);

        WorkPackage wp1 = em.find(WorkPackage.class, "WP-1");
        WorkPackage wp2 = em.find(WorkPackage.class, "WP-2");
        if (wp1 != null) {
            WorkPackageAssignment wpaDaffy = new WorkPackageAssignment();
            wpaDaffy.setEmployee(daffyDuck);
            wpaDaffy.setWorkPackage(wp1);
            wpaDaffy.setAssignmentDate(LocalDate.now());
            wpaDaffy.setWpRole(WpRole.MEMBER);
            em.persist(wpaDaffy);
        }
        if (wp2 != null) {
            WorkPackageAssignment wpaTweety = new WorkPackageAssignment();
            wpaTweety.setEmployee(tweetyBird);
            wpaTweety.setWorkPackage(wp2);
            wpaTweety.setAssignmentDate(LocalDate.now());
            wpaTweety.setWpRole(WpRole.MEMBER);
            em.persist(wpaTweety);
        }

        System.out.println("[Seeder] Seed complete: LaborGrade + Signature + Employee + Project + CA-1 + children + HR/PM/RE/MEMBER.");
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
        WorkPackageAssignment wpa = new WorkPackageAssignment();
        wpa.setEmployee(emp);
        wpa.setWorkPackage(child);
        wpa.setAssignmentDate(LocalDate.now());
        wpa.setWpRole(WpRole.RE);
        em.persist(wpa);
    }
}
