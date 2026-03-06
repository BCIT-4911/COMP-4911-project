package com.corejsf.DTO;

import java.math.BigDecimal;
import java.util.Map;

public class WorkPackageWeeklyDTO {

    private String wpId;

    // For display in the table
    private int number;              // WP#
    private String description;      // Work Description
    private String evMethod;         // EV Method (0/100, 50/50, etc.)
    private String type;             // Type column (optional)

    // weekIndex -> dollars
    private Map<Integer, BigDecimal> bcwsByWeek;
    private Map<Integer, BigDecimal> bcwpByWeek;
    private Map<Integer, BigDecimal> acwpByWeek; // can be empty until wired

    // Optional totals (so frontend doesn’t have to sum)
    private BigDecimal totalBcws;
    private BigDecimal totalBcwp;
    private BigDecimal totalAcwp;

    public WorkPackageWeeklyDTO() { }

    // getters/setters...
    public String getWpId() { return wpId; }
    public void setWpId(String wpId) { this.wpId = wpId; }

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEvMethod() { return evMethod; }
    public void setEvMethod(String evMethod) { this.evMethod = evMethod; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Map<Integer, BigDecimal> getBcwsByWeek() { return bcwsByWeek; }
    public void setBcwsByWeek(Map<Integer, BigDecimal> bcwsByWeek) { this.bcwsByWeek = bcwsByWeek; }

    public Map<Integer, BigDecimal> getBcwpByWeek() { return bcwpByWeek; }
    public void setBcwpByWeek(Map<Integer, BigDecimal> bcwpByWeek) { this.bcwpByWeek = bcwpByWeek; }

    public Map<Integer, BigDecimal> getAcwpByWeek() { return acwpByWeek; }
    public void setAcwpByWeek(Map<Integer, BigDecimal> acwpByWeek) { this.acwpByWeek = acwpByWeek; }

    public BigDecimal getTotalBcws() { return totalBcws; }  
    public void setTotalBcws(BigDecimal totalBcws) { this.totalBcws = totalBcws; }

    public BigDecimal getTotalBcwp() { return totalBcwp; }
    public void setTotalBcwp(BigDecimal totalBcwp) { this.totalBcwp = totalBcwp; }

    public BigDecimal getTotalAcwp() { return totalAcwp; }
    public void setTotalAcwp(BigDecimal totalAcwp) { this.totalAcwp = totalAcwp; }
}

