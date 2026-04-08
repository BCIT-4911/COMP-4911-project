package com.corejsf.Api;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.corejsf.TestConfig;

import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProjectAndWorkPackageRebacIntegrationTest extends TestConfig {

    private static int OPS_ID;
    private static int ELMER_ID;
    private static int HR_ID;
    private static int PM_PROJ1_ID;
    private static int RE_A_ID;
    private static int MEMBER_A2_ID;
    private static int RE_A2_ID;
    private static int PM_PROJ2_ID;

    private static String opsToken;
    private static String elmerToken;
    private static String hrToken;
    private static String pmProj1Token;
    private static String reAToken;
    private static String memberA2Token;
    private static String reA2Token;
    private static String pmProj2Token;

    @BeforeAll
    static void setup() {
        opsToken = loginAsSeedOps();
        StandardSeedIds ids = resolveStandardSeedIds(opsToken);
        OPS_ID = ids.opsId();
        ELMER_ID = ids.elmerId();
        HR_ID = ids.hrId();
        PM_PROJ1_ID = ids.pmProj1Id();
        RE_A_ID = ids.daffyId();
        MEMBER_A2_ID = ids.tweetyId();
        RE_A2_ID = ids.sylvesterId();
        PM_PROJ2_ID = ids.marvinPmProj2Id();

        elmerToken = login(ELMER_ID, DEFAULT_PASSWORD);
        hrToken = login(HR_ID, DEFAULT_PASSWORD);
        pmProj1Token = login(PM_PROJ1_ID, DEFAULT_PASSWORD);
        reAToken = login(RE_A_ID, DEFAULT_PASSWORD);
        memberA2Token = login(MEMBER_A2_ID, DEFAULT_PASSWORD);
        reA2Token = login(RE_A2_ID, DEFAULT_PASSWORD);
        pmProj2Token = login(PM_PROJ2_ID, DEFAULT_PASSWORD);

        // Remove stale WP assignments from previous test runs so visibility tests are accurate
        given()
                .header("Authorization", "Bearer " + pmProj1Token)
                .when()
                .delete("/workpackages/A.WP-1/employees/" + MEMBER_A2_ID);
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
    // Only Ops/Admin and Supervisors (for their direct reports) may assign.
    // PM role alone does NOT grant assignment rights.

    @Test
    void assignEmployeeToProject_asOpsManager_succeeds() {
        Response response = postWithToken(
                opsToken,
                "/projects/PROJ-1/employees/" + MEMBER_A2_ID + "?role=MEMBER"
        );

        assertSuccess2xx(response);
    }

    @Test
    void assignEmployeeToProject_asSupervisor_succeeds() {
        // Bugs Bunny (PM PROJ-1) is also supervisor of Sylvester (RE_A2).
        // This succeeds via the supervisor path, NOT the PM path.
        Response response = postWithToken(
                pmProj1Token,
                "/projects/PROJ-1/employees/" + RE_A2_ID + "?role=MEMBER"
        );

        assertSuccess2xx(response);
    }

    @Test
    void assignEmployeeToProject_asNonSupervisorPm_returns403() {
        // Marvin (PM PROJ-2) is NOT supervisor of Tweety (supervisor is Bugs).
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
                "/workpackages/A.WP-1/employees/" + MEMBER_A2_ID + "?role=MEMBER"
        );

        assertSuccess2xx(response);
    }

    @Test
    void assignEmployeeToWorkPackage_asOpsManager_returns403() {
        postWithToken(
                elmerToken,
                "/workpackages/A.WP-1/employees/" + MEMBER_A2_ID + "?role=MEMBER"
        )
        .then()
        .statusCode(403);
    }

    @Test
    void assignEmployeeToWorkPackage_asPmOfDifferentProject_returns403() {
        postWithToken(
                pmProj2Token,
                "/workpackages/A.WP-1/employees/" + MEMBER_A2_ID + "?role=MEMBER"
        )
        .then()
        .statusCode(403);
    }

    @Test
    void assignEmployeeToWorkPackage_asRegularEmployee_returns403() {
        postWithToken(
                memberA2Token,
                "/workpackages/A.WP-1/employees/" + RE_A_ID + "?role=MEMBER"
        )
        .then()
        .statusCode(403);
    }

    @Test
    void assignEmployeeToWorkPackage_asAssignedRe_returns403() {
        postWithToken(
                reA2Token,
                "/workpackages/A.WP-1/employees/" + MEMBER_A2_ID + "?role=MEMBER"
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
                "/workpackages/A.WP-2/close"
        );

        assertSuccess2xx(response);
    }

    @Test
    void openWorkPackage_asAssignedRe_succeeds() {
        Response response = putWithToken(
                reA2Token,
                "/workpackages/A.WP-2/open"
        );

        assertSuccess2xx(response);
    }

    @Test
    void closeWorkPackage_asProjectPm_succeeds() {
        Response response = putWithToken(
                pmProj1Token,
                "/workpackages/A.WP-2/close"
        );

        assertSuccess2xx(response);
    }

    @Test
    void openWorkPackage_asProjectPm_succeeds() {
        Response response = putWithToken(
                pmProj1Token,
                "/workpackages/A.WP-2/open"
        );

        assertSuccess2xx(response);
    }

    @Test
    void closeWorkPackage_asUnrelatedPm_returns403() {
        putWithToken(
                pmProj2Token,
                "/workpackages/A.WP-2/close"
        )
        .then()
        .statusCode(403);
    }

    @Test
    void openWorkPackage_asRegularEmployee_returns403() {
        putWithToken(
                memberA2Token,
                "/workpackages/A.WP-2/open"
        )
        .then()
        .statusCode(403);
    }

    @Test
    void closeWorkPackage_asHr_returns403() {
        putWithToken(
                hrToken,
                "/workpackages/A.WP-2/close"
        )
        .then()
        .statusCode(403);
    }

    // ----------------------------------------------------------------
    // WORK PACKAGE VISIBILITY (GET /projects/{id}/workpackages)
    //
    // Seed state for PROJ-1:
    //   A        - Elmer Fudd (RE)
    //   A.WP-1   - Daffy Duck (RE), Bugs Bunny (RE)
    //   A.WP-2   - Sylvester Cat (RE), Tweety Bird (MEMBER)
    //   A.WP-3   - Elmer Fudd (RE)
    //   C        - Elmer Fudd (RE)
    //   C.WP-1   - Bugs Bunny (RE)
    //   C.WP-2   - Elmer Fudd (RE)
    //
    // @Order(1-6) ensures these read-only tests run BEFORE mutation tests
    // (assignEmployeeToWorkPackage_asPmOfThatProject_succeeds adds Tweety
    //  to A.WP-1, which would otherwise corrupt the size==1 assertion).
    // ----------------------------------------------------------------

    @Test
    @Order(1)
    void getWorkPackages_asOpsManager_returnsAllSevenWps() {
        List<String> ids = getWorkPackageIds("PROJ-1", opsToken);

        assertTrue(ids.size() == 7,
                "Ops Manager should see all 7 WPs in PROJ-1 but got: " + ids);
        assertTrue(ids.containsAll(java.util.List.of("A", "A.WP-1", "A.WP-2", "A.WP-3", "C", "C.WP-1", "C.WP-2")),
                "Ops Manager is missing expected WP IDs. Got: " + ids);
    }

    @Test
    @Order(2)
    void getWorkPackages_asPmOfProject_returnsAllSevenWps() {
        List<String> ids = getWorkPackageIds("PROJ-1", pmProj1Token);

        assertTrue(ids.size() == 7,
                "PM of PROJ-1 should see all 7 WPs but got: " + ids);
        assertTrue(ids.containsAll(java.util.List.of("A", "A.WP-1", "A.WP-2", "A.WP-3", "C", "C.WP-1", "C.WP-2")),
                "PM of PROJ-1 is missing expected WP IDs. Got: " + ids);
    }

    @Test
    @Order(3)
    void getWorkPackages_asReOnWp1_returnsOnlyWp1() {
        // Daffy Duck is assigned as RE only to A.WP-1
        List<String> ids = getWorkPackageIds("PROJ-1", reAToken);

        assertTrue(ids.size() == 1,
                "Daffy Duck (RE on A.WP-1 only) should see exactly 1 WP but got: " + ids);
        assertTrue(ids.contains("A.WP-1"),
                "Daffy Duck should see A.WP-1 but got: " + ids);
    }

    @Test
    @Order(4)
    void getWorkPackages_asMemberOnWp2_returnsOnlyWp2() {
        // Tweety Bird is assigned as MEMBER only to A.WP-2
        List<String> ids = getWorkPackageIds("PROJ-1", memberA2Token);

        assertTrue(ids.size() == 1,
                "Tweety Bird (MEMBER on A.WP-2 only) should see exactly 1 WP but got: " + ids);
        assertTrue(ids.contains("A.WP-2"),
                "Tweety Bird should see A.WP-2 but got: " + ids);
    }

    @Test
    @Order(5)
    void getWorkPackages_asReOnWp2_returnsOnlyWp2() {
        // Sylvester Cat is assigned as RE only to A.WP-2
        List<String> ids = getWorkPackageIds("PROJ-1", reA2Token);

        assertTrue(ids.size() == 1,
                "Sylvester Cat (RE on A.WP-2 only) should see exactly 1 WP but got: " + ids);
        assertTrue(ids.contains("A.WP-2"),
                "Sylvester Cat should see A.WP-2 but got: " + ids);
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