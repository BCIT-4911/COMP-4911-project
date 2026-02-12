package com.corejsf.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;

@Entity
@Table(name = "Rate_History")
public class RateHistory {

    @Id
    @Column(name = "rate_history_id", nullable = false)
    private Integer rateHistoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "labor_grade_id", nullable = false)
    private LaborGrade laborGrade;

    @Column(name = "charge_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal chargeRate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    public RateHistory() {
    }

    public Integer getRateHistoryId() {
        return rateHistoryId;
    }

    public void setRateHistoryId(final Integer rateHistoryId) {
        this.rateHistoryId = rateHistoryId;
    }

    public LaborGrade getLaborGrade() {
        return laborGrade;
    }

    public void setLaborGrade(final LaborGrade laborGrade) {
        this.laborGrade = laborGrade;
    }

    public BigDecimal getChargeRate() {
        return chargeRate;
    }

    public void setChargeRate(final BigDecimal chargeRate) {
        this.chargeRate = chargeRate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(final LocalDate endDate) {
        this.endDate = endDate;
    }
}
