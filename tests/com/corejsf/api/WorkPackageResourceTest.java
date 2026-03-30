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
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Work package API tests. Active list-filtering tests reflect {@code ProjectService#getWorkPackages}
 * visibility rules. Uncomment the block at the bottom when iteration-2 features ship.
 */
@SuppressWarnings("unused")
class WorkPackageResourceTest extends TestConfig {

    private static StandardSeedIds IDS;
    private static String opsToken;
    private static String bugsToken;
    private static String daffyToken;
    private static String tweetyToken;
    private static String marvinToken;

    @BeforeAll
    static void setup() {
        opsToken = loginAsSeedOps();
        IDS = resolveStandardSeedIds(opsToken);
        bugsToken = login(IDS.pmProj1Id(), DEFAULT_PASSWORD);
        daffyToken = login(IDS.daffyId(), DEFAULT_PASSWORD);
        tweetyToken = login(IDS.tweetyId(), DEFAULT_PASSWORD);
        marvinToken = login(IDS.marvinPmProj2Id(), DEFAULT_PASSWORD);
        Objects.requireNonNull(opsToken, "seed OPS login");
        Objects.requireNonNull(bugsToken, "seed PM PROJ-1 login");
        Objects.requireNonNull(daffyToken, "seed Daffy login");
        Objects.requireNonNull(tweetyToken, "seed Tweety login");
        Objects.requireNonNull(marvinToken, "seed Marvin login");
    }

    @Test
    void getAll_returns200() {
        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/workpackages")
                .then()
                .statusCode(200);
    }

    @Test
    void getById_returnsSeedWorkPackage() {
        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/workpackages/A.WP-1")
                .then()
                .statusCode(200)
                .body("wpId", org.hamcrest.Matchers.equalTo("A.WP-1"));
    }

    @Test
    void getProjectWorkPackages_asPm_returnsFullList() {
        List<?> pmList = given()
                .header("Authorization", "Bearer " + bugsToken)
                .when()
                .get("/projects/PROJ-1/workpackages")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");
        assertNotNull(pmList);
        assertTrue(pmList.size() >= 2, "PM should see the full WP tree for PROJ-1");
    }

    @Test
    void getProjectWorkPackages_asOps_returnsFullList() {
        List<?> pmList = given()
                .header("Authorization", "Bearer " + bugsToken)
                .when()
                .get("/projects/PROJ-1/workpackages")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");
        List<?> opsList = given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/projects/PROJ-1/workpackages")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");
        assertNotNull(pmList);
        assertNotNull(opsList);
        assertTrue(pmList.size() >= 2);
        assertEquals(pmList.size(), opsList.size(), "OPS should see the same WP list as PM");
    }

    @Test
    void getProjectWorkPackages_asAssignedEmployee_returnsOnlyAssigned() {
        List<?> pmList = given()
                .header("Authorization", "Bearer " + bugsToken)
                .when()
                .get("/projects/PROJ-1/workpackages")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");
        List<?> tweetyList = given()
                .header("Authorization", "Bearer " + tweetyToken)
                .when()
                .get("/projects/PROJ-1/workpackages")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");
        assertNotNull(pmList);
        assertNotNull(tweetyList);
        assertTrue(tweetyList.size() <= pmList.size(),
                "Non-PM member should not see more WPs than the project manager");
    }

    @Test
    void create_asPm_thenDelete_succeeds() {
        String wpId = "AUTOWP" + Math.abs(System.nanoTime() % 1_000_000_000);
        given()
                .header("Authorization", "Bearer " + bugsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "wpId": "%s",
                          "wpName": "Integration auto work package",
                          "description": "test",
                          "projId": "PROJ-1",
                          "planStartDate": "2026-02-01",
                          "planEndDate": "2026-03-15",
                          "bac": 500.00,
                          "percentComplete": 0,
                          "reEmployeeId": %d
                        }
                        """.formatted(wpId, IDS.pmProj1Id()))
                .when()
                .post("/workpackages")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .when()
                .delete("/workpackages/" + wpId)
                .then()
                .statusCode(200);
    }

    @Test
    void create_asMember_returns403() {
        String wpId = "AUTOWP-X-" + Math.abs(System.nanoTime() % 1_000_000_000);
        given()
                .header("Authorization", "Bearer " + tweetyToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "wpId": "%s",
                          "wpName": "Should fail",
                          "description": "test",
                          "projId": "PROJ-1",
                          "planStartDate": "2026-02-01",
                          "planEndDate": "2026-03-15",
                          "bac": 100.00,
                          "percentComplete": 0,
                          "reEmployeeId": %d
                        }
                        """.formatted(wpId, IDS.pmProj1Id()))
                .when()
                .post("/workpackages")
                .then()
                .statusCode(403);
    }

    @Test
    void update_asResponsibleEngineer_succeeds() {
        given()
                .header("Authorization", "Bearer " + daffyToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "wpName": "Procure Anvil",
                          "description": "Procure Anvil",
                          "parentWpId": "A",
                          "reEmployeeId": %d,
                          "bac": 1500.00,
                          "percentComplete": 0.00,
                          "budgetedEffort": 0.00
                        }
                        """.formatted(IDS.daffyId()))
                .when()
                .put("/workpackages/A.WP-1")
                .then()
                .statusCode(200);
    }

    @Test
    void getChildren_ofRootSummary_returnsWorkPackages() {
        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/workpackages/A/children")
                .then()
                .statusCode(200);
    }

    @Test
    void getParent_ofChild_returnsSummary() {
        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/workpackages/A.WP-1/parent")
                .then()
                .statusCode(200)
                .body("wpId", org.hamcrest.Matchers.equalTo("A"));
    }

    @Test
    void getReport_returnsPlainText() {
        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/workpackages/A.WP-1/report")
                .then()
                .statusCode(200)
                .contentType(containsString("text/plain"))
                .body(containsString("Work Package Report"));
    }

    /*
    @Test
    void update_changeBac_returns400() {
        String wpId = "BACIM" + Math.abs(System.nanoTime() % 1_000_000_000);
        given()
                .header("Authorization", "Bearer " + bugsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "wpId": "%s",
                          "wpName": "BAC immutability probe",
                          "description": "test",
                          "projId": "PROJ-1",
                          "planStartDate": "2026-02-01",
                          "planEndDate": "2026-03-15",
                          "bac": 500.00,
                          "percentComplete": 0,
                          "reEmployeeId": %d
                        }
                        """.formatted(wpId, IDS.pmProj1Id()))
                .when()
                .post("/workpackages")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "wpName": "BAC immutability probe",
                          "description": "test",
                          "parentWpId": "A",
                          "reEmployeeId": %d,
                          "bac": 999.00,
                          "percentComplete": 0.00,
                          "budgetedEffort": 0.00
                        }
                        """.formatted(IDS.pmProj1Id()))
                .when()
                .put("/workpackages/" + wpId)
                .then()
                .statusCode(400)
                .body(containsString("BAC cannot be changed"));

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .when()
                .delete("/workpackages/" + wpId)
                .then()
                .statusCode(200);
    }

    @Test
    void assignToWp_notOnProject_returns400() {
        given()
                .header("Authorization", "Bearer " + bugsToken)
                .contentType(ContentType.JSON)
                .when()
                .post("/workpackages/A.WP-1/employees/" + IDS.marvinPmProj2Id() + "?role=MEMBER")
                .then()
                .statusCode(400);
    }

    @Test
    void assignToWp_onProject_succeeds() {
        given()
                .header("Authorization", "Bearer " + bugsToken)
                .contentType(ContentType.JSON)
                .when()
                .post("/workpackages/A.WP-1/employees/" + IDS.tweetyId() + "?role=MEMBER")
                .then()
                .statusCode(200);
    }

    @Test
    void getById_includesEtcField() {
        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/workpackages/A.WP-1")
                .then()
                .statusCode(200)
                .body("$", org.hamcrest.Matchers.hasKey("etc"));
    }

    @Test
    void updateEtc_asRe_succeeds() {
        given()
                .header("Authorization", "Bearer " + daffyToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "wpName": "Procure Anvil",
                          "description": "Procure Anvil",
                          "parentWpId": "A",
                          "reEmployeeId": %d,
                          "bac": 1500.00,
                          "percentComplete": 0.00,
                          "budgetedEffort": 0.00,
                          "etc": 120.5
                        }
                        """.formatted(IDS.daffyId()))
                .when()
                .put("/workpackages/A.WP-1")
                .then()
                .statusCode(200);
    }

    @Test
    void updateEtc_asNonRe_returns403() {
        given()
                .header("Authorization", "Bearer " + tweetyToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "wpName": "Procure Anvil",
                          "description": "Procure Anvil",
                          "parentWpId": "A",
                          "reEmployeeId": %d,
                          "bac": 1500.00,
                          "percentComplete": 0.00,
                          "budgetedEffort": 0.00,
                          "etc": 99.0
                        }
                        """.formatted(IDS.daffyId()))
                .when()
                .put("/workpackages/A.WP-1")
                .then()
                .statusCode(403);
    }

    @Test
    void structureLocked_falseBeforeApprovedCharge() {
        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/workpackages/A.WP-1")
                .then()
                .statusCode(200)
                .body("structureLocked", org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.nullValue(),
                        org.hamcrest.Matchers.equalTo(false)));
    }

    @Test
    void structureLocked_trueAfterApprovedCharge() {
        LocalDate weekEnding = LocalDate.now()
                .plusWeeks(120 + (System.nanoTime() % 400))
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        int tsId = given()
                .header("Authorization", "Bearer " + daffyToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "empId": %d,
                          "weekEnding": "%s",
                          "rows": [
                            {
                              "wpId": "A.WP-1",
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
                        """.formatted(IDS.daffyId(), weekEnding))
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
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/workpackages/A.WP-1")
                .then()
                .statusCode(200)
                .body("structureLocked", org.hamcrest.Matchers.equalTo(true));
    }

    @Test
    void allocateBac_withinProjectBudget_succeeds() {
        String projId = "BAC-OK-" + System.nanoTime();
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(6);

        given()
                .header("Authorization", "Bearer " + opsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "project_id": "%s",
                          "project_name": "BAC within budget",
                          "project_desc": "sum test",
                          "project_type": "INTERNAL",
                          "start_date": "%s",
                          "end_date": "%s",
                          "markup_rate": 10.00,
                          "project_manager_id": %d,
                          "bac": 10000.00
                        }
                        """.formatted(projId, start, end, IDS.pmProj1Id()))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        String wpA = "WPA" + Math.abs(System.nanoTime() % 1_000_000_000);
        String wpB = "WPB" + Math.abs(System.nanoTime() % 1_000_000_000);
        String wpC = "WPC" + Math.abs(System.nanoTime() % 1_000_000_000);

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "wpId": "%s",
                          "wpName": "WP-A",
                          "description": "bac 4000",
                          "projId": "%s",
                          "planStartDate": "%s",
                          "planEndDate": "%s",
                          "bac": 4000.00,
                          "percentComplete": 0,
                          "reEmployeeId": %d
                        }
                        """.formatted(wpA, projId, start, end, IDS.pmProj1Id()))
                .when()
                .post("/workpackages")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "wpId": "%s",
                          "wpName": "WP-B",
                          "description": "bac 3000",
                          "projId": "%s",
                          "planStartDate": "%s",
                          "planEndDate": "%s",
                          "bac": 3000.00,
                          "percentComplete": 0,
                          "reEmployeeId": %d
                        }
                        """.formatted(wpB, projId, start, end, IDS.pmProj1Id()))
                .when()
                .post("/workpackages")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "wpId": "%s",
                          "wpName": "WP-C",
                          "description": "bac 3000",
                          "projId": "%s",
                          "planStartDate": "%s",
                          "planEndDate": "%s",
                          "bac": 3000.00,
                          "percentComplete": 0,
                          "reEmployeeId": %d
                        }
                        """.formatted(wpC, projId, start, end, IDS.pmProj1Id()))
                .when()
                .post("/workpackages")
                .then()
                .statusCode(201);

        given().header("Authorization", "Bearer " + bugsToken).delete("/workpackages/" + wpC).then().statusCode(200);
        given().header("Authorization", "Bearer " + bugsToken).delete("/workpackages/" + wpB).then().statusCode(200);
        given().header("Authorization", "Bearer " + bugsToken).delete("/workpackages/" + wpA).then().statusCode(200);
        given().header("Authorization", "Bearer " + opsToken).delete("/projects/" + projId).then().statusCode(200);
    }

    @Test
    void allocateBac_exceedingProjectBudget_returns400() {
        String projId = "BAC-EX-" + System.nanoTime();
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(6);

        given()
                .header("Authorization", "Bearer " + opsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "project_id": "%s",
                          "project_name": "BAC exceed budget",
                          "project_desc": "sum test",
                          "project_type": "INTERNAL",
                          "start_date": "%s",
                          "end_date": "%s",
                          "markup_rate": 10.00,
                          "project_manager_id": %d,
                          "bac": 10000.00
                        }
                        """.formatted(projId, start, end, IDS.pmProj1Id()))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        String wpA = "WPA" + Math.abs(System.nanoTime() % 1_000_000_000);
        String wpB = "WPB" + Math.abs(System.nanoTime() % 1_000_000_000);

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "wpId": "%s",
                          "wpName": "WP-A",
                          "description": "bac 6000",
                          "projId": "%s",
                          "planStartDate": "%s",
                          "planEndDate": "%s",
                          "bac": 6000.00,
                          "percentComplete": 0,
                          "reEmployeeId": %d
                        }
                        """.formatted(wpA, projId, start, end, IDS.pmProj1Id()))
                .when()
                .post("/workpackages")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "wpId": "%s",
                          "wpName": "WP-B",
                          "description": "bac 5000 -- exceeds project",
                          "projId": "%s",
                          "planStartDate": "%s",
                          "planEndDate": "%s",
                          "bac": 5000.00,
                          "percentComplete": 0,
                          "reEmployeeId": %d
                        }
                        """.formatted(wpB, projId, start, end, IDS.pmProj1Id()))
                .when()
                .post("/workpackages")
                .then()
                .statusCode(400);

        given().header("Authorization", "Bearer " + bugsToken).delete("/workpackages/" + wpA).then().statusCode(200);
        given().header("Authorization", "Bearer " + opsToken).delete("/projects/" + projId).then().statusCode(200);
    }

    @Test
    void updateBac_exceedingProjectBudget_returns400() {
        String projId = "BAC-UPD-" + System.nanoTime();
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(6);

        given()
                .header("Authorization", "Bearer " + opsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "project_id": "%s",
                          "project_name": "BAC update exceed",
                          "project_desc": "sum test",
                          "project_type": "INTERNAL",
                          "start_date": "%s",
                          "end_date": "%s",
                          "markup_rate": 10.00,
                          "project_manager_id": %d,
                          "bac": 10000.00
                        }
                        """.formatted(projId, start, end, IDS.pmProj1Id()))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        String wpA = "WPA" + Math.abs(System.nanoTime() % 1_000_000_000);
        String wpB = "WPB" + Math.abs(System.nanoTime() % 1_000_000_000);

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "wpId": "%s",
                          "wpName": "WP-A",
                          "description": "bac 5000",
                          "projId": "%s",
                          "planStartDate": "%s",
                          "planEndDate": "%s",
                          "bac": 5000.00,
                          "percentComplete": 0,
                          "reEmployeeId": %d
                        }
                        """.formatted(wpA, projId, start, end, IDS.pmProj1Id()))
                .when()
                .post("/workpackages")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "wpId": "%s",
                          "wpName": "WP-B",
                          "description": "bac 4000",
                          "projId": "%s",
                          "planStartDate": "%s",
                          "planEndDate": "%s",
                          "bac": 4000.00,
                          "percentComplete": 0,
                          "reEmployeeId": %d
                        }
                        """.formatted(wpB, projId, start, end, IDS.pmProj1Id()))
                .when()
                .post("/workpackages")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "wpName": "WP-B",
                          "description": "bac 6000 -- exceeds project",
                          "reEmployeeId": %d,
                          "bac": 6000.00,
                          "percentComplete": 0,
                          "budgetedEffort": 0.00
                        }
                        """.formatted(IDS.pmProj1Id()))
                .when()
                .put("/workpackages/" + wpB)
                .then()
                .statusCode(400);

        given().header("Authorization", "Bearer " + bugsToken).delete("/workpackages/" + wpB).then().statusCode(200);
        given().header("Authorization", "Bearer " + bugsToken).delete("/workpackages/" + wpA).then().statusCode(200);
        given().header("Authorization", "Bearer " + opsToken).delete("/projects/" + projId).then().statusCode(200);
    }

    @Test
    void childWpBac_withinParentBudget_succeeds() {
        String projId = "BAC-CW-" + System.nanoTime();
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(6);

        given()
                .header("Authorization", "Bearer " + opsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "project_id": "%s",
                          "project_name": "Child BAC within parent",
                          "project_desc": "hierarchy test",
                          "project_type": "INTERNAL",
                          "start_date": "%s",
                          "end_date": "%s",
                          "markup_rate": 10.00,
                          "project_manager_id": %d,
                          "bac": 10000.00
                        }
                        """.formatted(projId, start, end, IDS.pmProj1Id()))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        String parentWp = "WPP" + Math.abs(System.nanoTime() % 1_000_000_000);
        String child1 = parentWp + ".1";
        String child2 = parentWp + ".2";

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "wpId": "%s",
                          "wpName": "Parent WP-P",
                          "description": "parent bac 6000",
                          "projId": "%s",
                          "planStartDate": "%s",
                          "planEndDate": "%s",
                          "bac": 6000.00,
                          "percentComplete": 0,
                          "reEmployeeId": %d
                        }
                        """.formatted(parentWp, projId, start, end, IDS.pmProj1Id()))
                .when()
                .post("/workpackages")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "wpId": "%s",
                          "wpName": "Child WP-P.1",
                          "description": "child bac 3000",
                          "projId": "%s",
                          "parentWpId": "%s",
                          "planStartDate": "%s",
                          "planEndDate": "%s",
                          "bac": 3000.00,
                          "percentComplete": 0,
                          "reEmployeeId": %d
                        }
                        """.formatted(child1, projId, parentWp, start, end, IDS.pmProj1Id()))
                .when()
                .post("/workpackages")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "wpId": "%s",
                          "wpName": "Child WP-P.2",
                          "description": "child bac 3000",
                          "projId": "%s",
                          "parentWpId": "%s",
                          "planStartDate": "%s",
                          "planEndDate": "%s",
                          "bac": 3000.00,
                          "percentComplete": 0,
                          "reEmployeeId": %d
                        }
                        """.formatted(child2, projId, parentWp, start, end, IDS.pmProj1Id()))
                .when()
                .post("/workpackages")
                .then()
                .statusCode(201);

        given().header("Authorization", "Bearer " + bugsToken).delete("/workpackages/" + child2).then().statusCode(200);
        given().header("Authorization", "Bearer " + bugsToken).delete("/workpackages/" + child1).then().statusCode(200);
        given().header("Authorization", "Bearer " + bugsToken).delete("/workpackages/" + parentWp).then().statusCode(200);
        given().header("Authorization", "Bearer " + opsToken).delete("/projects/" + projId).then().statusCode(200);
    }

    @Test
    void childWpBac_exceedingParentBudget_returns400() {
        String projId = "BAC-CX-" + System.nanoTime();
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(6);

        given()
                .header("Authorization", "Bearer " + opsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "project_id": "%s",
                          "project_name": "Child BAC exceed parent",
                          "project_desc": "hierarchy test",
                          "project_type": "INTERNAL",
                          "start_date": "%s",
                          "end_date": "%s",
                          "markup_rate": 10.00,
                          "project_manager_id": %d,
                          "bac": 10000.00
                        }
                        """.formatted(projId, start, end, IDS.pmProj1Id()))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        String parentWp = "WPP" + Math.abs(System.nanoTime() % 1_000_000_000);
        String child1 = parentWp + ".1";
        String child2 = parentWp + ".2";

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "wpId": "%s",
                          "wpName": "Parent WP-P",
                          "description": "parent bac 6000",
                          "projId": "%s",
                          "planStartDate": "%s",
                          "planEndDate": "%s",
                          "bac": 6000.00,
                          "percentComplete": 0,
                          "reEmployeeId": %d
                        }
                        """.formatted(parentWp, projId, start, end, IDS.pmProj1Id()))
                .when()
                .post("/workpackages")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "wpId": "%s",
                          "wpName": "Child WP-P.1",
                          "description": "child bac 4000",
                          "projId": "%s",
                          "parentWpId": "%s",
                          "planStartDate": "%s",
                          "planEndDate": "%s",
                          "bac": 4000.00,
                          "percentComplete": 0,
                          "reEmployeeId": %d
                        }
                        """.formatted(child1, projId, parentWp, start, end, IDS.pmProj1Id()))
                .when()
                .post("/workpackages")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "wpId": "%s",
                          "wpName": "Child WP-P.2",
                          "description": "child bac 3000 -- exceeds parent",
                          "projId": "%s",
                          "parentWpId": "%s",
                          "planStartDate": "%s",
                          "planEndDate": "%s",
                          "bac": 3000.00,
                          "percentComplete": 0,
                          "reEmployeeId": %d
                        }
                        """.formatted(child2, projId, parentWp, start, end, IDS.pmProj1Id()))
                .when()
                .post("/workpackages")
                .then()
                .statusCode(400);

        given().header("Authorization", "Bearer " + bugsToken).delete("/workpackages/" + child1).then().statusCode(200);
        given().header("Authorization", "Bearer " + bugsToken).delete("/workpackages/" + parentWp).then().statusCode(200);
        given().header("Authorization", "Bearer " + opsToken).delete("/projects/" + projId).then().statusCode(200);
    }
    */
}
