package com.corejsf.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class WorkPackageMonthlyPerformanceDTO {

    private String wpId;
    private String wpName;
    private String projectId;
    private LocalDate asOfDate;
    private BigDecimal bac;
    private BigDecimal etc;
    private BigDecimal eac;
    private BigDecimal vac;
    private List<String> months;
    private List<BigDecimal> bcwsByMonth;
    private List<BigDecimal> bcwpByMonth;
    private List<BigDecimal> acwpByMonth;
    private List<BigDecimal> svByMonth;
    private List<BigDecimal> cvByMonth;

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

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public void setAsOfDate(LocalDate asOfDate) {
        this.asOfDate = asOfDate;
    }

    public BigDecimal getBac() {
        return bac;
    }

    public void setBac(BigDecimal bac) {
        this.bac = bac;
    }

    public BigDecimal getEtc() {
        return etc;
    }

    public void setEtc(BigDecimal etc) {
        this.etc = etc;
    }

    public BigDecimal getEac() {
        return eac;
    }

    public void setEac(BigDecimal eac) {
        this.eac = eac;
    }

    public BigDecimal getVac() {
        return vac;
    }

    public void setVac(BigDecimal vac) {
        this.vac = vac;
    }

    public List<String> getMonths() {
        return months;
    }

    public void setMonths(List<String> months) {
        this.months = months;
    }

    public List<BigDecimal> getBcwsByMonth() {
        return bcwsByMonth;
    }

    public void setBcwsByMonth(List<BigDecimal> bcwsByMonth) {
        this.bcwsByMonth = bcwsByMonth;
    }

    public List<BigDecimal> getBcwpByMonth() {
        return bcwpByMonth;
    }

    public void setBcwpByMonth(List<BigDecimal> bcwpByMonth) {
        this.bcwpByMonth = bcwpByMonth;
    }

    public List<BigDecimal> getAcwpByMonth() {
        return acwpByMonth;
    }

    public void setAcwpByMonth(List<BigDecimal> acwpByMonth) {
        this.acwpByMonth = acwpByMonth;
    }

    public List<BigDecimal> getSvByMonth() {
        return svByMonth;
    }

    public void setSvByMonth(List<BigDecimal> svByMonth) {
        this.svByMonth = svByMonth;
    }

    public List<BigDecimal> getCvByMonth() {
        return cvByMonth;
    }

    public void setCvByMonth(List<BigDecimal> cvByMonth) {
        this.cvByMonth = cvByMonth;
    }
}
