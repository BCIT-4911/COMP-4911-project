USE Project_Management;

ALTER TABLE Timesheet ADD COLUMN status ENUM('DRAFT', 'SUBMITTED', 'APPROVED', 'RETURNED') NOT NULL DEFAULT 'DRAFT';

UPDATE Timesheet SET status = CASE
    WHEN approved = b'1' THEN 'SUBMITTED'
    ELSE 'DRAFT'
END;

ALTER TABLE Timesheet DROP COLUMN approved;
