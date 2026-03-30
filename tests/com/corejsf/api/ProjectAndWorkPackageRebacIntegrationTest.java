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
    private static int HR_ID;
    private static int PM_PROJ1_ID;
    private static int RE_A_ID;
    private static int MEMBER_A2_ID;
    private static int RE_A2_ID;
    private static int PM_PROJ2_ID;

    private static String opsToken;
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
        HR_ID = ids.hrId();
        PM_PROJ1_ID = ids.pmProj1Id();
        RE_A_ID = ids.daffyId();
        MEMBER_A2_ID = ids.tweetyId();
        RE_A2_ID = ids.sylvesterId();
        PM_PROJ2_ID = ids.marvinPmProj2Id();

        hrToken = login(HR_ID, DEFAULT_PASSWORD);
        pmProj1Token = login(PM_PROJ1_ID, DEFAULT_PASSWORD);
        reAToken = login(RE_A_ID, DEFAULT_PASSWORD);
        memberA2Token = login(MEMBER_A2_ID, DEFAULT_PASSWORD);
        reA2Token = login(RE_A2_ID, DEFAULT_PASSWORD);
        pmProj2Token = login(PM_PROJ2_ID, DEFAULT_PASSWORD);
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