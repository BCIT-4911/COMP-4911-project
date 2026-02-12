package com.corejsf.Entity;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
@Table(name = "Timesheet_Row")
public class TimesheetRow {

    @Id
    @Column(name = "ts_row_id")
    private Integer tsRowId;

    @Column(name = "ts_row_monday", precision = 4, scale = 1, nullable = false)
    private BigDecimal monday;

    @Column(name = "ts_row_tuesday", precision = 4, scale = 1, nullable = false)
    private BigDecimal tuesday;

    @Column(name = "ts_row_wednesday", precision = 4, scale = 1, nullable = false)
    private BigDecimal wednesday;

    @Column(name = "ts_row_thursday", precision = 4, scale = 1, nullable = false)
    private BigDecimal thursday;

    @Column(name = "ts_row_friday", precision = 4, scale = 1, nullable = false)
    private BigDecimal friday;

    @Column(name = "ts_row_saturday", precision = 4, scale = 1, nullable = false)
    private BigDecimal saturday;

    @Column(name = "ts_row_sunday", precision = 4, scale = 1, nullable = false)
    private BigDecimal sunday;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "labor_grade_id", nullable = false)
    private LaborGrade laborGrade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wp_id", nullable = false)
    private WorkPackage workPackage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ts_id", nullable = false)
    private Timesheet timesheet;
}

