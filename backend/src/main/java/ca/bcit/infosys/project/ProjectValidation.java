package ca.bcit.infosys.project;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Validation class for Project
 * 
 * @author Raymond
 * @author Russell
 * @version 1.0
 * @since 2026-02-15
 */
public final class ProjectValidation {

    /**
     * Validates the ID of a project.
     * 
     * @param id The ID of the project to validate.
     * @throws IllegalArgumentException if the ID is null, empty, contains invalid
     *                                  characters, or is too long.
     */
    public static void validateId(final String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Project ID cannot be null or empty");
        } else if (!id.matches("^[a-zA-Z0-9-]*$")) {
            throw new IllegalArgumentException("Project ID can only contain letters, numbers, and hyphens.");
        } else if (id.length() > 255) {
            throw new IllegalArgumentException("Project ID cannot be longer than 255 characters");
        }
    }

    /**
     * Validates the name of a project.
     * 
     * @param name The name of the project to validate.
     * @throws IllegalArgumentException if the name is null, empty, contains invalid
     *                                  characters, or is too long.
     */
    public static void validateName(final String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be null or empty");
        } else if (!name.matches("^[a-zA-Z0-9\s-]*$")) {
            throw new IllegalArgumentException("Project name can only contain letters, numbers, spaces, and hyphens.");
        } else if (name.length() > 255) {
            throw new IllegalArgumentException("Project name cannot be longer than 255 characters");
        }
    }

    /**
     * Validates the discription of a project.
     * 
     * @param description The discription of the project to validate.
     * @throws IllegalArgumentException if the discription is null or empty.
     */
    public static void validateDescription(final String description) {
        if (description == null || description.isEmpty()) {
            throw new IllegalArgumentException("Project description cannot be null or empty");
        }
    }

    /**
     * Validates the start date of a project.
     * 
     * @param startDate The start date of the project to validate.
     * @throws IllegalArgumentException if the start date is null or in the past.
     */
    public static void validateStartDate(final LocalDate startDate) {
        if (startDate != null && startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Project start date cannot be in the past");
        }
    }

    /**
     * Validates the end date of a project.
     * 
     * @param endDate   The end date of the project to validate.
     * @param startDate The start date of the project to validate.
     * @throws IllegalArgumentException if the end date is null or in the past.
     */
    public static void validateEndDate(final LocalDate endDate, final LocalDate startDate) {
        if (endDate != null && endDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Project end date cannot be in the past");
        }

        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Project end date cannot be before project start date");
        }
    }

    /**
     * Validates the start date and end date of a project.
     * 
     * @param startDate The start date of the project to validate.
     * @param endDate   The end date of the project to validate.
     * @throws IllegalArgumentException if the start date is null or in the past.
     */
    public static void validateDates(final LocalDate startDate, final LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Project end date cannot be before project start date");
        }
    }

    /**
     * Validates the created date of a project.
     * 
     * @param createdDate The created date of the project to validate.
     * @throws IllegalArgumentException if the created date is null or in the
     *                                  future.
     */
    public static void validateCreatedDate(final LocalDateTime createdDate) {
        if (createdDate != null && createdDate.toLocalDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Project created date cannot be in the future");
        }
    }

    /**
     * Validates the last modified date of a project.
     * 
     * @param lastModifiedDate The last modified date of the project to validate.
     * @param createdDate      The created date of the project to validate.
     * @throws IllegalArgumentException if the last modified date is null or in the
     *                                  future.
     */
    public static void validateLastModifiedDate(final LocalDateTime lastModifiedDate, final LocalDateTime createdDate) {
        if (lastModifiedDate != null && createdDate != null && lastModifiedDate.isBefore(createdDate)) {
            throw new IllegalArgumentException("Project last modified date cannot be before project created date");
        } else if (lastModifiedDate != null && lastModifiedDate.toLocalDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Project last modified date cannot be in the future");
        }
    }

    /**
     * Validates the created by ID of a project.
     * 
     * @param created_by_id The created by ID of the project to validate.
     * @throws IllegalArgumentException if the created by ID is less than or equal
     *                                  to 0.
     */
    public static void validateCreatedById(final int created_by_id) {
        if (created_by_id <= 0) {
            throw new IllegalArgumentException("Project created by ID must be greater than 0");
        }
    }

    /**
     * Validates the markup of a project.
     * 
     * @param markup The markup of the project to validate.
     * @throws IllegalArgumentException if the markup is null or less than or equal
     *                                  to 0.
     */
    public static void validateMarkup(final BigDecimal markup) {
        if (markup == null || markup.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Project markup must be greater than 0");
        }
    }

    /**
     * Validates the project manager ID of a project.
     * 
     * @param project_manager_id The project manager ID of the project to validate.
     * @throws IllegalArgumentException if the project manager ID is less than or
     *                                  equal to 0.
     */
    public static void validateProjectManagerId(final int project_manager_id) {
        if (project_manager_id <= 0) {
            throw new IllegalArgumentException("Project manager ID must be greater than 0");
        }
    }
}
