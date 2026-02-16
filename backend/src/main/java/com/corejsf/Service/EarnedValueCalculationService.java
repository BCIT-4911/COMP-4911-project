package com.corejsf.Service;


import jakarta.ejb.Stateless;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.corejsf.DTO.WorkPackageWeeklyDTO;
import com.corejsf.Entity.WorkPackage;

@Stateless
public class EarnedValueCalculationService {

    public List<WorkPackageWeeklyDTO> calculate(final EarnedValueAggregateInput input) {
        if (input == null)
            throw new IllegalArgumentException("input is required");

        final List<LocalDate> weekEndings = input.getWeekEndings();
        final LocalDate asOf = input.getAsOfDate();
        final List<WorkPackage> children = input.getChildWorkPackages();

        int fallbackIndex = 1;
        final java.util.ArrayList<WorkPackageWeeklyDTO> out = new java.util.ArrayList<>();

        for (final WorkPackage wp : children) {
            out.add(computeForChild(wp, weekEndings, asOf, fallbackIndex));
            fallbackIndex++;
        }

        return out;
    }

    private WorkPackageWeeklyDTO computeForChild(final WorkPackage wp,
                                                 final List<LocalDate> reportWeekEndings,
                                                 final LocalDate asOfDate,
                                                 final int fallbackIndex) {

        final BigDecimal bac = nz(wp.getBac());
        final BigDecimal pct = nz(wp.getPercentComplete()); // 0..100

        final LocalDate wpStart = (wp.getPlanStartDate() == null) ? reportWeekEndings.get(0) : wp.getPlanStartDate();
        final LocalDate wpEnd = (wp.getPlanEndDate() == null) ? reportWeekEndings.get(reportWeekEndings.size() - 1) : wp.getPlanEndDate();

        final LocalDate activeStartWe = toWeekEndingSunday(wpStart);
        final LocalDate activeEndWe = toWeekEndingSunday(wpEnd);

        final Map<Integer, BigDecimal> bcws = initZeroMap(reportWeekEndings.size());
        final Map<Integer, BigDecimal> bcwp = initZeroMap(reportWeekEndings.size());
        final Map<Integer, BigDecimal> acwp = initZeroMap(reportWeekEndings.size()); // placeholder

        final boolean[] active = new boolean[reportWeekEndings.size()];
        int activeWeeks = 0;

        for (int i = 0; i < reportWeekEndings.size(); i++) {
            final LocalDate we = reportWeekEndings.get(i);
            final boolean isActive = (!we.isBefore(activeStartWe)) && (!we.isAfter(activeEndWe));
            active[i] = isActive;
            if (isActive) activeWeeks++;
        }

        allocateBcwsStraightLine(bac, activeWeeks, active, bcws);
        allocateBcwpFromPercentComplete(bac, pct, asOfDate, reportWeekEndings, bcws, bcwp);

        final WorkPackageWeeklyDTO dto = new WorkPackageWeeklyDTO();
        dto.setWpId(wp.getWpId());
        dto.setNumber(extractNumber(wp.getWpId(), fallbackIndex));
        dto.setDescription(wp.getDescription());

        dto.setEvMethod("% Complete"); // for now
        dto.setType("WP");

        dto.setBcwsByWeek(bcws);
        dto.setBcwpByWeek(bcwp);
        dto.setAcwpByWeek(acwp);

        dto.setTotalBcws(sumMap(bcws));
        dto.setTotalBcwp(sumMap(bcwp));
        dto.setTotalAcwp(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        return dto;
    }

    private void allocateBcwsStraightLine(final BigDecimal bac,
                                          final int activeWeeks,
                                          final boolean[] active,
                                          final Map<Integer, BigDecimal> bcwsByWeek) {

        if (activeWeeks <= 0 || bac.compareTo(BigDecimal.ZERO) <= 0) return;

        final BigDecimal weekly = bac.divide(new BigDecimal(activeWeeks), 2, RoundingMode.HALF_UP);

        BigDecimal assigned = BigDecimal.ZERO;
        int seen = 0;

        for (int idx = 0; idx < active.length; idx++) {
            if (!active[idx]) continue;

            seen++;
            BigDecimal val = weekly;

            if (seen == activeWeeks) {
                val = bac.subtract(assigned);
            }

            assigned = assigned.add(val);
            bcwsByWeek.put(idx + 1, val);
        }
    }

    private void allocateBcwpFromPercentComplete(final BigDecimal bac,
                                                 final BigDecimal pct,
                                                 final LocalDate asOfDate,
                                                 final List<LocalDate> reportWeekEndings,
                                                 final Map<Integer, BigDecimal> bcwsByWeek,
                                                 final Map<Integer, BigDecimal> bcwpByWeek) {

        if (bac.compareTo(BigDecimal.ZERO) <= 0) return;

        final BigDecimal earnedTotal = bac.multiply(pct)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        final LocalDate asOfWe = toWeekEndingSunday(asOfDate);

        BigDecimal bcwsToDate = BigDecimal.ZERO;
        for (int i = 1; i <= reportWeekEndings.size(); i++) {
            final LocalDate we = reportWeekEndings.get(i - 1);
            if (!we.isAfter(asOfWe)) {
                bcwsToDate = bcwsToDate.add(bcwsByWeek.get(i));
            }
        }

        if (bcwsToDate.compareTo(BigDecimal.ZERO) == 0) {
            for (int i = 1; i <= reportWeekEndings.size(); i++) {
                if (bcwsByWeek.get(i).compareTo(BigDecimal.ZERO) > 0) {
                    bcwpByWeek.put(i, earnedTotal);
                    break;
                }
            }
            return;
        }

        BigDecimal assigned = BigDecimal.ZERO;
        int lastEarnWeek = -1;

        for (int i = 1; i <= reportWeekEndings.size(); i++) {
            final LocalDate we = reportWeekEndings.get(i - 1);
            if (we.isAfter(asOfWe)) continue;

            final BigDecimal plan = bcwsByWeek.get(i);
            if (plan.compareTo(BigDecimal.ZERO) == 0) continue;

            lastEarnWeek = i;

            final BigDecimal portion = plan.divide(bcwsToDate, 8, RoundingMode.HALF_UP);
            final BigDecimal earned = earnedTotal.multiply(portion).setScale(2, RoundingMode.HALF_UP);

            assigned = assigned.add(earned);
            bcwpByWeek.put(i, earned);
        }

        if (lastEarnWeek != -1) {
            final BigDecimal diff = earnedTotal.subtract(assigned);
            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                bcwpByWeek.put(lastEarnWeek, bcwpByWeek.get(lastEarnWeek).add(diff));
            }
        }
    }

    // Sunday week-ending
    private LocalDate toWeekEndingSunday(final LocalDate date) {
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    // Helpers
    private Map<Integer, BigDecimal> initZeroMap(final int size) {
        final Map<Integer, BigDecimal> m = new LinkedHashMap<>();
        for (int i = 1; i <= size; i++) {
            m.put(i, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
        return m;
    }

    private BigDecimal sumMap(final Map<Integer, BigDecimal> map) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal v : map.values()) total = total.add(v);
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private int extractNumber(final String wpId, final int fallback) {
        if (wpId == null) return fallback;
        try {
            final String digits = wpId.replaceAll("\\D", "");
            if (digits.isBlank()) return fallback;
            return Integer.parseInt(digits);
        } catch (Exception e) {
            return fallback;
        }
    }

    private BigDecimal nz(final BigDecimal v) {
        return (v == null) ? BigDecimal.ZERO : v;
    }

}