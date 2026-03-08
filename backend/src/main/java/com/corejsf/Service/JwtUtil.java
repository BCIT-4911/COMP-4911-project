package com.corejsf.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.corejsf.Entity.SystemRole;

import jakarta.json.Json;
import jakarta.json.JsonObject;

/**
 * Utility class for creating and validating JSON Web Tokens (JWT) using
 * HMAC-SHA256 signatures, with no external dependencies.
 * <p>
 * Provides static methods to generate and validate stateless JWT tokens using HMAC-SHA256.
 * <ul>
 *   <li><b>generateToken</b>: Creates a signed JWT with employee ID, system role, and expiration.</li>
 *   <li><b>validateToken</b>: Verifies signature, expiration, and returns parsed claims.</li>
 * </ul>
 * Tokens follow RFC 7519 and use a secret key from the JWT_SECRET environment variable.
 *
 * 
 * @author Nathan O.
 * @version 1.0
 */
public final class JwtUtil {

    private static final String ALGORITHM = "HmacSHA256";
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int HOURS_PER_TOKEN = 8;
    private static final long EXPIRATION_SECONDS = HOURS_PER_TOKEN * MINUTES_PER_HOUR * SECONDS_PER_MINUTE; // 8 hours

    // Secret key for signing the JWT, loaded from environment or fallback for dev only
    private static final String SECRET = System.getenv("JWT_SECRET") != null ?
            System.getenv("JWT_SECRET") :
            "change-me-in-production-use-env-var";
    
    private static final int JWT_PARTS_COUNT = 3;
    private static final String JWT_HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final int MILLIS_PER_SECOND = 1000;

    private JwtUtil() {
    }

    /**
     * Generates a signed JWT containing empId, systemRole, and exp.
     */
    public static String generateToken(int empId, SystemRole role) {
        // Calculate expiration time as a Unix timestamp (seconds since epoch)
        long exp = System.currentTimeMillis() / MILLIS_PER_SECOND + EXPIRATION_SECONDS;

        // Create JWT header, indicating HMAC SHA-256 is used
        String header = Base64.getUrlEncoder()
                              .withoutPadding()
                              .encodeToString(JWT_HEADER_JSON.getBytes(StandardCharsets.UTF_8));

        // Create JWT payload as a JSON string holding the user's info and expiration
        String payloadJson = Json.createObjectBuilder()
                                 .add("empId", empId)
                                 .add("systemRole", role.name())
                                 .add("exp", exp)
                                 .build()
                                 .toString();

        // Base64URL encode the payload JSON, per JWT spec
        String payload = Base64.getUrlEncoder()
                               .withoutPadding()
                               .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        // Concatenate header and payload so they can be signed
        String signatureInput = header + "." + payload;

        // Sign the header and payload using HMAC-SHA256 and the secret key
        String signature = sign(signatureInput);

        // Return the complete JWT: header.payload.signature
        return signatureInput + "." + signature;
    }

    /**
     * Validates the token, verifies signature and exp, returns parsed claims.
     *
     * @throws IllegalArgumentException if token is invalid or expired
     */
    public static JwtClaims validateToken(String token) {
        if(token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token is null or empty");
        }

        // Split the JWT into its three dot-separated parts
        String[] parts = token.split("\\.");
        if(parts.length != JWT_PARTS_COUNT) {
            throw new IllegalArgumentException("Invalid JWT format");
        }

        String header = parts[0];
        String payload = parts[1];
        String signature = parts[2];

        // Re-create the part of the JWT that was signed to verify its authenticity
        String signatureInput = header + "." + payload;
        String expectedSignature = sign(signatureInput);

        // Ensure the signature matches what we'd expect (prevents tampering)
        if(!constantTimeEquals(signature, expectedSignature)) {
            throw new IllegalArgumentException("Invalid signature");
        }

        // Decode the payload back to JSON to extract the actual claims
        String payloadJson = new String(Base64.getUrlDecoder()
                                              .decode(payload),
                                        StandardCharsets.UTF_8);
        JsonObject json = Json.createReader(new java.io.StringReader(payloadJson))
                                 .readObject();

        // Check if the token is expired (expiration in the past)
        long exp = json.getJsonNumber("exp").longValue();
        if(System.currentTimeMillis() / MILLIS_PER_SECOND > exp) {
            throw new IllegalArgumentException("Token expired");
        }

        // Extract user claims from the payload
        int empId = json.getInt("empId");
        String systemRoleStr = json.getString("systemRole");
        SystemRole systemRole = SystemRole.valueOf(systemRoleStr);

        return new JwtClaims(empId, systemRole);
    }

    /**
     * Signs input data with HMAC-SHA256 and returns a Base64URL-encoded signature
     */
    private static String sign(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            // Return as Base64URL without padding, as required by JWT spec
            return Base64.getUrlEncoder()
                         .withoutPadding()
                         .encodeToString(sig);
        }
        catch(Exception e) {
            throw new RuntimeException("JWT signing failed", e);
        }
    }

    /**
     * Compares two strings in constant time to prevent timing attacks.
     * Returns true only if both strings match exactly.
     */
    private static boolean constantTimeEquals(String a, String b) {
        if(a.length() != b.length()) {
            return false;
        }
        int result = 0;
        // XOR all characters to ensure operation always takes same time
        for(int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    // Holds the parsed claims from a valid JWT token
    public record JwtClaims(int empId, SystemRole systemRole) { }
}
