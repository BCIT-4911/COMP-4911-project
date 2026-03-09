package com.corejsf.DTO;

/**
 * Response DTO for login endpoint containing the signed JWT.
 */
public class LoginResponseDTO {

    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
