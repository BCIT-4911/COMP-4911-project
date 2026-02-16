package com.corejsf.Service;

import jakarta.inject.Inject;
import jakarta.ejb.Stateless;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;


import com.corejsf.Pojo.DateRange;
import com.corejsf.Repo.WorkPackageRepo;
import com.corejsf.Entity.WorkPackage;


@Stateless
public class EarnedValueAggregationService {

    @Inject
    private WorkPackageRepo workPackageRepo;

    public EarnedValueAggregateInput aggregateForParent(final String parentWpId, final LocalDate asOfDate) {
        final LocalDate asOf = (asOfDate == null) ? LocalDate.now() : asOfDate;

        final WorkPackage parent = loadParent(parentWpId);
        final List<WorkPackage> children = workPackageRepo.findDirectChildren(parentWpId);

        final DateRange range = resolveReportRange(parent, children, asOf);
        final List<LocalDate> weekEndings = weekEndingSundays(range.getStart(), range.getEnd());

        EarnedValueAggregateInput input = new EarnedValueAggregateInput();
        input.setParentWpId(parentWpId);
        input.setAsOfDate(asOf);
        input.setWeekEndings(weekEndings);
        input.setChildWorkPackages(children);
        return input;
    }

    private WorkPackage loadParent(final String parentWpId) {
        final WorkPackage parent = workPackageRepo.findById(parentWpId);
        if (parent == null) {
            throw new IllegalArgumentException("Parent WorkPackage not found: " + parentWpId);
        }
        return parent;
    }

    private DateRange resolveReportRange(final WorkPackage parent,
                                        final List<WorkPackage> children,
                                        final LocalDate asOf) {

        LocalDate start = parent.getPlanStartDate();
        LocalDate end = parent.getPlanEndDate();

        if ((start == null || end == null) && !children.isEmpty()) {
            start = children.stream()
                    .map(WorkPackage::getPlanStartDate)
                    .filter(d -> d != null)
                    .min(LocalDate::compareTo)
                    .orElse(asOf);

            end = children.stream()
                    .map(WorkPackage::getPlanEndDate)
                    .filter(d -> d != null)
                    .max(LocalDate::compareTo)
                    .orElse(asOf);
        }

        if (start == null) start = asOf;
        if (end == null) end = asOf;

        if (end.isBefore(start)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        return new DateRange(start, end);
    }

    private LocalDate toWeekEndingSunday(final LocalDate date) {
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    private List<LocalDate> weekEndingSundays(final LocalDate start, final LocalDate end) {
        LocalDate s = toWeekEndingSunday(start);
        LocalDate e = toWeekEndingSunday(end);

        final java.util.ArrayList<LocalDate> weeks = new java.util.ArrayList<>();
        LocalDate cur = s;

        while (!cur.isAfter(e)) {
            weeks.add(cur);
            cur = cur.plusWeeks(1);
        }
        return weeks;
    }
    
}
