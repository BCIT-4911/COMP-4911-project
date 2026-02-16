package ca.bcit.infosys.project;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

/**
 * Entity for the Project table.
 * Maps directly to the DB schema, just fields, getters, and setters.
 *
 * @author Raymond
 * @author Russell
 */
@Entity
@Table(name = "Project")
public class Project {

    /**
     * The unique project ID, e.g. "P001".
     */
    @Id
    @Column(name = "proj_id")
    @Size(max = 255, message = "Project ID must be less than 255 characters")
    private String project_id;

    /**
     * INTERNAL or EXTERNAL.
     */
    @Column(name = "proj_type")
    @Enumerated(EnumType.STRING)
    private ProjectType project_type;

    /**
     * Human-readable project name.
     */
    @Column(name = "proj_name")
    @Size(max = 255, message = "Project name must be less than 255 characters")
    private String project_name;

    /**
     * Longer description of the project.
     */
    @Column(name = "description")
    private String project_desc;

    /**
     * OPEN or ARCHIVED.
     */
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ProjectStatus project_status;

    /**
     * Project start date. Required by the DB.
     */
    @Column(name = "start_date")
    private LocalDate start_date;

    /**
     * Project end date.
     */
    @Column(name = "end_date")
    private LocalDate end_date;

    /**
     * When this record was created.
     */
    @Column(name = "created_date")
    private LocalDateTime created_date;

    /**
     * When this record was last modified.
     */
    @Column(name = "modified_date")
    private LocalDateTime modified_date;

    /**
     * Employee ID of who created this project.
     */
    @Column(name = "created_by")
    private Integer created_by_id;

    /**
     * Employee ID of who last modified this project.
     */
    @Column(name = "modified_by")
    private Integer modified_by;

    /**
     * Markup rate for billing.
     */
    @Column(name = "markup_rate")
    private float markup_rate;

    /**
     * The project manager's employee ID. Required by the DB.
     */
    @Column(name = "pm_employee_id")
    private int project_manager_id;

    /**
     * Employee assignments for this project.
     * Hidden from JSON to avoid proxy issues.
     */
    @OneToMany(mappedBy = "project")
    @JsonbTransient
    private List<ProjectAssignment> assignments = new ArrayList<>();

    /**
     * Default no-arg constructor, required by JPA.
     */
    public Project() {
    }

    // Getters and Setters

    public String getProject_id() {
        return project_id;
    }

    public void setProject_id(String project_id) {
        this.project_id = project_id;
    }

    public ProjectType getProject_type() {
        return project_type;
    }

    public void setProject_type(ProjectType project_type) {
        this.project_type = project_type;
    }

    public String getProject_name() {
        return project_name;
    }

    public void setProject_name(String project_name) {
        this.project_name = project_name;
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

    public LocalDate getStart_date() {
        return start_date;
    }

    public void setStart_date(LocalDate start_date) {
        this.start_date = start_date;
    }

    public LocalDate getEnd_date() {
        return end_date;
    }

    public void setEnd_date(LocalDate end_date) {
        this.end_date = end_date;
    }

    public LocalDateTime getCreated_date() {
        return created_date;
    }

    public void setCreated_date(LocalDateTime created_date) {
        this.created_date = created_date;
    }

    public LocalDateTime getModified_date() {
        return modified_date;
    }

    public void setModified_date(LocalDateTime modified_date) {
        this.modified_date = modified_date;
    }

    public Integer getCreated_by_id() {
        return created_by_id;
    }

    public void setCreated_by_id(Integer created_by_id) {
        this.created_by_id = created_by_id;
    }

    public Integer getModified_by() {
        return modified_by;
    }

    public void setModified_by(Integer modified_by) {
        this.modified_by = modified_by;
    }

    public float getMarkup_rate() {
        return markup_rate;
    }

    public void setMarkup_rate(float markup_rate) {
        this.markup_rate = markup_rate;
    }

    public int getProject_manager_id() {
        return project_manager_id;
    }

    public void setProject_manager_id(int project_manager_id) {
        this.project_manager_id = project_manager_id;
    }

    public List<ProjectAssignment> getAssignments() {
        return assignments;
    }
}
