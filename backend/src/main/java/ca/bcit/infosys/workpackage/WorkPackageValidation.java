package ca.bcit.infosys.workpackage;

import com.corejsf.Entity.WorkPackage;
import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * Validation class for WorkPackage.
 */
public final class WorkPackageValidation {

    /**
     * Validates the work package.
     * 
     * @param wp The work package to validate.
     * @throws IllegalArgumentException if the work package is null.
     */
    public static void validate(WorkPackage wp) {
        if (wp == null) {
            throw new IllegalArgumentException("WorkPackage cannot be null.");
        }
        validateId(wp.getWpId());
        validateName(wp.getWpName());
        validateProjectId(wp.getProjId());
        validateDates(wp.getPlanStartDate(), wp.getPlanEndDate());
        validateBudget(wp.getBudgetedEffort());
    }

    /**
     * Validates the start and end dates of a work package.
     * 
     * @param startDate The start date of the work package.
     * @param endDate   The end date of the work package.
     * @throws IllegalArgumentException if the end date is before the start date.
     */
    public static void validateDates(final LocalDate startDate, final LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Work Package end date cannot be before start date.");
        }
    }

    /**
     * Validates the budget of a work package.
     * 
     * @param budget The budget of the work package.
     * @throws IllegalArgumentException if the budget is negative.
     */
    public static void validateBudget(final BigDecimal budget) {
        if (budget != null && budget.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Work Package budget cannot be negative.");
        }
    }

    /**
     * Validates the ID of a work package.
     * 
     * @param id The ID of the work package to validate.
     * @throws IllegalArgumentException if the ID is null or empty.
     */
    public static void validateId(final String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Work Package ID cannot be null or empty.");
        }

        if (id.length() > 255) {
            throw new IllegalArgumentException("Work Package ID cannot be longer than 255 characters.");
        }

        if (!id.matches("^[a-zA-Z0-9.-]*$")) {
            throw new IllegalArgumentException(
                    "Work Package ID can only contain letters, numbers, periods, and hyphens.");
        }
    }

    /**
     * Validates the name of a work package.
     * 
     * @param name The name of the work package to validate.
     * @throws IllegalArgumentException if the name is null or empty.
     */
    public static void validateName(final String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Work Package name cannot be null or empty.");
        }
        if (name.length() > 255) {
            throw new IllegalArgumentException("Work Package name cannot be longer than 255 characters.");
        }
    }

    /**
     * Validates the project ID of a work package.
     * 
     * @param projId The project ID of the work package to validate.
     * @throws IllegalArgumentException if the project ID is null or empty.
     */
    public static void validateProjectId(final String projId) {
        if (projId == null || projId.isEmpty()) {
            throw new IllegalArgumentException("Project ID for Work Package cannot be null or empty.");
        }
    }
}
