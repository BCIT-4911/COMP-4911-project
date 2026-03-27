package com.corejsf.Entity;

public enum SystemRole {
    HR,
    OPERATIONS_MANAGER,
    EMPLOYEE,
    /**
     * ADMIN has full read access to all EV reports across all projects.
     * Added as part of the EV Security feature.
     */
    ADMIN
}
