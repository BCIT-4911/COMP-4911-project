package ca.bcit.infosys.project;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Validation class for Project
 * 
 * @author Raymond
 * @version 1.0
 * @since 2026-02-15
 */
public final class ProjectValidation {
    public static void validateId(final String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Project ID cannot be null or empty");
        } else if (!id.matches("^[a-zA-Z0-9-]*$")) {
            throw new IllegalArgumentException("Project ID can only contain letters, numbers, and hyphens.");
        } else if (id.length() > 255) {
            throw new IllegalArgumentException("Project ID cannot be longer than 255 characters");
        }
    }

    public static void validateName(final String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be null or empty");
        } else if (!name.matches("^[a-zA-Z0-9\s-]*$")) {
            throw new IllegalArgumentException("Project name can only contain letters, numbers, spaces, and hyphens.");
        } else if (name.length() > 255) {
            throw new IllegalArgumentException("Project name cannot be longer than 255 characters");
        }
    }

    public static void validateDescription(final String description) {
        if (description == null || description.isEmpty()) {
            throw new IllegalArgumentException("Project description cannot be null or empty");
        }
    }

    public static void validateStartDate(final LocalDate startDate) {
        // Using toLocalDate() avoids issues with time components. A start date of "today" is valid.
        if (startDate != null && startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Project start date cannot be in the past");
        }
    }

    public static void validateEndDate(final LocalDate endDate, final LocalDate startDate) {
        // This check is for new projects. An end date should not be in the past.
        if (endDate != null && endDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Project end date cannot be in the past");
        }
        
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Project end date cannot be before project start date");
        }
    }

    public static void validateDates(final LocalDate startDate, final LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Project end date cannot be before project start date");
        }
    }

    public static void validateCreatedDate(final LocalDateTime createdDate) {
        if (createdDate != null && createdDate.toLocalDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Project created date cannot be in the future");
        }
    }

    public static void validateLastModifiedDate(final LocalDateTime lastModifiedDate, final LocalDateTime createdDate) {
        if (lastModifiedDate != null && createdDate != null && lastModifiedDate.isBefore(createdDate)) {
            throw new IllegalArgumentException("Project last modified date cannot be before project created date");
        } else if (lastModifiedDate != null && lastModifiedDate.toLocalDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Project last modified date cannot be in the future");
        }
    }

    public static void validateCreatedById(final int created_by_id) {
        if (created_by_id <= 0) {
            throw new IllegalArgumentException("Project created by ID must be greater than 0");
        }
    }

    public static void validateMarkup(final BigDecimal markup) {
        if (markup == null || markup.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Project markup must be greater than 0");
        }
    }

    public static void validateProjectManagerId(final int project_manager_id) {
        if (project_manager_id <= 0) {
            throw new IllegalArgumentException("Project manager ID must be greater than 0");
        }
    }
}
