package com.corejsf.Api;

import com.corejsf.TestConfig;
import com.corejsf.TestConfig.StandardSeedIds;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Employee API tests. List/get reflect {@code canManageEmployees} (directory vs HR management).
 * <p>
 * Uncomment the single block comment below when the feature team ships employee PUT/DELETE,
 * POST create RBAC (HR-only), and password reset ({@code PUT /employees/{id}/password}).
 */
@SuppressWarnings("unused")
class EmployeeResourceTest extends TestConfig {

    private static StandardSeedIds IDS;
    private static String opsToken;
    private static String hrToken;
    private static String tweetyToken;

    @BeforeAll
    static void setup() {
        opsToken = loginAsSeedOps();
        IDS = resolveStandardSeedIds(opsToken);
        hrToken = login(IDS.hrId(), DEFAULT_PASSWORD);
        tweetyToken = login(IDS.tweetyId(), DEFAULT_PASSWORD);
        Objects.requireNonNull(hrToken, "seed HR login");
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
                .body("empId", equalTo(IDS.tweetyId()));
    }

    /** Current behavior: OPS can create. Replace with createEmployee_asOperationsManager_returns403 after RBAC tightening. */
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

    /*
    private static String createEmployeeBody(String firstName, String lastName, int supervisorId) {
        return """
                {
                  "firstName": "%s",
                  "lastName": "%s",
                  "password": "password",
                  "laborGradeId": 1,
                  "supervisorId": %d,
                  "systemRole": "EMPLOYEE"
                }
                """.formatted(firstName, lastName, supervisorId);
    }

    private static String updateEmployeeBody(String firstName, String lastName, int supervisorId) {
        return """
                {
                  "firstName": "%s",
                  "lastName": "%s",
                  "laborGradeId": 1,
                  "supervisorId": %d,
                  "systemRole": "EMPLOYEE"
                }
                """.formatted(firstName, lastName, supervisorId);
    }

    @Test
    void createEmployee_asHr_returns201() {
        String unique = "IT-" + System.nanoTime();
        Map<?, ?> created = given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body(createEmployeeBody("Integration", unique, IDS.opsId()))
                .when()
                .post("/employees")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getMap("$");
        assertTrue(created.containsKey("empId"));
    }

    @Test
    void createEmployee_asOperationsManager_returns403() {
        String unique = "IT-OPS-" + System.nanoTime();
        given()
                .header("Authorization", "Bearer " + opsToken)
                .contentType(ContentType.JSON)
                .body(createEmployeeBody("Integration", unique, IDS.opsId()))
                .when()
                .post("/employees")
                .then()
                .statusCode(403);
    }

    @Test
    void createEmployee_asRegularEmployee_returns403() {
        String unique = "IT-EMP-" + System.nanoTime();
        given()
                .header("Authorization", "Bearer " + tweetyToken)
                .contentType(ContentType.JSON)
                .body(createEmployeeBody("Integration", unique, IDS.opsId()))
                .when()
                .post("/employees")
                .then()
                .statusCode(403);
    }

    @Test
    void update_asHr_returns200() {
        String unique = "UP-" + System.nanoTime();
        int empId = given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body(createEmployeeBody("BeforePut", unique, IDS.opsId()))
                .when()
                .post("/employees")
                .then()
                .statusCode(201)
                .extract()
                .path("empId");

        given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body(updateEmployeeBody("AfterPut", unique, IDS.opsId()))
                .when()
                .put("/employees/" + empId)
                .then()
                .statusCode(200)
                .body("empId", equalTo(empId));
    }

    @Test
    void update_asOperationsManager_returns403() {
        given()
                .header("Authorization", "Bearer " + opsToken)
                .contentType(ContentType.JSON)
                .body(updateEmployeeBody("Tweety", "Bird", IDS.pmProj1Id()))
                .when()
                .put("/employees/" + IDS.tweetyId())
                .then()
                .statusCode(403);
    }

    @Test
    void update_asRegularEmployee_returns403() {
        given()
                .header("Authorization", "Bearer " + tweetyToken)
                .contentType(ContentType.JSON)
                .body(updateEmployeeBody("Tweety", "Bird", IDS.pmProj1Id()))
                .when()
                .put("/employees/" + IDS.daffyId())
                .then()
                .statusCode(403);
    }

    @Test
    void delete_asHr_returns200() {
        String unique = "DEL-" + System.nanoTime();
        int newId = given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body(createEmployeeBody("ToDelete", unique, IDS.opsId()))
                .when()
                .post("/employees")
                .then()
                .statusCode(201)
                .extract()
                .path("empId");
        assertNotNull(newId);

        given()
                .header("Authorization", "Bearer " + hrToken)
                .when()
                .delete("/employees/" + newId)
                .then()
                .statusCode(200);
    }

    @Test
    void delete_asRegularEmployee_returns403() {
        given()
                .header("Authorization", "Bearer " + tweetyToken)
                .when()
                .delete("/employees/" + IDS.daffyId())
                .then()
                .statusCode(403);
    }

    @Test
    void passwordReset_asHr_returns200() {
        String unique = "PR-" + System.nanoTime();
        int empId = given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body(createEmployeeBody("PwdReset", unique, IDS.opsId()))
                .when()
                .post("/employees")
                .then()
                .statusCode(201)
                .extract()
                .path("empId");

        given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body("{\"newPassword\": \"ResetPass1!\"}")
                .when()
                .put("/employees/" + empId + "/password")
                .then()
                .statusCode(200);
    }

    @Test
    void passwordReset_asUnauthorized_returns403() {
        given()
                .header("Authorization", "Bearer " + tweetyToken)
                .contentType(ContentType.JSON)
                .body("{\"newPassword\": \"ShouldNotApply1!\"}")
                .when()
                .put("/employees/" + IDS.daffyId() + "/password")
                .then()
                .statusCode(403);
    }

    @Test
    void loginAfterReset_succeeds() {
        String unique = "LR-" + System.nanoTime();
        String newPassword = "PostReset-" + (System.nanoTime() % 1_000_000);
        int empId = given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body(createEmployeeBody("LoginReset", unique, IDS.opsId()))
                .when()
                .post("/employees")
                .then()
                .statusCode(201)
                .extract()
                .path("empId");

        given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body("{\"newPassword\": \"" + newPassword + "\"}")
                .when()
                .put("/employees/" + empId + "/password")
                .then()
                .statusCode(200);

        login(empId, newPassword);
    }
    */
}
