package com.corejsf.Service;

import java.time.LocalDate;
import java.util.List;

import com.corejsf.Entity.WorkPackage;

public class EarnedValueAggregateInput {

    private String parentWpId;
    private LocalDate asOfDate;
    private List<LocalDate> weekEndings;
    private List<WorkPackage> childWorkPackages;

    public EarnedValueAggregateInput() { }

    public String getParentWpId() { return parentWpId; }
    public void setParentWpId(String parentWpId) { this.parentWpId = parentWpId; }

    public LocalDate getAsOfDate() { return asOfDate; }
    public void setAsOfDate(LocalDate asOfDate) { this.asOfDate = asOfDate; }

    public List<LocalDate> getWeekEndings() { return weekEndings; }
    public void setWeekEndings(List<LocalDate> weekEndings) { this.weekEndings = weekEndings; }

    public List<WorkPackage> getChildWorkPackages() { return childWorkPackages; }
    public void setChildWorkPackages(List<WorkPackage> childWorkPackages) { this.childWorkPackages = childWorkPackages; }
}
