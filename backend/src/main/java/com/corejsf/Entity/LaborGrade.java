package com.corejsf.Entity;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
@Table(name = "Labor_Grade")
public class LaborGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "labor_grade_id", nullable = false)
    private Integer laborGradeId;

    @Column(name = "grade_code", nullable = false, length = 2)
    private String gradeCode;

    @Column(name = "charge_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal chargeRate;

    public LaborGrade() {
    }

    public Integer getLaborGradeId() {
        return laborGradeId;
    }

    public void setLaborGradeId(final Integer laborGradeId) {
        this.laborGradeId = laborGradeId;
    }

    public String getGradeCode() {
        return gradeCode;
    }

    public void setGradeCode(final String gradeCode) {
        this.gradeCode = gradeCode;
    }

    public BigDecimal getChargeRate() {
        return chargeRate;
    }

    public void setChargeRate(final BigDecimal chargeRate) {
        this.chargeRate = chargeRate;
    }
}
