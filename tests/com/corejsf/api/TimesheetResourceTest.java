package com.corejsf.Api;

import com.corejsf.TestConfig;
import com.corejsf.TestConfig.StandardSeedIds;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class TimesheetResourceTest extends TestConfig {

    private static StandardSeedIds IDS;
    private static String daffyToken;
    private static String bugsToken;

    @BeforeAll
    static void setup() {
        String opsToken = loginAsSeedOps();
        IDS = resolveStandardSeedIds(opsToken);
        daffyToken = login(IDS.daffyId(), DEFAULT_PASSWORD);
        bugsToken = login(IDS.pmProj1Id(), DEFAULT_PASSWORD);
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

    private static String buildTimesheetJson(int empId, LocalDate weekEnding) {
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
                      "thursday": 0,
                      "friday": 0,
                      "saturday": 0,
                      "sunday": 0
                    }
                  ]
                }
                """.formatted(empId, weekEnding);
    }
}
