package com.corejsf.entity;

import java.io.Serial;
import java.io.Serializable;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Login Credentials.
 * @author blink
 * @version 1.1
 * //test
 */
public class UserCredential implements Serializable {

    @Serial
    private static final long serialVersionUID = 11L;

    /** The login ID. */
    private String userName;

    /** Plain password. */
    private String passwordPlain;

    /** The hashed password. */
    private String passwordHash;

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
        return passwordPlain;
    }

    /**
     * password setter.
     * @param plainPassword the password to set
     */
    public void setPassword(String plainPassword) {
        this.passwordHash = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }


    @Override
    public String toString() {
        return userName + '\t' + passwordPlain;
    }

}


