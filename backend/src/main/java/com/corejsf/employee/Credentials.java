package com.corejsf.employee;

import java.io.Serial;
import java.io.Serializable;

public class Credentials implements Serializable {
    @Serial
    private static final long serialVersionUID = 11L;

    /** The login ID. */
    private String userName;

    /** The password. */
    private String password;

    /**
     * userName getter.
     * @return the loginID
     */
    public String getUserName() {
        return userName;
    }

    /**
     * userName setter.
     * @param id the loginID to set
     */
    public void setUserName(final String id) {
        userName = id;
    }

    /**
     * password getter.
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * password setter.
     * @param pw the password to set
     */
    public void setPassword(final String pw) {
        password = pw;
    }

    @Override
    public String toString() {
        return userName + '\t' + password;
    }
}
