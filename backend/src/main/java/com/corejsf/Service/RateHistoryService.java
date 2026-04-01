package com.corejsf.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.corejsf.Entity.LaborGrade;
import com.corejsf.Entity.RateHistory;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class RateHistoryService {

    @PersistenceContext(unitName = "project-management-pu")
    private EntityManager em;

    public BigDecimal getEffectiveRate(final LaborGrade laborGrade, final LocalDate targetDate) {
        if (laborGrade == null) {
            throw new IllegalArgumentException("Labor grade cannot be null.");
        }

        if (targetDate == null) {
            throw new IllegalArgumentException("Target date cannot be null.");
        }

        List<RateHistory> matches = em.createQuery("""
                SELECT rh
                FROM RateHistory rh
                WHERE rh.laborGrade = :laborGrade
                  AND rh.startDate <= :targetDate
                  AND (rh.endDate IS NULL OR rh.endDate >= :targetDate)
                ORDER BY rh.startDate DESC
                """, RateHistory.class)
            .setParameter("laborGrade", laborGrade)
            .setParameter("targetDate", targetDate)
            .setMaxResults(1)
            .getResultList();

        if (matches.isEmpty()) {
            return laborGrade.getChargeRate();
        }

        return matches.get(0).getChargeRate();
    }
}
