package com.corejsf.Api;

import com.corejsf.TestConfig;
import com.corejsf.TestConfig.StandardSeedIds;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Timesheet API integration tests. Active tests cover immutability, identity, and approver queue.
 * Uncomment the block at the bottom when WP Charge Authorization is implemented.
 */
@SuppressWarnings("unused")
class TimesheetResourceTest extends TestConfig {

    private static StandardSeedIds IDS;
    private static String opsToken;
    private static String daffyToken;
    private static String bugsToken;
    private static String tweetyToken;
    private static String marvinToken;

    @BeforeAll
    static void setup() {
        opsToken = loginAsSeedOps();
        IDS = resolveStandardSeedIds(opsToken);
        daffyToken = login(IDS.daffyId(), DEFAULT_PASSWORD);
        bugsToken = login(IDS.pmProj1Id(), DEFAULT_PASSWORD);
        tweetyToken = login(IDS.tweetyId(), DEFAULT_PASSWORD);
        marvinToken = login(IDS.marvinPmProj2Id(), DEFAULT_PASSWORD);
        Objects.requireNonNull(opsToken, "seed OPS login");
        Objects.requireNonNull(daffyToken, "seed Daffy login");
        Objects.requireNonNull(bugsToken, "seed PM PROJ-1 login");
        Objects.requireNonNull(tweetyToken, "seed Tweety login");
        Objects.requireNonNull(marvinToken, "seed Marvin login");
    }

    private static LocalDate uniqueWeekEnding() {
        long salt = System.nanoTime() % 400;
        return LocalDate.now()
                .plusWeeks(120 + salt)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    @Test
    void create_get_update_submit_approve_happyPath() {
        LocalDate weekEnding = uniqueWeekEnding();
        int tsId = given()
                .header("Authorization", "Bearer " + daffyToken)
                .contentType(ContentType.JSON)
                .body(buildTimesheetJson(IDS.daffyId(), weekEnding))
                .when()
                .post("/timesheets")
                .then()
                .statusCode(201)
                .body("status", equalTo("DRAFT"))
                .extract()
                .path("tsId");

        given()
                .header("Authorization", "Bearer " + daffyToken)
                .when()
                .get("/timesheets/" + tsId)
                .then()
                .statusCode(200)
                .body("tsId", equalTo(tsId));

        given()
                .header("Authorization", "Bearer " + daffyToken)
                .when()
                .get("/timesheets")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + daffyToken)
                .contentType(ContentType.JSON)
                .body(buildTimesheetJson(IDS.daffyId(), weekEnding))
                .when()
                .put("/timesheets/" + tsId)
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + daffyToken)
                .when()
                .put("/timesheets/" + tsId + "/submit")
                .then()
                .statusCode(200)
                .body("status", equalTo("SUBMITTED"));

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .when()
                .put("/timesheets/" + tsId + "/approve")
                .then()
                .statusCode(200)
                .body("status", equalTo("APPROVED"));
    }

    @Test
    void submit_return_thenDelete() {
        LocalDate weekEnding = uniqueWeekEnding();
        int tsId = given()
                .header("Authorization", "Bearer " + daffyToken)
                .contentType(ContentType.JSON)
                .body(buildTimesheetJson(IDS.daffyId(), weekEnding))
                .when()
                .post("/timesheets")
                .then()
                .statusCode(201)
                .extract()
                .path("tsId");

        given()
                .header("Authorization", "Bearer " + daffyToken)
                .when()
                .put("/timesheets/" + tsId + "/submit")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .contentType(ContentType.JSON)
                .body("{\"returnComment\": \"Please fix hours on Monday.\"}")
                .when()
                .put("/timesheets/" + tsId + "/return")
                .then()
                .statusCode(200)
                .body("status", equalTo("RETURNED"));

        given()
                .header("Authorization", "Bearer " + daffyToken)
                .when()
                .delete("/timesheets/" + tsId)
                .then()
                .statusCode(200);
    }

    @Test
    void editTimesheet_whenSubmitted_returns400() {
        LocalDate weekEnding = uniqueWeekEnding();
        int tsId = given()
                .header("Authorization", "Bearer " + daffyToken)
                .contentType(ContentType.JSON)
                .body(buildTimesheetJson(IDS.daffyId(), weekEnding))
                .when()
                .post("/timesheets")
                .then()
                .statusCode(201)
                .extract()
                .path("tsId");

        given()
                .header("Authorization", "Bearer " + daffyToken)
                .when()
                .put("/timesheets/" + tsId + "/submit")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + daffyToken)
                .contentType(ContentType.JSON)
                .body(buildTimesheetJson(IDS.daffyId(), weekEnding))
                .when()
                .put("/timesheets/" + tsId)
                .then()
                .statusCode(400);
    }

    @Test
    void editTimesheet_whenApproved_returns400() {
        LocalDate weekEnding = uniqueWeekEnding();
        int tsId = given()
                .header("Authorization", "Bearer " + daffyToken)
                .contentType(ContentType.JSON)
                .body(buildTimesheetJson(IDS.daffyId(), weekEnding))
                .when()
                .post("/timesheets")
                .then()
                .statusCode(201)
                .extract()
                .path("tsId");

        given()
                .header("Authorization", "Bearer " + daffyToken)
                .when()
                .put("/timesheets/" + tsId + "/submit")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .when()
                .put("/timesheets/" + tsId + "/approve")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + daffyToken)
                .contentType(ContentType.JSON)
                .body(buildTimesheetJson(IDS.daffyId(), weekEnding))
                .when()
                .put("/timesheets/" + tsId)
                .then()
                .statusCode(400);
    }

    @Test
    void deleteTimesheet_whenSubmitted_returns400() {
        LocalDate weekEnding = uniqueWeekEnding();
        int tsId = given()
                .header("Authorization", "Bearer " + daffyToken)
                .contentType(ContentType.JSON)
                .body(buildTimesheetJson(IDS.daffyId(), weekEnding))
                .when()
                .post("/timesheets")
                .then()
                .statusCode(201)
                .extract()
                .path("tsId");

        given()
                .header("Authorization", "Bearer " + daffyToken)
                .when()
                .put("/timesheets/" + tsId + "/submit")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + daffyToken)
                .when()
                .delete("/timesheets/" + tsId)
                .then()
                .statusCode(400);
    }

    @Test
    void deleteTimesheet_whenApproved_returns400() {
        LocalDate weekEnding = uniqueWeekEnding();
        int tsId = given()
                .header("Authorization", "Bearer " + daffyToken)
                .contentType(ContentType.JSON)
                .body(buildTimesheetJson(IDS.daffyId(), weekEnding))
                .when()
                .post("/timesheets")
                .then()
                .statusCode(201)
                .extract()
                .path("tsId");

        given()
                .header("Authorization", "Bearer " + daffyToken)
                .when()
                .put("/timesheets/" + tsId + "/submit")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .when()
                .put("/timesheets/" + tsId + "/approve")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + daffyToken)
                .when()
                .delete("/timesheets/" + tsId)
                .then()
                .statusCode(400);
    }

    @Test
    void create_asDifferentEmployee_returns403() {
        LocalDate weekEnding = uniqueWeekEnding();
        given()
                .header("Authorization", "Bearer " + daffyToken)
                .contentType(ContentType.JSON)
                .body(buildTimesheetJson(IDS.tweetyId(), weekEnding))
                .when()
                .post("/timesheets")
                .then()
                .statusCode(403);
    }

    @Test
    void getAll_withNoParams_returnsOnlyOwnTimesheets() {
        LocalDate weekEnding = uniqueWeekEnding();
        given()
                .header("Authorization", "Bearer " + daffyToken)
                .contentType(ContentType.JSON)
                .body(buildTimesheetJson(IDS.daffyId(), weekEnding))
                .when()
                .post("/timesheets")
                .then()
                .statusCode(201);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = given()
                .header("Authorization", "Bearer " + daffyToken)
                .when()
                .get("/timesheets")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");

        for (Map<String, Object> row : list) {
            int empId = ((Number) row.get("empId")).intValue();
            assertTrue(empId == IDS.daffyId(), "GET /timesheets without filters must return only the caller's timesheets");
        }
    }

    @Test
    void getOwnTimesheet_asEmployee_returns200() {
        LocalDate weekEnding = uniqueWeekEnding();
        int tsId = given()
                .header("Authorization", "Bearer " + tweetyToken)
                .contentType(ContentType.JSON)
                .body(buildTimesheetJsonForWp(IDS.tweetyId(), weekEnding, "CA-1.WP-2"))
                .when()
                .post("/timesheets")
                .then()
                .statusCode(201)
                .extract()
                .path("tsId");

        given()
                .header("Authorization", "Bearer " + tweetyToken)
                .when()
                .get("/timesheets/" + tsId)
                .then()
                .statusCode(200)
                .body("tsId", equalTo(tsId))
                .body("empId", equalTo(IDS.tweetyId()));

        given()
                .header("Authorization", "Bearer " + tweetyToken)
                .when()
                .delete("/timesheets/" + tsId)
                .then()
                .statusCode(200);
    }

    @Test
    void getAll_asApprover_withStatusFilter_returnsSubmittedQueue() {
        LocalDate weekEnding = uniqueWeekEnding();
        int tsId = given()
                .header("Authorization", "Bearer " + daffyToken)
                .contentType(ContentType.JSON)
                .body(buildTimesheetJson(IDS.daffyId(), weekEnding))
                .when()
                .post("/timesheets")
                .then()
                .statusCode(201)
                .extract()
                .path("tsId");

        given()
                .header("Authorization", "Bearer " + daffyToken)
                .when()
                .put("/timesheets/" + tsId + "/submit")
                .then()
                .statusCode(200);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> queue = given()
                .header("Authorization", "Bearer " + bugsToken)
                .when()
                .get("/timesheets?approverId=" + IDS.pmProj1Id() + "&status=SUBMITTED")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");

        boolean found = false;
        for (Map<String, Object> ts : queue) {
            if (ts.get("tsId") instanceof Number n && n.intValue() == tsId) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Approver queue should include the submitted timesheet awaiting review");
    }

    private static String buildTimesheetJson(int empId, LocalDate weekEnding) {
        return buildTimesheetJsonForWp(empId, weekEnding, "CA-1.WP-1");
    }

    private static String buildTimesheetJsonForWp(int empId, LocalDate weekEnding, String wpId) {
        return """
                {
                  "empId": %d,
                  "weekEnding": "%s",
                  "rows": [
                    {
                      "wpId": "%s",
                      "laborGradeId": 1,
                      "monday": 8.0,
                      "tuesday": 8.0,
                      "wednesday": 8.0,
                      "thursday": 0,
                      "friday": 0,
                      "saturday": 0,
                      "sunday": 0
                    }
                  ]
                }
                """.formatted(empId, weekEnding, wpId);
    }

    /*
    @Test
    void create_chargingUnassignedWp_returns400() {
        LocalDate weekEnding = uniqueWeekEnding();
        given()
                .header("Authorization", "Bearer " + marvinToken)
                .contentType(ContentType.JSON)
                .body(buildTimesheetJson(IDS.marvinPmProj2Id(), weekEnding))
                .when()
                .post("/timesheets")
                .then()
                .statusCode(400)
                .body(containsString("not assigned"));
    }

    @Test
    void submit_chargingUnassignedWp_returns400() {
        LocalDate weekEnding = uniqueWeekEnding();
        int tsId = given()
                .header("Authorization", "Bearer " + daffyToken)
                .contentType(ContentType.JSON)
                .body(buildTimesheetJsonForWp(IDS.daffyId(), weekEnding, "CA-1.WP-2"))
                .when()
                .post("/timesheets")
                .then()
                .statusCode(201)
                .extract()
                .path("tsId");

        given()
                .header("Authorization", "Bearer " + daffyToken)
                .when()
                .put("/timesheets/" + tsId + "/submit")
                .then()
                .statusCode(400)
                .body(containsString("not assigned"));

        given()
                .header("Authorization", "Bearer " + daffyToken)
                .when()
                .delete("/timesheets/" + tsId)
                .then()
                .statusCode(200);
    }
    */
}
