package com.corejsf.Api;

import com.corejsf.TestConfig;
import com.corejsf.TestConfig.StandardSeedIds;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for {@code GET /earned-value}.
 * <p>
 * The endpoint is currently unauthenticated beyond JWT; ReBAC tests are in a block comment until EV Security ships.
 */
@SuppressWarnings("unused")
class EarnedValueResourceTest extends TestConfig {

    private static StandardSeedIds IDS;
    private static String opsToken;
    private static String bugsToken;
    private static String tweetyToken;
    private static String hrToken;
    private static String marvinToken;

    @BeforeAll
    static void setup() {
        opsToken = loginAsSeedOps();
        IDS = resolveStandardSeedIds(opsToken);
        bugsToken = login(IDS.pmProj1Id(), DEFAULT_PASSWORD);
        tweetyToken = login(IDS.tweetyId(), DEFAULT_PASSWORD);
        hrToken = login(IDS.hrId(), DEFAULT_PASSWORD);
        marvinToken = login(IDS.marvinPmProj2Id(), DEFAULT_PASSWORD);
        Objects.requireNonNull(opsToken, "seed OPS login");
        Objects.requireNonNull(bugsToken, "seed PM PROJ-1 login");
        Objects.requireNonNull(tweetyToken, "seed Tweety login");
        Objects.requireNonNull(hrToken, "seed HR login");
        Objects.requireNonNull(marvinToken, "seed Marvin login");
    }

    @Test
    void getWeekly_withValidParentWpId_returns200() {
        List<?> list = given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/earned-value?parentWpId=A")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");
        assertFalse(list.isEmpty(), "Expected non-empty EV rows for parent A");
    }

    @Test
    void getWeekly_withAsOfDate_returns200() {
        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/earned-value?parentWpId=A&asOf=2026-03-01")
                .then()
                .statusCode(200);
    }

    @Test
    void getWeekly_missingParentWpId_returnsError() {
        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/earned-value")
                .then()
                .statusCode(anyOf(is(400), is(500)));
    }

    @Test
    void getWeekly_responseContainsBcwsAndBcwp() {
        @SuppressWarnings("unchecked")
        Map<String, Object> first = given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/earned-value?parentWpId=A")
                .then()
                .statusCode(200)
                .body("[0].bcwsByWeek", notNullValue())
                .body("[0].bcwpByWeek", notNullValue())
                .body("[0].totalBcws", notNullValue())
                .body("[0].totalBcwp", notNullValue())
                .extract()
                .jsonPath()
                .getMap("[0]");
        assertNotNull(first.get("bcwsByWeek"));
        assertNotNull(first.get("bcwpByWeek"));
        assertNotNull(first.get("totalBcws"));
        assertNotNull(first.get("totalBcwp"));
    }

    /*
    @Test
    void getWeekly_asAssignedPm_returns200() {
        given()
                .header("Authorization", "Bearer " + bugsToken)
                .when()
                .get("/earned-value?parentWpId=A")
                .then()
                .statusCode(200);
    }

    @Test
    void getWeekly_asOps_returns200() {
        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/earned-value?parentWpId=A")
                .then()
                .statusCode(200);
    }

    @Test
    void getWeekly_asUnrelatedPm_returns403() {
        given()
                .header("Authorization", "Bearer " + marvinToken)
                .when()
                .get("/earned-value?parentWpId=A")
                .then()
                .statusCode(403);
    }

    @Test
    void getWeekly_asRegularEmployee_returns403() {
        given()
                .header("Authorization", "Bearer " + tweetyToken)
                .when()
                .get("/earned-value?parentWpId=A")
                .then()
                .statusCode(403);
    }

    @Test
    void getWeekly_asHr_returns403() {
        given()
                .header("Authorization", "Bearer " + hrToken)
                .when()
                .get("/earned-value?parentWpId=A")
                .then()
                .statusCode(403);
    }
    */
}
