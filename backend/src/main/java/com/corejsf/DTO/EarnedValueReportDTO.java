package com.corejsf.DTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class EarnedValueReportDTO {

    private int weekCount;

    private int selectedProjectId;
    private int selectedControlAccountId;

    private List<OptionDTO> projects;
    private List<OptionDTO> controlAccounts;

    private String controlAccountTitle;
    private String controlAccountManager;
    private BigDecimal bac;

    private List<WorkPackageWeeklyDTO> workPackages;

    // Totals per week (weekIndex -> value)
    private Map<Integer, BigDecimal> totalBcwsByWeek;
    private Map<Integer, BigDecimal> totalBcwpByWeek;
    private Map<Integer, BigDecimal> totalAcwpByWeek;

    private Map<Integer, BigDecimal> svByWeek; // BCWP - BCWS
    private Map<Integer, BigDecimal> cvByWeek; // BCWP - ACWP

    public EarnedValueReportDTO() { }

    // getters/setters...
    public int getWeekCount() { return weekCount; }
    public void setWeekCount(int weekCount) { this.weekCount = weekCount; }

    public int getSelectedProjectId() { return selectedProjectId; }
    public void setSelectedProjectId(int selectedProjectId) { this.selectedProjectId = selectedProjectId; }

    public int getSelectedControlAccountId() { return selectedControlAccountId; }
    public void setSelectedControlAccountId(int selectedControlAccountId) { this.selectedControlAccountId = selectedControlAccountId; }

    public List<OptionDTO> getProjects() { return projects; }
    public void setProjects(List<OptionDTO> projects) { this.projects = projects; }

    public List<OptionDTO> getControlAccounts() { return controlAccounts; }
    public void setControlAccounts(List<OptionDTO> controlAccounts) { this.controlAccounts = controlAccounts; }

    public String getControlAccountTitle() { return controlAccountTitle; }
    public void setControlAccountTitle(String controlAccountTitle) { this.controlAccountTitle = controlAccountTitle; }

    public String getControlAccountManager() { return controlAccountManager; }
    public void setControlAccountManager(String controlAccountManager) { this.controlAccountManager = controlAccountManager; }

    public BigDecimal getBac() { return bac; }
    public void setBac(BigDecimal bac) { this.bac = bac; }

    public List<WorkPackageWeeklyDTO> getWorkPackages() { return workPackages; }
    public void setWorkPackages(List<WorkPackageWeeklyDTO> workPackages) { this.workPackages = workPackages; }

    public Map<Integer, BigDecimal> getTotalBcwsByWeek() { return totalBcwsByWeek; }
    public void setTotalBcwsByWeek(Map<Integer, BigDecimal> totalBcwsByWeek) { this.totalBcwsByWeek = totalBcwsByWeek; }

    public Map<Integer, BigDecimal> getTotalBcwpByWeek() { return totalBcwpByWeek; }
    public void setTotalBcwpByWeek(Map<Integer, BigDecimal> totalBcwpByWeek) { this.totalBcwpByWeek = totalBcwpByWeek; }

    public Map<Integer, BigDecimal> getTotalAcwpByWeek() { return totalAcwpByWeek; }
    public void setTotalAcwpByWeek(Map<Integer, BigDecimal> totalAcwpByWeek) { this.totalAcwpByWeek = totalAcwpByWeek; }

    public Map<Integer, BigDecimal> getSvByWeek() { return svByWeek; }
    public void setSvByWeek(Map<Integer, BigDecimal> svByWeek) { this.svByWeek = svByWeek; }

    public Map<Integer, BigDecimal> getCvByWeek() { return cvByWeek; }
    public void setCvByWeek(Map<Integer, BigDecimal> cvByWeek) { this.cvByWeek = cvByWeek; }

    
}