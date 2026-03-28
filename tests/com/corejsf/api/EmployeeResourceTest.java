package com.corejsf.Api;

import com.corejsf.TestConfig;
import com.corejsf.TestConfig.StandardSeedIds;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Employee API tests. Note: {@code canManageEmployees} in ReBAC allows HR, OPS, and EMPLOYEE roles
 * (directory access for PM assignments), so list/get behavior reflects that policy.
 */
class EmployeeResourceTest extends TestConfig {

    private static StandardSeedIds IDS;
    private static String opsToken;
    private static String tweetyToken;

    @BeforeAll
    static void setup() {
        opsToken = loginAsSeedOps();
        IDS = resolveStandardSeedIds(opsToken);
        tweetyToken = login(IDS.tweetyId(), DEFAULT_PASSWORD);
    }

    @Test
    void getAll_asOperationsManager_returns200() {
        List<?> list = given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/employees")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");
        assertFalse(list.isEmpty());
    }

    @Test
    void getAll_asRegularEmployee_returns200_directoryPolicy() {
        given()
                .header("Authorization", "Bearer " + tweetyToken)
                .when()
                .get("/employees")
                .then()
                .statusCode(200);
    }

    @Test
    void getById_ownRecord_returns200() {
        given()
                .header("Authorization", "Bearer " + tweetyToken)
                .when()
                .get("/employees/" + IDS.tweetyId())
                .then()
                .statusCode(200)
                .body("empId", org.hamcrest.Matchers.equalTo(IDS.tweetyId()));
    }

    @Test
    void create_asOperationsManager_returns201() {
        String unique = "IT-" + System.nanoTime();
        Map<?, ?> created = given()
                .header("Authorization", "Bearer " + opsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "firstName": "Integration",
                          "lastName": "%s",
                          "password": "password",
                          "laborGradeId": 1,
                          "supervisorId": %d,
                          "systemRole": "EMPLOYEE"
                        }
                        """.formatted(unique, IDS.opsId()))
                .when()
                .post("/employees")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getMap("$");
        assertTrue(created.containsKey("empId"));
    }
}
