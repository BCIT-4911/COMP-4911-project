package com.corejsf.Service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.corejsf.Entity.SystemRole;
import com.corejsf.Entity.Employee;

public class RebacServiceTest {

    private final RebacService rebacService = new RebacService();

    @Test
    void operationsManagerCanCreateProject() {
        assertTrue(rebacService.canCreateProject(SystemRole.OPERATIONS_MANAGER));
    }

    @Test
    void hrCannotCreateProject() {
        assertFalse(rebacService.canCreateProject(SystemRole.HR));
    }

    @Test
    void employeeCannotCreateProject() {
        assertFalse(rebacService.canCreateProject(SystemRole.EMPLOYEE));
    }

    @Test
    void hrCanManageEmployees() {
        assertTrue(rebacService.canManageEmployees(SystemRole.HR));
    }

    @Test
    void operationsManagerCanManageEmployees() {
        assertTrue(rebacService.canManageEmployees(SystemRole.OPERATIONS_MANAGER));
    }

    @Test
    void employeeCanManageEmployees() {
        assertTrue(rebacService.canManageEmployees(SystemRole.EMPLOYEE));
    }

    @Test
    void isOperationsManagerReturnsTrueForOperationsManager() {
        Employee emp = new Employee();
        emp.setSystemRole(SystemRole.OPERATIONS_MANAGER);
        assertTrue(rebacService.isOperationsManager(emp));
    }

    @Test
    void isOperationsManagerReturnsFalseForHr() {
        Employee emp = new Employee();
        emp.setSystemRole(SystemRole.HR);
        assertFalse(rebacService.isOperationsManager(emp));
    }

    @Test
    void isOperationsManagerReturnsFalseForEmployee() {
        Employee emp = new Employee();
        emp.setSystemRole(SystemRole.EMPLOYEE);
        assertFalse(rebacService.isOperationsManager(emp));
    }

    @Test
    void isHrReturnsTrueForHrEmployee() {
        Employee emp = new Employee();
        emp.setSystemRole(SystemRole.HR);
        assertTrue(rebacService.isHr(emp));
    }

    @Test
    void isHrReturnFalseForNonHrEmployee() {
        Employee emp = new Employee();
        emp.setSystemRole(SystemRole.EMPLOYEE);
        assertFalse(rebacService.isHr(emp));
    }

    @Test
    void isHrReturnsFalseForNullEmployee() {
        assertFalse(rebacService.isHr(null));
    }

    @Test
    void employeeObjectCanCreateProjectWhenSetToOperationsManager() {
        Employee emp = new Employee();
        emp.setSystemRole(SystemRole.OPERATIONS_MANAGER);
        assertTrue(rebacService.canCreateProject(emp));
    }

    @Test
    void employeeObjectCannotCreateProjectWhenSetToHr() {
        Employee emp = new Employee();
        emp.setSystemRole(SystemRole.HR);
        assertFalse(rebacService.canCreateProject(emp));
    }

    @Test
    void nullEmployeeCannotCreateProject() {
        assertFalse(rebacService.canCreateProject((Employee) null));
    }

    @Test
    void employeeObjectCanManageEmployeeWhenSetToHr() {
        Employee emp = new Employee();
        emp.setSystemRole(SystemRole.HR);
        assertTrue(rebacService.canManageEmployees(emp));
    }

    @Test
    void employeeObjectCanManageEmployeeWhenSetToOperationsManager() {
        Employee emp = new Employee();
        emp.setSystemRole(SystemRole.OPERATIONS_MANAGER);
        assertTrue(rebacService.canManageEmployees(emp));
    }

    @Test
    void nullEmployeeCannotManageEmployee() {
        assertFalse(rebacService.canManageEmployees((Employee) null));
    }

    @Test
    void adminCanWriteEmployees() {
        assertTrue(rebacService.canWriteEmployees(SystemRole.ADMIN));
    }

    @Test
    void hrCanWriteEmployees() {
        assertTrue(rebacService.canWriteEmployees(SystemRole.HR));
    }

    @Test
    void operationsManagerCannotWriteEmployees() {
        assertFalse(rebacService.canWriteEmployees(SystemRole.OPERATIONS_MANAGER));
    }

    @Test
    void employeeCannotWriteEmployees() {
        assertFalse(rebacService.canWriteEmployees(SystemRole.EMPLOYEE));
    }

    @Test
    void employeeObjectCanWriteWhenAdmin() {
        Employee emp = new Employee();
        emp.setSystemRole(SystemRole.ADMIN);
        assertTrue(rebacService.canWriteEmployees(emp));
    }

    @Test
    void employeeObjectCanWriteWhenHr() {
        Employee emp = new Employee();
        emp.setSystemRole(SystemRole.HR);
        assertTrue(rebacService.canWriteEmployees(emp));
    }

    @Test
    void employeeObjectCannotWriteWhenOperationsManager() {
        Employee emp = new Employee();
        emp.setSystemRole(SystemRole.OPERATIONS_MANAGER);
        assertFalse(rebacService.canWriteEmployees(emp));
    }

    @Test
    void nullEmployeeCannotWriteEmployees() {
        assertFalse(rebacService.canWriteEmployees((Employee) null));
    }

    // ---- canManageLaborGrades (SystemRole overload) ----

    @Test
    void adminCanManageLaborGrades() {
        assertTrue(rebacService.canManageLaborGrades(SystemRole.ADMIN));
    }

    @Test
    void operationsManagerCanManageLaborGrades() {
        assertTrue(rebacService.canManageLaborGrades(SystemRole.OPERATIONS_MANAGER));
    }

    @Test
    void hrCannotManageLaborGrades() {
        assertFalse(rebacService.canManageLaborGrades(SystemRole.HR));
    }

    @Test
    void employeeCannotManageLaborGrades() {
        assertFalse(rebacService.canManageLaborGrades(SystemRole.EMPLOYEE));
    }

    // ---- ADMIN role coverage for canCreateProject ----

    @Test
    void adminCanCreateProject() {
        assertTrue(rebacService.canCreateProject(SystemRole.ADMIN));
    }

    @Test
    void adminEmployeeCanCreateProject() {
        Employee emp = new Employee();
        emp.setSystemRole(SystemRole.ADMIN);
        assertTrue(rebacService.canCreateProject(emp));
    }

    // ---- ADMIN role coverage for canManageEmployees ----

    @Test
    void adminCanManageEmployees() {
        assertTrue(rebacService.canManageEmployees(SystemRole.ADMIN));
    }

    @Test
    void adminEmployeeCanManageEmployees() {
        Employee emp = new Employee();
        emp.setSystemRole(SystemRole.ADMIN);
        assertTrue(rebacService.canManageEmployees(emp));
    }
}