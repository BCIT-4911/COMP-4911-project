package com.corejsf.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

/*
 * This class handles the legal sign-off stuff. The Use Cases explicitly mention we need to capture an 
 * electronic signature to acknowledge that the submitted hours are accurate.
 */
@Entity
@Table(name = "Employee_E_Signature")
public class EmployeeESignature {

    @Id
    @Column(name = "emp_e_sig_id", nullable = false)
    private Integer empESigId;

    @Lob
    @Column(name = "signature_data", nullable = false)
    private byte[] signatureData;

    @Column(name = "signed_at", nullable = false)
    private LocalDateTime signedAt;

    public EmployeeESignature() {
    }

    public Integer getEmpESigId() {
        return empESigId;
    }

    public void setEmpESigId(final Integer empESigId) {
        this.empESigId = empESigId;
    }

    public byte[] getSignatureData() {
        return signatureData;
    }

    public void setSignatureData(final byte[] signatureData) {
        this.signatureData = signatureData;
    }

    public LocalDateTime getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(final LocalDateTime signedAt) {
        this.signedAt = signedAt;
    }
}