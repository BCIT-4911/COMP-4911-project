package com.corejsf.Api;

import com.corejsf.TestConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

}