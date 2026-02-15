package com.corejsf.Project;

import java.sql.Date;

public final class ProjectValidation {
    public static void validateId(final String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Project ID cannot be null or empty");
        } else if (id.matches("^[a-zA-Z0-9]*$")) {
            throw new IllegalArgumentException("Project ID cannot contain special characters");
        } else if (id.length() > 255) {
            throw new IllegalArgumentException("Project ID cannot be longer than 255 characters");
        }
    }

    public static void validateName(final String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be null or empty");
        } else if (name.matches("^[a-zA-Z0-9]*$")) {
            throw new IllegalArgumentException("Project name cannot contain special characters");
        } else if (name.length() > 255) {
            throw new IllegalArgumentException("Project name cannot be longer than 255 characters");
        }
    }

    public static void validateDescription(final String description) {
        if (description == null || description.isEmpty()) {
            throw new IllegalArgumentException("Project description cannot be null or empty");
        }
    }

    public static void validateStartDate(final Date startDate) {
        if (startDate.before(new Date(System.currentTimeMillis()))) {
            throw new IllegalArgumentException("Project start date cannot be in the past");
        }
    }

    public static void validateEndDate(final Date endDate, final Date startDate) {
        if (endDate.before(new Date(System.currentTimeMillis()))) {
            throw new IllegalArgumentException("Project end date cannot be in the past");
        } else if (endDate.before(startDate)) {
            throw new IllegalArgumentException("Project end date cannot be before project start date");
        }
    }

    public static void validateCreatedDate(final Date createdDate) {
        if (createdDate.before(new Date(System.currentTimeMillis()))) {
            throw new IllegalArgumentException("Project created date cannot be in the past");
        }
    }

    public static void validateLastModifiedDate(final Date lastModifiedDate, final Date createdDate) {
        if (lastModifiedDate.before(createdDate)) {
            throw new IllegalArgumentException("Project last modified date cannot be before project created date");
        } else if (lastModifiedDate.after(new Date(System.currentTimeMillis()))) {
            throw new IllegalArgumentException("Project last modified date cannot be in the future");
        }
    }

    public static void validateCreatedById(final int created_by_id) {
        if (created_by_id <= 0) {
            throw new IllegalArgumentException("Project created by ID must be greater than 0");
        }
    }

    public static void validateMarkup(final float markup) {
        if (markup < 0) {
            throw new IllegalArgumentException("Project markup must be greater than 0");
        }
    }

    public static void validateProjectManagerId(final long project_manager_id) {
        if (project_manager_id <= 0) {
            throw new IllegalArgumentException("Project manager ID must be greater than 0");
        }
    }
}
