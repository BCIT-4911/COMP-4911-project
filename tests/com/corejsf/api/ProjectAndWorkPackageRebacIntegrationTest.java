package com.corejsf.Api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProjectAndWorkPackageRebacIntegrationTest {

    private static Integer OPS_ID;
    private static Integer HR_ID;
    private static Integer PM_PROJ1_ID;
    private static Integer RE_A_ID;
    private static Integer MEMBER_A2_ID;
    private static Integer RE_A2_ID;
    private static Integer PM_PROJ2_ID;

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

        opsToken = login(1, PASSWORD);
        resolveSeedIds(opsToken);
        hrToken = login(HR_ID, PASSWORD);
        pmProj1Token = login(PM_PROJ1_ID, PASSWORD);
        reAToken = login(RE_A_ID, PASSWORD);
        memberA2Token = login(MEMBER_A2_ID, PASSWORD);
        reA2Token = login(RE_A2_ID, PASSWORD);
        pmProj2Token = login(PM_PROJ2_ID, PASSWORD);
    }

    private static void resolveSeedIds(String opsToken) {
        List<Map<String, Object>> employees = given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/employees")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");

        for (Map<String, Object> e : employees) {
            String first = (String) e.get("empFirstName");
            String last = (String) e.get("empLastName");
            int id = ((Number) e.get("empId")).intValue();
            if ("Wile".equals(first) && "Coyote".equals(last)) OPS_ID = id;
            else if ("Road".equals(first) && "Runner".equals(last)) HR_ID = id;
            else if ("Bugs".equals(first) && "Bunny".equals(last)) PM_PROJ1_ID = id;
            else if ("Daffy".equals(first) && "Duck".equals(last)) RE_A_ID = id;
            else if ("Tweety".equals(first) && "Bird".equals(last)) MEMBER_A2_ID = id;
            else if ("Sylvester".equals(first) && "Cat".equals(last)) RE_A2_ID = id;
            else if ("Marvin".equals(first) && "Martian".equals(last)) PM_PROJ2_ID = id;
        }
        assertNotNull(OPS_ID, "Seed data missing: Wile Coyote. Run: cd sql && docker compose down -v && docker compose up -d");
        assertNotNull(HR_ID, "Seed data missing: Road Runner");
        assertNotNull(PM_PROJ1_ID, "Seed data missing: Bugs Bunny");
        assertNotNull(RE_A_ID, "Seed data missing: Daffy Duck");
        assertNotNull(MEMBER_A2_ID, "Seed data missing: Tweety Bird");
        assertNotNull(RE_A2_ID, "Seed data missing: Sylvester Cat");
        assertNotNull(PM_PROJ2_ID, "Seed data missing: Marvin Martian");
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

    private List<String> getWorkPackageIds(String projId, String token) {
        List<Map<String, Object>> wps = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/projects/" + projId + "/workpackages")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");
        List<String> ids = new java.util.ArrayList<>();
        for (Map<String, Object> wp : wps) {
            ids.add((String) wp.get("wpId"));
        }
        return ids;
    }

    // PROJECT ASSIGNMENT RBAC
    // Endpoint:
    // POST /projects/{id}/employees/{empId}?role=PM

    @Test
    void assignEmployeeToProject_asOpsManager_succeeds() {
        Response response = postWithToken(
                opsToken,
                "/projects/PROJ-1/employees/" + MEMBER_A2_ID + "?role=MEMBER"
        );

        assertSuccess2xx(response);
    }

    @Test
    void assignEmployeeToProject_asPmOfThatProject_succeeds() {
        Response response = postWithToken(
                pmProj1Token,
                "/projects/PROJ-1/employees/" + RE_A2_ID + "?role=MEMBER"
        );

        assertSuccess2xx(response);
    }

    @Test
    void assignEmployeeToProject_asPmOfDifferentProject_returns403() {
        postWithToken(
                pmProj2Token,
                "/projects/PROJ-1/employees/" + MEMBER_A2_ID + "?role=MEMBER"
        )
        .then()
        .statusCode(403);
    }

    @Test
    void assignEmployeeToProject_asRegularEmployee_returns403() {
        postWithToken(
                memberA2Token,
                "/projects/PROJ-1/employees/" + RE_A_ID + "?role=MEMBER"
        )
        .then()
        .statusCode(403);
    }

    @Test
    void assignEmployeeToProject_asHr_returns403() {
        postWithToken(
                hrToken,
                "/projects/PROJ-1/employees/" + MEMBER_A2_ID + "?role=MEMBER"
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
                "/workpackages/CA-1.WP-1/employees/" + MEMBER_A2_ID + "?role=MEMBER"
        );

        assertSuccess2xx(response);
    }

    @Test
    void assignEmployeeToWorkPackage_asOpsManager_returns403() {
        postWithToken(
                opsToken,
                "/workpackages/CA-1.WP-1/employees/" + MEMBER_A2_ID + "?role=MEMBER"
        )
        .then()
        .statusCode(403);
    }

    @Test
    void assignEmployeeToWorkPackage_asPmOfDifferentProject_returns403() {
        postWithToken(
                pmProj2Token,
                "/workpackages/CA-1.WP-1/employees/" + MEMBER_A2_ID + "?role=MEMBER"
        )
        .then()
        .statusCode(403);
    }

    @Test
    void assignEmployeeToWorkPackage_asRegularEmployee_returns403() {
        postWithToken(
                memberA2Token,
                "/workpackages/CA-1.WP-1/employees/" + RE_A_ID + "?role=MEMBER"
        )
        .then()
        .statusCode(403);
    }

    @Test
    void assignEmployeeToWorkPackage_asAssignedRe_returns403() {
        postWithToken(
                reA2Token,
                "/workpackages/CA-1.WP-1/employees/" + MEMBER_A2_ID + "?role=MEMBER"
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

    // ----------------------------------------------------------------
    // WORK PACKAGE VISIBILITY (GET /projects/{id}/workpackages)
    //
    // Seed state for PROJ-1:
    //   A           - no WP assignments
    //   CA-1.WP-1   - Daffy Duck (RE)
    //   CA-1.WP-2   - Sylvester Cat (RE), Tweety Bird (MEMBER)
    //   CA-1.WP-3   - no WP assignments
    //
    // @Order(1-6) ensures these read-only tests run BEFORE mutation tests
    // (assignEmployeeToWorkPackage_asPmOfThatProject_succeeds adds Tweety
    //  to CA-1.WP-1, which would otherwise corrupt the size==1 assertion).
    // ----------------------------------------------------------------

    @Test
    @Order(1)
    void getWorkPackages_asOpsManager_returnsAllFourWps() {
        List<String> ids = getWorkPackageIds("PROJ-1", opsToken);

        assertTrue(ids.size() == 4,
                "Ops Manager should see all 4 WPs in PROJ-1 but got: " + ids);
        assertTrue(ids.containsAll(java.util.List.of("A", "CA-1.WP-1", "CA-1.WP-2", "CA-1.WP-3")),
                "Ops Manager is missing expected WP IDs. Got: " + ids);
    }

    @Test
    @Order(2)
    void getWorkPackages_asPmOfProject_returnsAllFourWps() {
        List<String> ids = getWorkPackageIds("PROJ-1", pmProj1Token);

        assertTrue(ids.size() == 4,
                "PM of PROJ-1 should see all 4 WPs but got: " + ids);
        assertTrue(ids.containsAll(java.util.List.of("A", "CA-1.WP-1", "CA-1.WP-2", "CA-1.WP-3")),
                "PM of PROJ-1 is missing expected WP IDs. Got: " + ids);
    }

    @Test
    @Order(3)
    void getWorkPackages_asReOnWp1_returnsOnlyWp1() {
        // Daffy Duck is assigned as RE only to CA-1.WP-1
        List<String> ids = getWorkPackageIds("PROJ-1", reAToken);

        assertTrue(ids.size() == 1,
                "Daffy Duck (RE on CA-1.WP-1 only) should see exactly 1 WP but got: " + ids);
        assertTrue(ids.contains("CA-1.WP-1"),
                "Daffy Duck should see CA-1.WP-1 but got: " + ids);
    }

    @Test
    @Order(4)
    void getWorkPackages_asMemberOnWp2_returnsOnlyWp2() {
        // Tweety Bird is assigned as MEMBER only to CA-1.WP-2
        List<String> ids = getWorkPackageIds("PROJ-1", memberA2Token);

        assertTrue(ids.size() == 1,
                "Tweety Bird (MEMBER on CA-1.WP-2 only) should see exactly 1 WP but got: " + ids);
        assertTrue(ids.contains("CA-1.WP-2"),
                "Tweety Bird should see CA-1.WP-2 but got: " + ids);
    }

    @Test
    @Order(5)
    void getWorkPackages_asReOnWp2_returnsOnlyWp2() {
        // Sylvester Cat is assigned as RE only to CA-1.WP-2
        List<String> ids = getWorkPackageIds("PROJ-1", reA2Token);

        assertTrue(ids.size() == 1,
                "Sylvester Cat (RE on CA-1.WP-2 only) should see exactly 1 WP but got: " + ids);
        assertTrue(ids.contains("CA-1.WP-2"),
                "Sylvester Cat should see CA-1.WP-2 but got: " + ids);
    }

    @Test
    @Order(6)
    void getWorkPackages_asPmOfDifferentProject_returnsZeroWps() {
        // Marvin Martian is PM of PROJ-2 and has no WP assignments in PROJ-1
        List<String> ids = getWorkPackageIds("PROJ-1", pmProj2Token);

        assertTrue(ids.isEmpty(),
                "Marvin Martian (PM of PROJ-2 only) should see 0 WPs in PROJ-1 but got: " + ids);
    }

}