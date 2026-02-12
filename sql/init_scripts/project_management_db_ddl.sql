CREATE DATABASE IF NOT EXISTS Project_Management;

USE Project_Management;

DROP TABLE IF EXISTS Timesheet_Row; 
DROP TABLE IF EXISTS Timesheet;    
DROP TABLE IF EXISTS Work_Package_Assignment;
DROP TABLE IF EXISTS Project_Assignment;
DROP TABLE IF EXISTS Work_Package;
DROP TABLE IF EXISTS Rate_History;
DROP TABLE IF EXISTS Project;
DROP TABLE IF EXISTS Employee;
DROP TABLE IF EXISTS Employee_E_Signature;
DROP TABLE IF EXISTS Labor_Grade;

CREATE TABLE Labor_Grade(
    labor_grade_id INT PRIMARY KEY,
    grade_code VARCHAR(2) NOT NULL, 
    charge_rate DECIMAL(10,2) NOT NULL
);

CREATE TABLE Rate_History(
    rate_history_id INT PRIMARY KEY,
    labor_grade_id INT NOT NULL,
    charge_rate DECIMAL(10,2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    FOREIGN KEY (labor_grade_id) REFERENCES Labor_Grade(labor_grade_id)
);

CREATE TABLE Employee_E_Signature(
    emp_e_sig_id INT PRIMARY KEY,
    signature_data LONGBLOB NOT NULL,
    signed_at DATETIME NOT NULL
);

CREATE TABLE Employee(
    emp_id INT PRIMARY KEY,
    emp_first_name VARCHAR(255) NOT NULL,
    emp_last_name VARCHAR(255) NOT NULL,
    emp_password VARCHAR(255) NOT NULL,
    system_role ENUM('HR', 'ADMIN', 'EMPLOYEE'),
    emp_e_sig_id INT NOT NULL,
    labor_grade_id INT NOT NULL,
    supervisor_id INT,
    vacation_sick_balance DECIMAL(10,2),
    expected_weekly_hours DECIMAL(3,1) NOT NULL,
    FOREIGN KEY (emp_e_sig_id) REFERENCES Employee_E_Signature(emp_e_sig_id),
    FOREIGN KEY (labor_grade_id) REFERENCES Labor_Grade(labor_grade_id),
    FOREIGN KEY (supervisor_id) REFERENCES Employee(emp_id)
);

CREATE TABLE Project(
    proj_id VARCHAR(255) PRIMARY KEY,
    proj_type ENUM('INTERNAL', 'EXTERNAL'),
    pm_employee_id INT NOT NULL,
    proj_name VARCHAR(255),
    description TEXT,
    status ENUM('OPEN', 'ARCHIVED'),
    start_date DATE NOT NULL,
    end_date DATE,
    created_date DATETIME,
    modified_date DATETIME,
    created_by INT,
    modified_by INT,
    markup_rate DECIMAL(5,2),
    FOREIGN KEY (pm_employee_id) REFERENCES Employee(emp_id)
);

CREATE TABLE Work_Package(
    wp_id VARCHAR(255) PRIMARY KEY,
    wp_name VARCHAR(255),
    description TEXT,
    proj_id VARCHAR(255) NOT NULL,
    parent_wp_id VARCHAR(255),
    wp_type ENUM('Summary', 'Lowest-Level'),
    status ENUM('Open for Charges', 'Closed for Charges', 'Complete'),
    structure_locked BOOLEAN,
    budgeted_effort DECIMAL(10,2),
    bcws DECIMAL(12,2),
    plan_start_date DATE NOT NULL,
    plan_end_date DATE NOT NULL,
    re_employee_id INT NOT NULL,
    bac DECIMAL(12,2),
    percent_complete DECIMAL(5,2),
    eac DECIMAL(12,2),
    cv DECIMAL(12,2),
    created_date DATETIME,
    modified_date DATETIME,
    created_by INT,
    modified_by INT,
    work_accomplished VARCHAR(255),
    work_planned VARCHAR(255),
    problems VARCHAR(255),
    anticipated_problems VARCHAR(255),
    FOREIGN KEY (proj_id) REFERENCES Project(proj_id),
    FOREIGN KEY (parent_wp_id) REFERENCES Work_Package(wp_id),
    FOREIGN KEY (re_employee_id) REFERENCES Employee(emp_id),
    FOREIGN KEY (created_by) REFERENCES Employee(emp_id),
    FOREIGN KEY (modified_by) REFERENCES Employee(emp_id)
);

CREATE TABLE Project_Assignment(
    pa_id INT PRIMARY KEY,
    emp_id INT NOT NULL,
    proj_id VARCHAR(255) NOT NULL,
    assignment_date DATE NOT NULL,
    FOREIGN KEY (emp_id) REFERENCES Employee(emp_id),
    FOREIGN KEY (proj_id) REFERENCES Project(proj_id)
);

CREATE TABLE Work_Package_Assignment(
    wpa_id INT PRIMARY KEY,
    emp_id INT NOT NULL,
    wp_id VARCHAR(255) NOT NULL,
    assignment_date DATE NOT NULL,
    FOREIGN KEY (emp_id) REFERENCES Employee(emp_id),
    FOREIGN KEY (wp_id) REFERENCES Work_Package(wp_id)
);

CREATE TABLE Timesheet(
    ts_id INT NOT NULL PRIMARY KEY,
    emp_id INT NOT NULL,
    week_ending DATE NOT NULL,
    approver_id INT NOT NULL,
    approval_status BOOLEAN NOT NULL,
    return_comment TEXT,
    emp_e_sig_id INT, 
    FOREIGN KEY (emp_id) REFERENCES Employee(emp_id),
    FOREIGN KEY (approver_id) REFERENCES Employee(emp_id),
    FOREIGN KEY (emp_e_sig_id) REFERENCES Employee_E_Signature(emp_e_sig_id)
);

CREATE TABLE Timesheet_Row(
    ts_row_id INT NOT NULL PRIMARY KEY,
    ts_row_monday DECIMAL(4,1) NOT NULL,
    ts_row_tuesday DECIMAL(4,1) NOT NULL,
    ts_row_wednesday DECIMAL(4,1) NOT NULL,
    ts_row_thursday DECIMAL(4,1) NOT NULL,
    ts_row_friday DECIMAL(4,1) NOT NULL,
    ts_row_saturday DECIMAL(4,1) NOT NULL,
    ts_row_sunday DECIMAL(4,1) NOT NULL,
    labor_grade_id INT NOT NULL,
    wp_id VARCHAR(255) NOT NULL,
    ts_id INT NOT NULL,
    FOREIGN KEY (labor_grade_id) REFERENCES Labor_Grade(labor_grade_id),
    FOREIGN KEY (wp_id) REFERENCES Work_Package(wp_id),
    FOREIGN KEY (ts_id) REFERENCES Timesheet(ts_id)
);