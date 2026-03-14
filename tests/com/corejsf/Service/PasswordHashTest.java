package com.corejsf.Service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

public class PasswordHashTest {

    @Test
    void passwordHashShouldNotMatchPlaintext() {
        String password = "Password123!";
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        assertNotEquals(password, hashedPassword);
    }

    @Test
    void correctPasswordShouldValidate(){
        String password = "Password123!";
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        assertTrue(BCrypt.checkpw(password, hashedPassword));
    }
}