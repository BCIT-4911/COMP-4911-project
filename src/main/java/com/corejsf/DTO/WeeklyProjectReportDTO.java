package com.corejsf.DTO;

import java.math.BigDecimal;
import java.util.List;


public class WeeklyProjectReportDTO {

    private String projectId;

    private String projectName;
    private List<EmployeeReportEntryDTO> employees;

    // Getters and Setters for outer class
    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public List<EmployeeReportEntryDTO> getEmployees() {
        return employees;
    }

    public void setEmployees(List<EmployeeReportEntryDTO> employees) {
        this.employees = employees;
    }

    // Inner class: one entry per employee
    public static class EmployeeReportEntryDTO {
        private int empId;
        private String empName;
        private List<WpHoursEntryDTO> workPackages;

        private BigDecimal currentWeekHours;
        private BigDecimal currentMonthHours;
        private BigDecimal projectToDateHours;

        // Getters and Setters
        public int getEmpId() {
            return empId;
        }

        public void setEmpId(int empId) {
            this.empId = empId;
        }

        public String getEmpName() {
            return empName;
        }

        public void setEmpName(String empName) {
            this.empName = empName;
        }

        public List<WpHoursEntryDTO> getWorkPackages() {
            return workPackages;
        }

        public void setWorkPackages(List<WpHoursEntryDTO> workPackages) {
            this.workPackages = workPackages;
        }

        public BigDecimal getCurrentWeekHours() {
            return currentWeekHours;
        }

        public void setCurrentWeekHours(BigDecimal currentWeekHours) {
            this.currentWeekHours = currentWeekHours;
        }

        public BigDecimal getCurrentMonthHours() {
            return currentMonthHours;
        }

        public void setCurrentMonthHours(BigDecimal currentMonthHours) {
            this.currentMonthHours = currentMonthHours;
        }

        public BigDecimal getProjectToDateHours() {
            return projectToDateHours;
        }

        public void setProjectToDateHours(BigDecimal projectToDateHours) {
            this.projectToDateHours = projectToDateHours;
        }
    }

    // Inner class: one row per work package per employee
    public static class WpHoursEntryDTO {
        private String wpId;
        private String wpName;
        private BigDecimal currentWeekHours;
        private BigDecimal currentMonthHours;
        private BigDecimal projectToDateHours;

        // Getters and Setters
        public String getWpId() {
            return wpId;
        }

        public void setWpId(String wpId) {
            this.wpId = wpId;
        }

        public String getWpName() {
            return wpName;
        }

        public void setWpName(String wpName) {
            this.wpName = wpName;
        }

        public BigDecimal getCurrentWeekHours() {
            return currentWeekHours;
        }

        public void setCurrentWeekHours(BigDecimal currentWeekHours) {
            this.currentWeekHours = currentWeekHours;
        }

        public BigDecimal getCurrentMonthHours() {
            return currentMonthHours;
        }

        public void setCurrentMonthHours(BigDecimal currentMonthHours) {
            this.currentMonthHours = currentMonthHours;
        }

        public BigDecimal getProjectToDateHours() {
            return projectToDateHours;
        }

        public void setProjectToDateHours(BigDecimal projectToDateHours) {
            this.projectToDateHours = projectToDateHours;
        }
    }
}