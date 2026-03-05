package com.corejsf.DTO;

import java.math.BigDecimal;

/**
 * DTO for labor grade data.
 * Used to populate dropdowns and display labor grade info.
 */
public class LaborGradeDTO {

    private Integer laborGradeId;
    private String gradeCode;
    private BigDecimal chargeRate;

    public Integer getLaborGradeId() {
        return laborGradeId;
    }

    public void setLaborGradeId(Integer laborGradeId) {
        this.laborGradeId = laborGradeId;
    }

    public String getGradeCode() {
        return gradeCode;
    }

    public void setGradeCode(String gradeCode) {
        this.gradeCode = gradeCode;
    }

    public BigDecimal getChargeRate() {
        return chargeRate;
    }

    public void setChargeRate(BigDecimal chargeRate) {
        this.chargeRate = chargeRate;
    }
}
