package com.corejsf.Api;

import com.corejsf.TestConfig;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Health / smoke check. {@code /greet} is protected by JWT like other API paths.
 */
class HealthCheckTest extends TestConfig {

    @Test
    void greet_withValidToken_returnsGreeting() {
        String token = loginAsSeedOps();
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/greet")
                .then()
                .statusCode(200)
                .body(equalTo("Greetings from the backend"));
    }
}
