package com.corejsf.Api;

import com.corejsf.TestConfig;
import com.corejsf.TestConfig.StandardSeedIds;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

class WorkPackageResourceTest extends TestConfig {

    private static StandardSeedIds IDS;
    private static String opsToken;
    private static String bugsToken;
    private static String daffyToken;
    private static String tweetyToken;

    @BeforeAll
    static void setup() {
        opsToken = loginAsSeedOps();
        IDS = resolveStandardSeedIds(opsToken);
        bugsToken = login(IDS.pmProj1Id(), DEFAULT_PASSWORD);
        daffyToken = login(IDS.daffyId(), DEFAULT_PASSWORD);
        tweetyToken = login(IDS.tweetyId(), DEFAULT_PASSWORD);
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
                .get("/workpackages/CA-1.WP-1")
                .then()
                .statusCode(200)
                .body("wpId", org.hamcrest.Matchers.equalTo("CA-1.WP-1"));
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
                .put("/workpackages/CA-1.WP-1")
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
                .get("/workpackages/CA-1.WP-1/parent")
                .then()
                .statusCode(200)
                .body("wpId", org.hamcrest.Matchers.equalTo("A"));
    }

    @Test
    void getReport_returnsPlainText() {
        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/workpackages/CA-1.WP-1/report")
                .then()
                .statusCode(200)
                .contentType(containsString("text/plain"))
                .body(containsString("Work Package Report"));
    }
}
