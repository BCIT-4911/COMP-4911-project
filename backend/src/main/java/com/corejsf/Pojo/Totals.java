package com.corejsf.Pojo;
import java.math.BigDecimal;
import java.util.Map;


public final class Totals {
    private Map<Integer, BigDecimal> totalBcws;
    private Map<Integer, BigDecimal> totalBcwp;
    private Map<Integer, BigDecimal> sv;
    private Map<Integer, BigDecimal> cv;

    public Map<Integer, BigDecimal> getTotalBcws() { return totalBcws; }
    public void setTotalBcws(Map<Integer, BigDecimal> totalBcws) { this.totalBcws = totalBcws; }

    public Map<Integer, BigDecimal> getTotalBcwp() { return totalBcwp; }
    public void setTotalBcwp(Map<Integer, BigDecimal> totalBcwp) { this.totalBcwp = totalBcwp; }

    public Map<Integer, BigDecimal> getSv() { return sv; }
    public void setSv(Map<Integer, BigDecimal> sv) { this.sv = sv; }

    public Map<Integer, BigDecimal> getCv() { return cv; }
    public void setCv(Map<Integer, BigDecimal> cv) { this.cv = cv; }

}
