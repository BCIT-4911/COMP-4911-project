package com.corejsf.Api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProjectAndWorkPackageRebacIntegrationTest {

    private static final int OPS_ID = 1;
    private static final int HR_ID = 2;
    private static final int PM_PROJ1_ID = 3;
    private static final int RE_A_ID = 4;
    private static final int MEMBER_A2_ID = 5;
    private static final int RE_A2_ID = 6;
    private static final int PM_PROJ2_ID = 7;

    private static final String PASSWORD = "password";

    private static String opsToken;
    private static String hrToken;
    private static String pmProj1Token;
    private static String reAToken;
    private static String memberA2Token;
    private static String reA2Token;
    private static String pmProj2Token;

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;
        RestAssured.basePath = "/Project/api";

        opsToken = login(OPS_ID, PASSWORD);
        hrToken = login(HR_ID, PASSWORD);
        pmProj1Token = login(PM_PROJ1_ID, PASSWORD);
        reAToken = login(RE_A_ID, PASSWORD);
        memberA2Token = login(MEMBER_A2_ID, PASSWORD);
        reA2Token = login(RE_A2_ID, PASSWORD);
        pmProj2Token = login(PM_PROJ2_ID, PASSWORD);
    }

    private static String login(int empId, String password) {
        Response response = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "empId": %d,
                          "password": "%s"
                        }
                        """.formatted(empId, password))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .response();

        String token = response.jsonPath().getString("token");

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Login succeeded but token was missing for empId=" + empId);
        }

        return token;
    }

    private Response postWithToken(String token, String endpoint) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when()
                .post(endpoint);
    }

    private Response putWithToken(String token, String endpoint) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when()
                .put(endpoint);
    }

    private void assertSuccess2xx(Response response) {
        int status = response.getStatusCode();
        assertTrue(status >= 200 && status < 300,
                "Expected 2xx success but got " + status + " with body: " + response.getBody().asString());
    }

    // PROJECT ASSIGNMENT RBAC
    // Endpoint:
    // POST /projects/{id}/employees/{empId}?role=PM

    @Test
    void assignEmployeeToProject_asOpsManager_succeeds() {
        Response response = postWithToken(
                opsToken,
                "/projects/PROJ-1/employees/5?role=MEMBER"
        );

        assertSuccess2xx(response);
    }

    @Test
    void assignEmployeeToProject_asPmOfThatProject_succeeds() {
        Response response = postWithToken(
                pmProj1Token,
                "/projects/PROJ-1/employees/6?role=MEMBER"
        );

        assertSuccess2xx(response);
    }

    @Test
    void assignEmployeeToProject_asPmOfDifferentProject_returns403() {
        postWithToken(
                pmProj2Token,
                "/projects/PROJ-1/employees/5?role=MEMBER"
        )
        .then()
        .statusCode(403);
    }

    @Test
    void assignEmployeeToProject_asRegularEmployee_returns403() {
        postWithToken(
                memberA2Token,
                "/projects/PROJ-1/employees/4?role=MEMBER"
        )
        .then()
        .statusCode(403);
    }

    @Test
    void assignEmployeeToProject_asHr_returns403() {
        postWithToken(
                hrToken,
                "/projects/PROJ-1/employees/5?role=MEMBER"
        )
        .then()
        .statusCode(403);
    }

   
    // WORK PACKAGE ASSIGNMENT ReBAC
    // Endpoint:
    // POST /workpackages/{id}/employees/{empId}?role=RE or MEMBER

    @Test
    void assignEmployeeToWorkPackage_asPmOfThatProject_succeeds() {
        Response response = postWithToken(
                pmProj1Token,
                "/workpackages/CA-1.WP-1/employees/5?role=MEMBER"
        );

        assertSuccess2xx(response);
    }

    @Test
    void assignEmployeeToWorkPackage_asOpsManager_returns403() {
        postWithToken(
                opsToken,
                "/workpackages/CA-1.WP-1/employees/5?role=MEMBER"
        )
        .then()
        .statusCode(403);
    }

    @Test
    void assignEmployeeToWorkPackage_asPmOfDifferentProject_returns403() {
        postWithToken(
                pmProj2Token,
                "/workpackages/CA-1.WP-1/employees/5?role=MEMBER"
        )
        .then()
        .statusCode(403);
    }

    @Test
    void assignEmployeeToWorkPackage_asRegularEmployee_returns403() {
        postWithToken(
                memberA2Token,
                "/workpackages/CA-1.WP-1/employees/4?role=MEMBER"
        )
        .then()
        .statusCode(403);
    }

    @Test
    void assignEmployeeToWorkPackage_asAssignedRe_returns403() {
        postWithToken(
                reA2Token,
                "/workpackages/CA-1.WP-1/employees/5?role=MEMBER"
        )
        .then()
        .statusCode(403);
    }

    // 
    // WORK PACKAGE OPEN/CLOSE ReBAC
    // Endpoint:
    // PUT /workpackages/{id}/close
    // PUT /workpackages/{id}/open
    //
    // Current logic:
    // assigned RE or PM of that project may do this
    

    @Test
    void closeWorkPackage_asAssignedRe_succeeds() {
        Response response = putWithToken(
                reA2Token,
                "/workpackages/CA-1.WP-2/close"
        );

        assertSuccess2xx(response);
    }

    @Test
    void openWorkPackage_asAssignedRe_succeeds() {
        Response response = putWithToken(
                reA2Token,
                "/workpackages/CA-1.WP-2/open"
        );

        assertSuccess2xx(response);
    }

    @Test
    void closeWorkPackage_asProjectPm_succeeds() {
        Response response = putWithToken(
                pmProj1Token,
                "/workpackages/CA-1.WP-2/close"
        );

        assertSuccess2xx(response);
    }

    @Test
    void openWorkPackage_asProjectPm_succeeds() {
        Response response = putWithToken(
                pmProj1Token,
                "/workpackages/CA-1.WP-2/open"
        );

        assertSuccess2xx(response);
    }

    @Test
    void closeWorkPackage_asUnrelatedPm_returns403() {
        putWithToken(
                pmProj2Token,
                "/workpackages/CA-1.WP-2/close"
        )
        .then()
        .statusCode(403);
    }

    @Test
    void openWorkPackage_asRegularEmployee_returns403() {
        putWithToken(
                memberA2Token,
                "/workpackages/CA-1.WP-2/open"
        )
        .then()
        .statusCode(403);
    }

    @Test
    void closeWorkPackage_asHr_returns403() {
        putWithToken(
                hrToken,
                "/workpackages/CA-1.WP-2/close"
        )
        .then()
        .statusCode(403);
    }

}