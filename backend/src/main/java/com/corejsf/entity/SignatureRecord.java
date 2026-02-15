package com.corejsf.entity;

import java.io.Serial;
import java.io.Serializable;

public class SignatureRecord implements Serializable {

    private int signatureId;

    private int timesheetId;

    private byte[] eSignature;

    /** Date of signing */
    private int signedAt;

    /**
     * The following methods are the respective getters and setters
     * @return
     */
    public int getSignatureId() {
        return signatureId;
    }

    public void setSignatureId(int signId) {
        signatureId = signId;
    }

    public int getTimesheetId() {
        return timesheetId;
    }

    public void setTimesheetId(int tsId) {
        timesheetId = tsId;
    }

    public byte[] getESignature() {
        return eSignature;
    }

    public void setESignature(byte[] eSign) {
        eSignature = eSign;
    }

    public int getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(int timeOfSign) {
        signedAt = timeOfSign;
    }









}