package com.corejsf.Service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.corejsf.Entity.SystemRole;

public class JwtUtilTest {

    @Test
    void generateTokenShouldReturnNonEmptyToken() {
        String token = JwtUtil.generateToken(101, SystemRole.HR, "Test", "User");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void generatedTokenShouldHaveThreeParts() {
        String token = JwtUtil.generateToken(101, SystemRole.HR, "Test", "User");
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    void validateTokenShouldReturnCorrectEmpId() {
        String token = JwtUtil.generateToken(101, SystemRole.EMPLOYEE, "Test", "User");
        JwtUtil.JwtClaims claims = JwtUtil.validateToken(token);
        assertEquals(101, claims.empId());
    }

    @Test
    void validateTokenShouldReturnCorrectSystemRole() {
        String token = JwtUtil.generateToken(101, SystemRole.OPERATIONS_MANAGER, "Test", "User");
        JwtUtil.JwtClaims claims = JwtUtil.validateToken(token);
        assertEquals(SystemRole.OPERATIONS_MANAGER, claims.systemRole());
    }

    @Test
    void tamperedTokenShouldFailValidation() {
        String token = JwtUtil.generateToken(101, SystemRole.EMPLOYEE, "Test", "User");
        // Tamper with the token by changing the last character
        String tamperedToken = token.substring(0, token.length() - 1) 
        + (token.charAt(token.length() - 1) == 'a' ? 'b' : 'a');

        assertThrows(IllegalArgumentException.class, () -> JwtUtil.validateToken(tamperedToken));
    }

    @Test
    void nullTokenShouldFailValidation() {
        assertThrows(IllegalArgumentException.class, () -> JwtUtil.validateToken(null));
    }

    @Test
    void blankTokenShouldFailValidation() {
        assertThrows(IllegalArgumentException.class, () -> JwtUtil.validateToken("  " ));
    }

    @Test
    void invalidFormatTokenShouldFailValidation() {
        assertThrows(IllegalArgumentException.class, () -> JwtUtil.validateToken("not.a.valid.jwt.token"));
    }
}