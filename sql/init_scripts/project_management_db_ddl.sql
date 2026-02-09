CREATE DATABASE IF NOT EXISTS Project_Management;

USE Project_Management;

DROP TABLE IF EXISTS Rate_History;
DROP TABLE IF EXISTS Project;
DROP TABLE IF EXISTS Employee;
DROP TABLE IF EXISTS Employee_E_Signature;
DROP TABLE IF EXISTS Labor_Grade;

CREATE TABLE Labor_Grade(
    labor_grade_id INT PRIMARY KEY
);

CREATE TABLE Rate_History(
    rate_history INT PRIMARY KEY,
    labor_grade_id INT NOT NULL,
    charge_rate DECIMAL NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    FOREIGN KEY (labor_grade_id) REFERENCES Labor_Grade(labor_grade_id)
);

CREATE TABLE Employee_E_Signature(
    emp_e_sig_id INT PRIMARY KEY,
    signature_data VARBINARY(255) NOT NULL,
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
    supervisor_id INT NOT NULL,
    vacation_sick_balance DECIMAL,
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
