package ca.bcit.infosys.workpackage;

import ca.bcit.infosys.workpackage.WorkPackage;

/**
 * Validation class for WorkPackage.
 */
public final class WorkPackageValidation {

    public static void validate(WorkPackage wp) {
        if (wp == null) {
            throw new IllegalArgumentException("WorkPackage cannot be null.");
        }
        validateId(wp.getWpId());
        validateName(wp.getWpName());
        validateProjectId(wp.getProjId());
    }

    public static void validateId(final String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Work Package ID cannot be null or empty.");
        }
        if (id.length() > 255) {
            throw new IllegalArgumentException("Work Package ID cannot be longer than 255 characters.");
        }
        // Example: P100-1.1.1 - allows letters, numbers, hyphens, and periods.
        if (!id.matches("^[a-zA-Z0-9.-]*$")) {
            throw new IllegalArgumentException("Work Package ID can only contain letters, numbers, periods, and hyphens.");
        }
    }

    public static void validateName(final String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Work Package name cannot be null or empty.");
        }
        if (name.length() > 255) {
            throw new IllegalArgumentException("Work Package name cannot be longer than 255 characters.");
        }
    }

    public static void validateProjectId(final String projId) {
        if (projId == null || projId.isEmpty()) {
            throw new IllegalArgumentException("Project ID for Work Package cannot be null or empty.");
        }
    }
}
