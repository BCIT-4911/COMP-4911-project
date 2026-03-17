package com.corejsf.api;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;

class AuthFilterIntegrationTest {

    private static final String INVALID_BEARER = "Bearer this.is.not.a.valid.jwt";

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;
        RestAssured.basePath = "/Project/api";
    }

    // ---------- helpers ----------

    private void assertUnauthorizedWithoutToken(String method, String endpoint) {
        switch (method.toUpperCase()) {
            case "GET":
                given()
                        .when()
                        .get(endpoint)
                        .then()
                        .statusCode(401);
                break;

            case "POST":
                given()
                        .contentType(ContentType.JSON)
                        .body("{}")
                        .when()
                        .post(endpoint)
                        .then()
                        .statusCode(401);
                break;

            case "PUT":
                given()
                        .contentType(ContentType.JSON)
                        .body("{}")
                        .when()
                        .put(endpoint)
                        .then()
                        .statusCode(401);
                break;

            case "DELETE":
                given()
                        .when()
                        .delete(endpoint)
                        .then()
                        .statusCode(401);
                break;

            default:
                throw new IllegalArgumentException("Unsupported method: " + method);
        }
    }

    private void assertUnauthorizedWithInvalidToken(String method, String endpoint) {
        switch (method.toUpperCase()) {
            case "GET":
                given()
                        .header("Authorization", INVALID_BEARER)
                        .when()
                        .get(endpoint)
                        .then()
                        .statusCode(401);
                break;

            case "POST":
                given()
                        .header("Authorization", INVALID_BEARER)
                        .contentType(ContentType.JSON)
                        .body("{}")
                        .when()
                        .post(endpoint)
                        .then()
                        .statusCode(401);
                break;

            case "PUT":
                given()
                        .header("Authorization", INVALID_BEARER)
                        .contentType(ContentType.JSON)
                        .body("{}")
                        .when()
                        .put(endpoint)
                        .then()
                        .statusCode(401);
                break;

            case "DELETE":
                given()
                        .header("Authorization", INVALID_BEARER)
                        .when()
                        .delete(endpoint)
                        .then()
                        .statusCode(401);
                break;

            default:
                throw new IllegalArgumentException("Unsupported method: " + method);
        }
    }

    // ---------- no token -> 401 ----------

    @Test
    @DisplayName("GET /projects without token returns 401")
    void getProjects_withoutToken_returns401() {
        assertUnauthorizedWithoutToken("GET", "/projects");
    }

    @Test
    @DisplayName("GET /projects/{id} without token returns 401")
    void getProjectById_withoutToken_returns401() {
        assertUnauthorizedWithoutToken("GET", "/projects/1");
    }

    @Test
    @DisplayName("POST /projects without token returns 401")
    void createProject_withoutToken_returns401() {
        assertUnauthorizedWithoutToken("POST", "/projects");
    }

    @Test
    @DisplayName("GET /timesheets without token returns 401")
    void getTimesheets_withoutToken_returns401() {
        assertUnauthorizedWithoutToken("GET", "/timesheets");
    }

    @Test
    @DisplayName("GET /timesheets/{id} without token returns 401")
    void getTimesheetById_withoutToken_returns401() {
        assertUnauthorizedWithoutToken("GET", "/timesheets/1");
    }

    @Test
    @DisplayName("PUT /workpackages/{id} without token returns 401")
    void updateWorkPackage_withoutToken_returns401() {
        assertUnauthorizedWithoutToken("PUT", "/workpackages/1");
    }

    @Test
    @DisplayName("GET /workpackages without token returns 401")
    void getWorkPackages_withoutToken_returns401() {
        assertUnauthorizedWithoutToken("GET", "/workpackages");
    }

    @Test
    @DisplayName("POST /workpackages/{id}/employees/{empId} without token returns 401")
    void assignEmployeeToWorkPackage_withoutToken_returns401() {
        assertUnauthorizedWithoutToken("POST", "/workpackages/1/employees/100");
    }

    // ---------- invalid token -> 401 ----------

    @Test
    @DisplayName("GET /projects with invalid token returns 401")
    void getProjects_invalidToken_returns401() {
        assertUnauthorizedWithInvalidToken("GET", "/projects");
    }

    @Test
    @DisplayName("GET /projects/{id} with invalid token returns 401")
    void getProjectById_invalidToken_returns401() {
        assertUnauthorizedWithInvalidToken("GET", "/projects/1");
    }

    @Test
    @DisplayName("POST /projects with invalid token returns 401")
    void createProject_invalidToken_returns401() {
        assertUnauthorizedWithInvalidToken("POST", "/projects");
    }

    @Test
    @DisplayName("GET /timesheets with invalid token returns 401")
    void getTimesheets_invalidToken_returns401() {
        assertUnauthorizedWithInvalidToken("GET", "/timesheets");
    }

    @Test
    @DisplayName("GET /timesheets/{id} with invalid token returns 401")
    void getTimesheetById_invalidToken_returns401() {
        assertUnauthorizedWithInvalidToken("GET", "/timesheets/1");
    }

    @Test
    @DisplayName("PUT /workpackages/{id} with invalid token returns 401")
    void updateWorkPackage_invalidToken_returns401() {
        assertUnauthorizedWithInvalidToken("PUT", "/workpackages/1");
    }

    @Test
    @DisplayName("GET /workpackages with invalid token returns 401")
    void getWorkPackages_invalidToken_returns401() {
        assertUnauthorizedWithInvalidToken("GET", "/workpackages");
    }

    @Test
    @DisplayName("POST /workpackages/{id}/employees/{empId} with invalid token returns 401")
    void assignEmployeeToWorkPackage_invalidToken_returns401() {
        assertUnauthorizedWithInvalidToken("POST", "/workpackages/1/employees/100");
    }
}