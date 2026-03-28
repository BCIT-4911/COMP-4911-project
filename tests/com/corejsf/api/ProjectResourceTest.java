package com.corejsf.Api;

import com.corejsf.TestConfig;
import com.corejsf.TestConfig.StandardSeedIds;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Project API tests. Uncomment the block at the bottom when iteration-2 project close cascade,
 * reopen semantics, and project-level BAC are implemented.
 */
@SuppressWarnings("unused")
class ProjectResourceTest extends TestConfig {

    private static StandardSeedIds IDS;
    private static String opsToken;
    private static String bugsToken;
    private static String daffyToken;
    private static String marvinToken;
    private static String tweetyToken;

    @BeforeAll
    static void setup() {
        opsToken = loginAsSeedOps();
        IDS = resolveStandardSeedIds(opsToken);
        bugsToken = login(IDS.pmProj1Id(), DEFAULT_PASSWORD);
        daffyToken = login(IDS.daffyId(), DEFAULT_PASSWORD);
        marvinToken = login(IDS.marvinPmProj2Id(), DEFAULT_PASSWORD);
        tweetyToken = login(IDS.tweetyId(), DEFAULT_PASSWORD);
        Objects.requireNonNull(opsToken, "seed OPS login");
        Objects.requireNonNull(bugsToken, "seed PM PROJ-1 login");
        Objects.requireNonNull(daffyToken, "seed Daffy login");
        Objects.requireNonNull(marvinToken, "seed Marvin login");
        Objects.requireNonNull(tweetyToken, "seed Tweety login");
    }

    @Test
    void getAll_asOperationsManager_returnsProjects() {
        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/projects")
                .then()
                .statusCode(200);
    }

    @Test
    void getById_returnsProject() {
        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/projects/PROJ-1")
                .then()
                .statusCode(200)
                .body("project_id", org.hamcrest.Matchers.equalTo("PROJ-1"));
    }

    @Test
    void create_asOperationsManager_thenDelete_returns201and200() {
        String projId = "AUTO-P-" + System.nanoTime();
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(6);

        given()
                .header("Authorization", "Bearer " + opsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "project_id": "%s",
                          "project_name": "Auto Test Project",
                          "project_desc": "Integration test create/delete",
                          "project_type": "INTERNAL",
                          "start_date": "%s",
                          "end_date": "%s",
                          "markup_rate": 10.50,
                          "project_manager_id": %d
                        }
                        """.formatted(projId, start, end, IDS.pmProj1Id()))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .delete("/projects/" + projId)
                .then()
                .statusCode(200);
    }

    @Test
    void create_asRegularEmployee_returns403() {
        String projId = "AUTO-P-X-" + System.nanoTime();
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(1);
        given()
                .header("Authorization", "Bearer " + tweetyToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "project_id": "%s",
                          "project_name": "Should Fail",
                          "project_desc": "No permission",
                          "project_type": "INTERNAL",
                          "start_date": "%s",
                          "end_date": "%s",
                          "markup_rate": 10.00,
                          "project_manager_id": %d
                        }
                        """.formatted(projId, start, end, IDS.pmProj1Id()))
                .when()
                .post("/projects")
                .then()
                .statusCode(403);
    }

    @Test
    void update_asProjectPm_succeeds() {
        given()
                .header("Authorization", "Bearer " + bugsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "project_name": "Demo Project",
                          "project_desc": "Seed data for Earned Value report",
                          "start_date": "2026-01-01",
                          "end_date": "2026-03-31",
                          "markup_rate": 10.00,
                          "project_manager_id": %d
                        }
                        """.formatted(IDS.pmProj1Id()))
                .when()
                .put("/projects/PROJ-1")
                .then()
                .statusCode(200);
    }

    @Test
    void update_asUnrelatedPm_returns403() {
        given()
                .header("Authorization", "Bearer " + marvinToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "project_name": "Hack Attempt",
                          "project_desc": "Should not apply",
                          "start_date": "2026-01-01",
                          "end_date": "2026-03-31",
                          "markup_rate": 10.00,
                          "project_manager_id": %d
                        }
                        """.formatted(IDS.marvinPmProj2Id()))
                .when()
                .put("/projects/PROJ-1")
                .then()
                .statusCode(403);
    }

    @Test
    void closeThenOpen_asPm_restoresOpenState() {
        given()
                .header("Authorization", "Bearer " + bugsToken)
                .when()
                .put("/projects/PROJ-1/close")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .when()
                .put("/projects/PROJ-1/open")
                .then()
                .statusCode(200);
    }

    @Test
    void getWorkPackages_returnsList() {
        given()
                .header("Authorization", "Bearer " + bugsToken)
                .when()
                .get("/projects/PROJ-1/workpackages")
                .then()
                .statusCode(200);
    }

    @Test
    void getAssignedEmployees_returnsList() {
        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/projects/PROJ-1/employees")
                .then()
                .statusCode(200);
    }

    @Test
    void getReport_returnsPlainText() {
        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/projects/PROJ-1/report")
                .then()
                .statusCode(200)
                .contentType(org.hamcrest.Matchers.containsString("text/plain"))
                .body(containsString("Project Report"));
    }

    /*
    @Test
    void closeProject_cascadesClosesToWps() {
        List<?> raw = given()
                .header("Authorization", "Bearer " + bugsToken)
                .when()
                .get("/projects/PROJ-1/workpackages")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");
        assertNotNull(raw);
        List<String> wpIds = new ArrayList<>();
        for (Object o : raw) {
            if (o instanceof Map<?, ?> m && m.get("wpId") instanceof String id) {
                wpIds.add(id);
            }
        }

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .when()
                .put("/projects/PROJ-1/close")
                .then()
                .statusCode(200);

        for (String wpId : wpIds) {
            given()
                    .header("Authorization", "Bearer " + bugsToken)
                    .when()
                    .get("/workpackages/" + wpId)
                    .then()
                    .statusCode(200)
                    .body("status", org.hamcrest.Matchers.equalTo("CLOSED_FOR_CHARGES"));
        }

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .when()
                .put("/projects/PROJ-1/open")
                .then()
                .statusCode(200);
    }

    @Test
    void reopenProject_doesNotReopenWps() {
        List<?> raw = given()
                .header("Authorization", "Bearer " + bugsToken)
                .when()
                .get("/projects/PROJ-1/workpackages")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");
        List<String> wpIds = new ArrayList<>();
        for (Object o : raw) {
            if (o instanceof Map<?, ?> m && m.get("wpId") instanceof String id) {
                wpIds.add(id);
            }
        }

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .when()
                .put("/projects/PROJ-1/close")
                .then()
                .statusCode(200);

        for (String wpId : wpIds) {
            given()
                    .header("Authorization", "Bearer " + bugsToken)
                    .when()
                    .get("/workpackages/" + wpId)
                    .then()
                    .statusCode(200)
                    .body("status", org.hamcrest.Matchers.equalTo("CLOSED_FOR_CHARGES"));
        }

        given()
                .header("Authorization", "Bearer " + bugsToken)
                .when()
                .put("/projects/PROJ-1/open")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/projects/PROJ-1")
                .then()
                .statusCode(200)
                .body("project_status", org.hamcrest.Matchers.equalTo("OPEN"));

        for (String wpId : wpIds) {
            given()
                    .header("Authorization", "Bearer " + bugsToken)
                    .when()
                    .get("/workpackages/" + wpId)
                    .then()
                    .statusCode(200)
                    .body("status", org.hamcrest.Matchers.equalTo("CLOSED_FOR_CHARGES"));
        }

        for (String wpId : wpIds) {
            given()
                    .header("Authorization", "Bearer " + bugsToken)
                    .when()
                    .put("/workpackages/" + wpId + "/open")
                    .then()
                    .statusCode(200);
        }
    }

    @Test
    void createProject_withBac_persistsBac() {
        String projId = "AUTO-BAC-" + System.nanoTime();
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(6);
        given()
                .header("Authorization", "Bearer " + opsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "project_id": "%s",
                          "project_name": "BAC create probe",
                          "project_desc": "iteration 2",
                          "project_type": "INTERNAL",
                          "start_date": "%s",
                          "end_date": "%s",
                          "markup_rate": 10.50,
                          "project_manager_id": %d,
                          "bac": 10000.00
                        }
                        """.formatted(projId, start, end, IDS.pmProj1Id()))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/projects/" + projId)
                .then()
                .statusCode(200)
                .body("bac", org.hamcrest.Matchers.equalTo(10000.00f));

        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .delete("/projects/" + projId)
                .then()
                .statusCode(200);
    }
    */
}
