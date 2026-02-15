package com.corejsf.Project;

import java.sql.Date;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.validation.constraints.Size;

public class Project {
    @Id
    @Size(max = 255, message = "Project ID must be less than 255 characters")
    final private String project_id;
    final private ProjectType project_type;
    @Size(max = 255, message = "Project name must be less than 255 characters")
    final private String project_name;
    private String project_desc;
    private ProjectStatus project_status;
    final private Date start_date;
    private Date end_date;
    final private Date created_date;
    private Date last_modified_date;
    final private int created_by_id;
    private float markup_date;
    @JoinColumn(name = "project_manager_id", foreignKey = @ForeignKey(name = "project_manager"))
    final private long project_manager_id;

    public Project(final String id, final ProjectType type, final String name, final String desc,
            final ProjectStatus status, final Date start, final Date end, final Date created, final Date modified,
            final int createdBy, final float markup, final long projectManager) {
        ProjectValidation.validateId(id);
        ProjectValidation.validateName(name);
        ProjectValidation.validateDescription(desc);
        ProjectValidation.validateStartDate(start);
        ProjectValidation.validateEndDate(end, start);
        ProjectValidation.validateCreatedDate(created);
        ProjectValidation.validateLastModifiedDate(modified, created);
        ProjectValidation.validateCreatedById(createdBy);
        ProjectValidation.validateMarkup(markup);
        ProjectValidation.validateProjectManagerId(projectManager);

        this.project_id = id;
        this.project_type = type;
        this.project_name = name;
        this.project_desc = desc;
        this.project_status = status;
        this.start_date = start;
        this.end_date = end;
        this.created_date = created;
        this.last_modified_date = modified;
        this.created_by_id = createdBy;
        this.markup_date = markup;
        this.project_manager_id = projectManager;
    }

    public String getProject_id() {
        return project_id;
    }

    public ProjectType getProject_type() {
        return project_type;
    }

    public String getProject_name() {
        return project_name;
    }

    public String getProject_desc() {
        return project_desc;
    }

    public void setProject_desc(String project_desc) {
        this.project_desc = project_desc;
    }

    public ProjectStatus getProject_status() {
        return project_status;
    }

    public void setProject_status(ProjectStatus project_status) {
        this.project_status = project_status;
    }

    public Date getStart_date() {
        return start_date;
    }

    public Date getEnd_date() {
        return end_date;
    }

    public void setEnd_date(Date end_date) {
        this.end_date = end_date;
    }

    public Date getCreated_date() {
        return created_date;
    }

    public Date getLast_modified_date() {
        return last_modified_date;
    }

    public void setLast_modified_date(Date last_modified_date) {
        this.last_modified_date = last_modified_date;
    }

    public int getCreated_by_id() {
        return created_by_id;
    }

    public float getMarkup_date() {
        return markup_date;
    }

    public void setMarkup_date(float markup_date) {
        this.markup_date = markup_date;
    }

    public long getProject_manager_id() {
        return project_manager_id;
    }

    public String generateReport() {
        StringBuilder builder = new StringBuilder();
        builder.append("Project ID: " + project_id + "\n");
        builder.append("Project Manager: " + project_manager_id + "\n");
        builder.append("Project Type: " + project_type + "\n");
        builder.append("Project Name: " + project_name + "\n");
        builder.append("Project Description: " + project_desc + "\n");
        builder.append("Project Status: " + project_status + "\n");
        builder.append("Start Date: " + start_date + "\n");
        builder.append("End Date: " + end_date + "\n");
        builder.append("Created Date: " + created_date + "\n");
        builder.append("Last Modified Date: " + last_modified_date + "\n");
        builder.append("Created By: " + created_by_id + "\n");
        builder.append("Markup: " + markup_date + "\n");
        return builder.toString();
    }
}
