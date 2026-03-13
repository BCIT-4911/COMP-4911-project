CREATE TABLE Employee
(
    emp_id                INT AUTO_INCREMENT NOT NULL,
    emp_first_name        VARCHAR(255)  NOT NULL,
    emp_last_name         VARCHAR(255)  NOT NULL,
    emp_password          VARCHAR(255)  NOT NULL,
    system_role           VARCHAR(20) NULL,
    emp_e_sig_id          INT           NOT NULL,
    labor_grade_id        INT           NOT NULL,
    supervisor_id         INT NULL,
    vacation_sick_balance DECIMAL(10, 2) NULL,
    expected_weekly_hours DECIMAL(3, 1) NOT NULL,
    CONSTRAINT pk_employee PRIMARY KEY (emp_id)
);

CREATE TABLE Employee_E_Signature
(
    emp_e_sig_id   INT AUTO_INCREMENT NOT NULL,
    signature_data LONGBLOB NOT NULL,
    signed_at      datetime NOT NULL,
    CONSTRAINT pk_employee_e_signature PRIMARY KEY (emp_e_sig_id)
);

CREATE TABLE Labor_Grade
(
    labor_grade_id INT AUTO_INCREMENT NOT NULL,
    grade_code     VARCHAR(2)     NOT NULL,
    charge_rate    DECIMAL(10, 2) NOT NULL,
    CONSTRAINT pk_labor_grade PRIMARY KEY (labor_grade_id)
);

CREATE TABLE Project
(
    proj_id        VARCHAR(255) NOT NULL,
    proj_type      VARCHAR(255) NULL,
    pm_employee_id INT          NOT NULL,
    proj_name      VARCHAR(255) NULL,
    `description`  TEXT NULL,
    status         VARCHAR(255) NULL,
    start_date     date         NOT NULL,
    end_date       date NULL,
    created_date   datetime NULL,
    modified_date  datetime NULL,
    created_by     INT NULL,
    modified_by    INT NULL,
    markup_rate    DECIMAL(5, 2) NULL,
    CONSTRAINT pk_project PRIMARY KEY (proj_id)
);

CREATE TABLE Project_Assignment
(
    pa_id           INT AUTO_INCREMENT NOT NULL,
    emp_id          INT          NOT NULL,
    proj_id         VARCHAR(255) NOT NULL,
    assignment_date date         NOT NULL,
    CONSTRAINT pk_project_assignment PRIMARY KEY (pa_id)
);

CREATE TABLE Rate_History
(
    rate_history_id INT AUTO_INCREMENT NOT NULL,
    labor_grade_id  INT            NOT NULL,
    charge_rate     DECIMAL(10, 2) NOT NULL,
    start_date      date           NOT NULL,
    end_date        date NULL,
    CONSTRAINT pk_rate_history PRIMARY KEY (rate_history_id)
);

CREATE TABLE Timesheet
(
    ts_id          INT AUTO_INCREMENT NOT NULL,
    emp_id         INT    NOT NULL,
    week_ending    date   NOT NULL,
    approver_id    INT NULL,
    approved       BIT(1) NOT NULL,
    return_comment TEXT NULL,
    status         INT    NOT NULL,
    emp_e_sig_id   INT NULL,
    CONSTRAINT pk_timesheet PRIMARY KEY (ts_id)
);

CREATE TABLE Timesheet_Row
(
    ts_row_id        INT AUTO_INCREMENT NOT NULL,
    ts_row_monday    DECIMAL(4, 1) NOT NULL,
    ts_row_tuesday   DECIMAL(4, 1) NOT NULL,
    ts_row_wednesday DECIMAL(4, 1) NOT NULL,
    ts_row_thursday  DECIMAL(4, 1) NOT NULL,
    ts_row_friday    DECIMAL(4, 1) NOT NULL,
    ts_row_saturday  DECIMAL(4, 1) NULL,
    ts_row_sunday    DECIMAL(4, 1) NOT NULL,
    labor_grade_id   INT           NOT NULL,
    wp_id            VARCHAR(255)  NOT NULL,
    ts_id            INT           NOT NULL,
    CONSTRAINT pk_timesheet_row PRIMARY KEY (ts_row_id)
);

CREATE TABLE Work_Package
(
    wp_id                VARCHAR(255) NOT NULL,
    wp_name              VARCHAR(255) NULL,
    `description`        TEXT NULL,
    proj_id              VARCHAR(255) NOT NULL,
    parent_wp_id         VARCHAR(255) NULL,
    wp_type              VARCHAR(255) NULL,
    status               VARCHAR(255) NULL,
    structure_locked     BIT(1) NULL,
    budgeted_effort      DECIMAL(10, 2) NULL,
    bcws                 DECIMAL(12, 2) NULL,
    plan_start_date      date         NOT NULL,
    plan_end_date        date         NOT NULL,
    re_employee_id       INT          NOT NULL,
    bac                  DECIMAL(12, 2) NULL,
    percent_complete     DECIMAL(5, 2) NULL,
    eac                  DECIMAL(12, 2) NULL,
    cv                   DECIMAL(12, 2) NULL,
    created_date         datetime NULL,
    modified_date        datetime NULL,
    created_by           INT NULL,
    modified_by          INT NULL,
    work_accomplished    VARCHAR(255) NULL,
    work_planned         VARCHAR(255) NULL,
    problems             VARCHAR(255) NULL,
    anticipated_problems VARCHAR(255) NULL,
    CONSTRAINT pk_work_package PRIMARY KEY (wp_id)
);

CREATE TABLE Work_Package_Assignment
(
    wpa_id          INT AUTO_INCREMENT NOT NULL,
    emp_id          INT          NOT NULL,
    wp_id           VARCHAR(255) NOT NULL,
    assignment_date date         NOT NULL,
    CONSTRAINT pk_work_package_assignment PRIMARY KEY (wpa_id)
);

ALTER TABLE Employee
    ADD CONSTRAINT FK_EMPLOYEE_ON_EMP_E_SIG FOREIGN KEY (emp_e_sig_id) REFERENCES Employee_E_Signature (emp_e_sig_id);

ALTER TABLE Employee
    ADD CONSTRAINT FK_EMPLOYEE_ON_LABOR_GRADE FOREIGN KEY (labor_grade_id) REFERENCES Labor_Grade (labor_grade_id);

ALTER TABLE Employee
    ADD CONSTRAINT FK_EMPLOYEE_ON_SUPERVISOR FOREIGN KEY (supervisor_id) REFERENCES Employee (emp_id);

ALTER TABLE Project_Assignment
    ADD CONSTRAINT FK_PROJECT_ASSIGNMENT_ON_EMP FOREIGN KEY (emp_id) REFERENCES Employee (emp_id);

ALTER TABLE Project_Assignment
    ADD CONSTRAINT FK_PROJECT_ASSIGNMENT_ON_PROJ FOREIGN KEY (proj_id) REFERENCES Project (proj_id);

ALTER TABLE Project
    ADD CONSTRAINT FK_PROJECT_ON_CREATED_BY FOREIGN KEY (created_by) REFERENCES Employee (emp_id);

ALTER TABLE Project
    ADD CONSTRAINT FK_PROJECT_ON_MODIFIED_BY FOREIGN KEY (modified_by) REFERENCES Employee (emp_id);

ALTER TABLE Project
    ADD CONSTRAINT FK_PROJECT_ON_PM_EMPLOYEE FOREIGN KEY (pm_employee_id) REFERENCES Employee (emp_id);

ALTER TABLE Rate_History
    ADD CONSTRAINT FK_RATE_HISTORY_ON_LABOR_GRADE FOREIGN KEY (labor_grade_id) REFERENCES Labor_Grade (labor_grade_id);

ALTER TABLE Timesheet
    ADD CONSTRAINT FK_TIMESHEET_ON_APPROVER FOREIGN KEY (approver_id) REFERENCES Employee (emp_id);

ALTER TABLE Timesheet
    ADD CONSTRAINT FK_TIMESHEET_ON_EMP FOREIGN KEY (emp_id) REFERENCES Employee (emp_id);

ALTER TABLE Timesheet
    ADD CONSTRAINT FK_TIMESHEET_ON_EMP_E_SIG FOREIGN KEY (emp_e_sig_id) REFERENCES Employee_E_Signature (emp_e_sig_id);

ALTER TABLE Timesheet_Row
    ADD CONSTRAINT FK_TIMESHEET_ROW_ON_LABOR_GRADE FOREIGN KEY (labor_grade_id) REFERENCES Labor_Grade (labor_grade_id);

ALTER TABLE Timesheet_Row
    ADD CONSTRAINT FK_TIMESHEET_ROW_ON_TS FOREIGN KEY (ts_id) REFERENCES Timesheet (ts_id);

ALTER TABLE Timesheet_Row
    ADD CONSTRAINT FK_TIMESHEET_ROW_ON_WP FOREIGN KEY (wp_id) REFERENCES Work_Package (wp_id);

ALTER TABLE Work_Package_Assignment
    ADD CONSTRAINT FK_WORK_PACKAGE_ASSIGNMENT_ON_EMP FOREIGN KEY (emp_id) REFERENCES Employee (emp_id);

ALTER TABLE Work_Package_Assignment
    ADD CONSTRAINT FK_WORK_PACKAGE_ASSIGNMENT_ON_WP FOREIGN KEY (wp_id) REFERENCES Work_Package (wp_id);

ALTER TABLE Work_Package
    ADD CONSTRAINT FK_WORK_PACKAGE_ON_CREATED_BY FOREIGN KEY (created_by) REFERENCES Employee (emp_id);

ALTER TABLE Work_Package
    ADD CONSTRAINT FK_WORK_PACKAGE_ON_MODIFIED_BY FOREIGN KEY (modified_by) REFERENCES Employee (emp_id);

ALTER TABLE Work_Package
    ADD CONSTRAINT FK_WORK_PACKAGE_ON_PARENT_WP FOREIGN KEY (parent_wp_id) REFERENCES Work_Package (wp_id);

ALTER TABLE Work_Package
    ADD CONSTRAINT FK_WORK_PACKAGE_ON_PROJ FOREIGN KEY (proj_id) REFERENCES Project (proj_id);

ALTER TABLE Work_Package
    ADD CONSTRAINT FK_WORK_PACKAGE_ON_RE_EMPLOYEE FOREIGN KEY (re_employee_id) REFERENCES Employee (emp_id);