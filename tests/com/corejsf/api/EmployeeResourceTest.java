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
 * Employee API tests. Read access uses {@code canManageEmployees}; create/update/delete use
 * {@code canWriteEmployees} (ADMIN and HR only). OPS_MANAGER and EMPLOYEE can list/read directory.
 */
class EmployeeResourceTest extends TestConfig {

    private static StandardSeedIds IDS;
    /** Seed user Wile Coyote (ADMIN); used for read-all and HR-forbidden write checks where needed. */
    private static String adminToken;
    private static String hrToken;
    private static String tweetyToken;
    /** Elmer Fudd (OPERATIONS_MANAGER) — not allowed to write employees. */
    private static String elmerToken;

    @BeforeAll
    static void setup() {
        adminToken = loginAsSeedOps();
        IDS = resolveStandardSeedIds(adminToken);
        hrToken = login(IDS.hrId(), DEFAULT_PASSWORD);
        tweetyToken = login(IDS.tweetyId(), DEFAULT_PASSWORD);
        elmerToken = login(IDS.elmerId(), DEFAULT_PASSWORD);
        Objects.requireNonNull(hrToken, "seed HR login");
        Objects.requireNonNull(elmerToken, "seed Operations Manager login");
    }

    @Test
    void getAll_asOperationsManager_stillReturns200() {
        List<?> list = given()
                .header("Authorization", "Bearer " + elmerToken)
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

    @Test
    void getById_nonExistent_returns404() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/employees/999999")
                .then()
                .statusCode(404);
    }

    @Test
    void getAll_responseBodyContainsNoProxyFields() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> employees = given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/employees")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");
        for (Map<String, Object> e : employees) {
            assertFalse(e.containsKey("hibernateLazyInitializer"),
                    "List entry should not expose Hibernate proxy field");
            assertFalse(e.containsKey("handler"), "List entry should not expose proxy handler");
        }
    }

    @Test
    void getAll_eachEmployeeHasExpectedFields() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> employees = given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/employees")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");
        for (Map<String, Object> e : employees) {
            assertNotNull(e.get("empId"), "empId required for employee " + e);
            assertNotNull(e.get("empFirstName"), "empFirstName required");
            assertNotNull(e.get("empLastName"), "empLastName required");
            assertNotNull(e.get("systemRole"), "systemRole required");
            int empId = ((Number) e.get("empId")).intValue();
            assertTrue(empId > 0, "empId should be positive");
        }
    }

    @Test
    void getById_supervisorEmployee_responseContainsNoProxyFields() {
        @SuppressWarnings("unchecked")
        Map<String, Object> body = given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/employees/" + IDS.pmProj1Id())
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getMap("$");
        assertFalse(body.containsKey("hibernateLazyInitializer"));
        assertFalse(body.containsKey("handler"));
        assertNotNull(body.get("empId"));
        assertNotNull(body.get("empFirstName"));
        assertNotNull(body.get("empLastName"));
    }

    @Test
    void getAll_supervisorIdFieldSerializesCorrectly() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> employees = given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/employees")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");
        Integer daffySupervisor = null;
        for (Map<String, Object> e : employees) {
            if ("Daffy".equals(e.get("empFirstName")) && "Duck".equals(e.get("empLastName"))) {
                assertTrue(e.containsKey("supervisor_id"), "Daffy should have supervisor_id in JSON");
                daffySupervisor = ((Number) e.get("supervisor_id")).intValue();
                break;
            }
        }
        int supId = Objects.requireNonNull(daffySupervisor, "Seed data should include Daffy Duck");
        assertTrue(supId > 0, "supervisor_id should be a positive employee id");
        boolean supervisorInList = employees.stream()
                .anyMatch(row -> supId == ((Number) row.get("empId")).intValue());
        assertTrue(supervisorInList,
                "supervisor_id should reference an employee present in the list (stable across DB id ordering)");
    }

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
                .header("Authorization", "Bearer " + elmerToken)
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
                .header("Authorization", "Bearer " + elmerToken)
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
    void delete_thenGet_returns404() {
        String unique = "DG-" + System.nanoTime();
        int newId = given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body(createEmployeeBody("DelThenGet", unique, IDS.opsId()))
                .when()
                .post("/employees")
                .then()
                .statusCode(201)
                .extract()
                .path("empId");

        given()
                .header("Authorization", "Bearer " + hrToken)
                .when()
                .delete("/employees/" + newId)
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/employees/" + newId)
                .then()
                .statusCode(404);
    }

    /*
     * TODO: PUT /employees/{id}/password is not implemented on EmployeeResource yet.
     *
    @Test
    void passwordReset_asHr_returns200() { ... }

    @Test
    void passwordReset_asUnauthorized_returns403() { ... }

    @Test
    void loginAfterReset_succeeds() { ... }
    */
}
