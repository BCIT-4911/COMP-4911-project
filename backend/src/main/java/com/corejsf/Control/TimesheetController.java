package com.corejsf.Control;

import com.corejsf.entity.Timesheet;
import com.corejsf.entity.TimesheetRow;
import com.corejsf.entity.EmployeeESignature;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/*
 * The TimesheetController serves as the core business logic layer for timesheet management.
 * It sits between the user interface boundaries and the database entities, ensuring all rules 
 * defined in the use cases are enforced before data is committed.
 * * Breakdown of necessary components:
 * - EntityManager: The standard JPA API required to create, read, update, and delete our entities 
 * in the underlying database.
 * * - saveAsDraft: Fulfills the requirement to let employees edit timesheets before submission 
 * without triggering validation or locking the record. It simply persists the current row data.
 * * - validateHours: Implements the automated check to compare entered hours against expected 
 * full-time hours. It aggregates the values across all days (Monday-Sunday) and rows to ensure 
 * the total meets the required threshold before submission is allowed.
 * * - finalizeSubmission: Handles the formal submission process. It generates the EmployeeESignature 
 * record with the provided byte array and links it to the timesheet. The presence of this signature 
 * acts as the indicator that the timesheet is officially submitted and awaiting review.
 * * - applyApprovalLock: Used by the Timesheet Approver. By setting the approvalStatus boolean to true, 
 * the system establishes a lock that prevents any further edits to the timesheet data, securing 
 * the finalized record.
 * * - returnForCorrection: Used by the Timesheet Approver to reject a submission. It captures the 
 * rejection reason in the returnComment field, sets approvalStatus to false, and importantly, 
 * removes the electronic signature connection so the employee regains edit access to fix mistakes.
 */
@Stateless
public class TimesheetController {

    @PersistenceContext
    private EntityManager em;

    /**
     * Front-end sends data about new timesheet, which is eventually
     * transferred over to back-end and saved in the database.
     * @param timesheet
     * @return Timesheet
     */
    public Timesheet createTimesheet(Timesheet timesheet) {
        em.persist(timesheet);
        return timesheet;
    }

    /**
     * Get one timesheet by ID. Database finds timesheet with
     * specified ID.
     * @param tsId
     * @return Timesheet
     */
    public Timesheet getTimesheet(Integer tsId) {
        return em.find(Timesheet.class, tsId);

    }

    /**We can add a method to fetch all lists for a specific employee here later */

    public List<Timesheet> getTimesheetsByEmployee(Integer employeeId) {
        return em.createQuery(
                        "SELECT t FROM Timesheet t WHERE t.employee.empId = :empId",
                        Timesheet.class)
                .setParameter("empId", employeeId)
                .getResultList();
    }

    /**We can add a method to fetch all lists for a specific employee here later */

    /** Select only the timesheets that are signed but not approved yet
     * @return List<Timesheet>
     */
    public List<Timesheet> getPendingTimesheets() {
        return em.createQuery(
                "SELECT t FROM Timesheet t WHERE t.eSignature IS NOT NULL AND t.approvalStatus = false",
                Timesheet.class)
                .getResultList();
    }

    /**
     * Removes timesheet only if not signed and submitted.
     * @param tsId
     * @return boolean
     */
    public boolean deleteTimesheet(Integer tsId) {
        Timesheet timesheet = em.find(Timesheet.class, tsId);

        if (timesheet.geteSignature() == null) {
            em.remove(timesheet);
            return true;
        }
        return false;
    }



    public Timesheet saveAsDraft(Integer tsId, List<TimesheetRow> draftRows) {
        Timesheet timesheet = em.find(Timesheet.class, tsId);
        
        // Only allow edits if the timesheet has not been approved
        if (timesheet != null && !Boolean.TRUE.equals(timesheet.getApprovalStatus())) {
            // In a full implementation, this would involve merging the updated rows
            // into the existing timesheet.rows collection via the EntityManager.
            timesheet.setRows(draftRows);
            em.merge(timesheet);
        }
        return timesheet;
    }

    public boolean validateHours(Integer tsId, BigDecimal expectedWeeklyHours) {
        Timesheet timesheet = em.find(Timesheet.class, tsId);
        if (timesheet == null || timesheet.getRows() == null) {
            return false;
        }

        BigDecimal totalHours = BigDecimal.ZERO;

        for (TimesheetRow row : timesheet.getRows()) {
            totalHours = totalHours.add(row.getMonday())
                                   .add(row.getTuesday())
                                   .add(row.getWednesday())
                                   .add(row.getThursday())
                                   .add(row.getFriday())
                                   .add(row.getSaturday())
                                   .add(row.getSunday());
        }

        // Returns true if the accumulated hours match or exceed expected full-time hours
        return totalHours.compareTo(expectedWeeklyHours) >= 0;
    }

    public void finalizeSubmission(Integer tsId, byte[] signatureData) {
        Timesheet timesheet = em.find(Timesheet.class, tsId);
        
        if (timesheet != null && !Boolean.TRUE.equals(timesheet.getApprovalStatus())) {
            // Create and persist the legal e-signature record
            EmployeeESignature signature = new EmployeeESignature();
            // Assuming we use a sequence or auto-increment for the ID in the database
            // signature.setEmpESigId(...) would be handled by JPA @GeneratedValue
            signature.setSignatureData(signatureData);
            signature.setSignedAt(LocalDateTime.now());
            
            em.persist(signature);

            // Link the signature to the timesheet to mark it as submitted
            timesheet.seteSignature(signature);
            em.merge(timesheet);
        }
    }

    public void applyApprovalLock(Integer tsId) {
        Timesheet timesheet = em.find(Timesheet.class, tsId);
        
        // Ensure timesheet exists and has been submitted (has a signature)
        if (timesheet != null && timesheet.geteSignature() != null) {
            timesheet.setApprovalStatus(true); // This boolean acts as the edit lock
            timesheet.setReturnComment(null); // Clear any old rejection comments
            em.merge(timesheet);
        }
    }

    public void returnForCorrection(Integer tsId, String comments) {
        Timesheet timesheet = em.find(Timesheet.class, tsId);
        
        if (timesheet != null) {
            timesheet.setApprovalStatus(false);
            timesheet.setReturnComment(comments); // Provide reason to the employee
            
            // Unlink and optionally delete the signature to "un-submit" the timesheet
            EmployeeESignature oldSignature = timesheet.geteSignature();
            if (oldSignature != null) {
                timesheet.seteSignature(null);
                em.remove(oldSignature); 
            }
            
            em.merge(timesheet);
        }
    }
}