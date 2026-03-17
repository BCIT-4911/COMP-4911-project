package com.corejsf.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

public class TimesheetLifecycleTest {

    private static final String PASSWORD = "password";

    private static final int EMPLOYEE_ID = 4;   // Daffy Duck
    private static final int SUPERVISOR_ID = 3; // Bugs Bunny

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;
        RestAssured.basePath = "/Project/api";
    }

    private static String login(final int empId, final String password) {
        final Response response = given()
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

        final String token = response.jsonPath().getString("token");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Login succeeded but token was missing for empId=" + empId);
        }
        return token;
    }

    @Test
    void loginWorks() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "empId": 4,
                          "password": "password"
                        }
                        """)
        .when()
                .post("/auth/login")
        .then()
                .statusCode(200)
                .body("token", containsString("."));
    }

    private static String buildTimesheetRequest(final int empId,
                                                final String weekEnding,
                                                final String fridayHours) {
        return """
                {
                  "empId": %d,
                  "weekEnding": "%s",
                  "rows": [
                    {
                      "wpId": "CA-1.WP-1",
                      "laborGradeId": 1,
                      "monday": 8.0,
                      "tuesday": 8.0,
                      "wednesday": 8.0,
                      "thursday": 8.0,
                      "friday": %s,
                      "saturday": 0.0,
                      "sunday": 0.0
                    }
                  ]
                }
                """.formatted(empId, weekEnding, fridayHours);
    }

    private static int createDraftTimesheet(final String employeeToken, final String weekEnding) {
        final Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + employeeToken)
                .body(buildTimesheetRequest(EMPLOYEE_ID, weekEnding, "8.0"))
        .when()
                .post("/timesheets")
        .then()
                .statusCode(201)
                .body("status", equalTo("DRAFT"))
                .body("empId", equalTo(EMPLOYEE_ID))
                .extract()
                .response();

        return JsonPath.from(response.asString()).getInt("tsId");
    }

    @Test
    void draftToSubmittedToApproved() {
        final String employeeToken = login(EMPLOYEE_ID, PASSWORD);
        final String supervisorToken = login(SUPERVISOR_ID, PASSWORD);

        final int tsId = createDraftTimesheet(employeeToken, "2026-03-15");

        given()
                .header("Authorization", "Bearer " + employeeToken)
        .when()
                .put("/timesheets/{id}/submit", tsId)
        .then()
                .statusCode(200)
                .body("status", equalTo("SUBMITTED"))
                .body("approverId", equalTo(SUPERVISOR_ID))
                .body("approverName", containsString("Bugs"));

        given()
                .header("Authorization", "Bearer " + supervisorToken)
        .when()
                .put("/timesheets/{id}/approve", tsId)
        .then()
                .statusCode(200)
                .body("status", equalTo("APPROVED"))
                .body("approverId", equalTo(SUPERVISOR_ID));
    }

    @Test
    void submitReturnEditAndResubmit() {
        final String employeeToken = login(EMPLOYEE_ID, PASSWORD);
        final String supervisorToken = login(SUPERVISOR_ID, PASSWORD);

        final int tsId = createDraftTimesheet(employeeToken, "2026-03-22");

        given()
                .header("Authorization", "Bearer " + employeeToken)
        .when()
                .put("/timesheets/{id}/submit", tsId)
        .then()
                .statusCode(200)
                .body("status", equalTo("SUBMITTED"))
                .body("approverId", equalTo(SUPERVISOR_ID));

        final String returnBody = """
                {
                  "returnComment": "Please fix your hours."
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + supervisorToken)
                .body(returnBody)
        .when()
                .put("/timesheets/{id}/return", tsId)
        .then()
                .statusCode(200)
                .body("status", equalTo("RETURNED"))
                .body("returnComment", equalTo("Please fix your hours."));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + employeeToken)
                .body(buildTimesheetRequest(EMPLOYEE_ID, "2026-03-22", "6.0"))
        .when()
                .put("/timesheets/{id}", tsId)
        .then()
                .statusCode(200)
                .body("status", equalTo("RETURNED"))
                .body("rows[0].friday", equalTo(6.0f));

        given()
                .header("Authorization", "Bearer " + employeeToken)
        .when()
                .put("/timesheets/{id}/submit", tsId)
        .then()
                .statusCode(200)
                .body("status", equalTo("SUBMITTED"))
                .body("approverId", equalTo(SUPERVISOR_ID));
    }

    @Test
    void editSubmittedRejected() {
        final String employeeToken = login(EMPLOYEE_ID, PASSWORD);

        final int tsId = createDraftTimesheet(employeeToken, "2026-03-29");

        given()
                .header("Authorization", "Bearer " + employeeToken)
        .when()
                .put("/timesheets/{id}/submit", tsId)
        .then()
                .statusCode(200)
                .body("status", equalTo("SUBMITTED"));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + employeeToken)
                .body(buildTimesheetRequest(EMPLOYEE_ID, "2026-03-29", "7.0"))
        .when()
                .put("/timesheets/{id}", tsId)
        .then()
                .statusCode(400);
    }

    @Test
    void editApprovedRejected() {
        final String employeeToken = login(EMPLOYEE_ID, PASSWORD);
        final String supervisorToken = login(SUPERVISOR_ID, PASSWORD);

        final int tsId = createDraftTimesheet(employeeToken, "2026-04-05");

        given()
                .header("Authorization", "Bearer " + employeeToken)
        .when()
                .put("/timesheets/{id}/submit", tsId)
        .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + supervisorToken)
        .when()
                .put("/timesheets/{id}/approve", tsId)
        .then()
                .statusCode(200)
                .body("status", equalTo("APPROVED"));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + employeeToken)
                .body(buildTimesheetRequest(EMPLOYEE_ID, "2026-04-05", "7.0"))
        .when()
                .put("/timesheets/{id}", tsId)
        .then()
                .statusCode(400);
    }

    @Test
    void deleteApprovedRejected() {
        final String employeeToken = login(EMPLOYEE_ID, PASSWORD);
        final String supervisorToken = login(SUPERVISOR_ID, PASSWORD);

        final int tsId = createDraftTimesheet(employeeToken, "2026-04-12");

        given()
                .header("Authorization", "Bearer " + employeeToken)
        .when()
                .put("/timesheets/{id}/submit", tsId)
        .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + supervisorToken)
        .when()
                .put("/timesheets/{id}/approve", tsId)
        .then()
                .statusCode(200)
                .body("status", equalTo("APPROVED"));

        given()
                .header("Authorization", "Bearer " + employeeToken)
        .when()
                .delete("/timesheets/{id}", tsId)
        .then()
                .statusCode(400);
    }
}