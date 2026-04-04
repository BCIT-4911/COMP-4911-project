package com.corejsf.Api;

import com.corejsf.TestConfig;
import com.corejsf.TestConfig.StandardSeedIds;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthEndpointTest extends TestConfig {

    private static StandardSeedIds IDS;

    @BeforeAll
    static void loadIds() {
        IDS = resolveStandardSeedIds(loginAsSeedOps());
    }

    @Test
    void login_validCredentials_returnsToken() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "empId": %d, "password": "%s" }
                        """.formatted(IDS.opsId(), DEFAULT_PASSWORD))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue());
    }

    @Test
    void login_wrongPassword_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "empId": %d, "password": "not-the-real-password" }
                        """.formatted(IDS.opsId()))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    void login_missingFields_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/auth/login")
                .then()
                .statusCode(400);
    }

    @Test
    void canAccessApproverDashboard_supervisor_returnsAllowedTrue() {
        String bugsToken = login(IDS.pmProj1Id(), DEFAULT_PASSWORD);
        boolean allowed = given()
                .header("Authorization", "Bearer " + bugsToken)
                .when()
                .get("/auth/can-access-approver-dashboard")
                .then()
                .statusCode(200)
                .extract()
                .path("allowed");
        assertTrue(allowed);
    }

    @Test
    void directReports_supervisor_returnsEmployeeIdList() {
        String bugsToken = login(IDS.pmProj1Id(), DEFAULT_PASSWORD);
        var list = given()
                .header("Authorization", "Bearer " + bugsToken)
                .when()
                .get("/auth/direct-reports")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("employeeIds", Integer.class);
        assertFalse(list.isEmpty());
        assertTrue(list.contains(IDS.daffyId()));
    }
}
