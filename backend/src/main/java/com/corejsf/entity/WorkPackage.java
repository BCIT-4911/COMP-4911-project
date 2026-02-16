package com.corejsf.entity;

import jakarta.persistence.*;

/*
 * This is just a bare-minimum stub for the WorkPackage class so our Timesheet can actually compile.
 */
@Entity
@Table(name = "Work_Package")
public class WorkPackage {

    @Id
    @Column(name = "wp_id", nullable = false, length = 255)
    private String wpId;

    @Column(name = "wp_name", length = 255)
    private String wpName;

    public WorkPackage() {
    }

    public String getWpId() {
        return wpId;
    }

    public void setWpId(final String wpId) {
        this.wpId = wpId;
    }

    public String getWpName() {
        return wpName;
    }

    public void setWpName(final String wpName) {
        this.wpName = wpName;
    }
}