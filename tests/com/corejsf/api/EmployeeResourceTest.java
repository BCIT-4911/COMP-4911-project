package com.corejsf.Api;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.corejsf.TestConfig;
import com.corejsf.TestConfig.StandardSeedIds;

import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;

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

    // ---- ADMIN can also write employees (HR + ADMIN) ----

    @Test
    void createEmployee_asAdmin_returns201() {
        String unique = "IT-ADM-" + System.nanoTime();
        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(createEmployeeBody("AdminCreated", unique, IDS.opsId()))
                .when()
                .post("/employees")
                .then()
                .statusCode(201);
    }

    @Test
    void update_asAdmin_returns200() {
        String unique = "ADM-UP-" + System.nanoTime();
        int empId = given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body(createEmployeeBody("AdminUpBefore", unique, IDS.opsId()))
                .when()
                .post("/employees")
                .then()
                .statusCode(201)
                .extract()
                .path("empId");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(updateEmployeeBody("AdminUpAfter", unique, IDS.opsId()))
                .when()
                .put("/employees/" + empId)
                .then()
                .statusCode(200)
                .body("empFirstName", equalTo("AdminUpAfter"));
    }

    @Test
    void delete_asAdmin_returns200() {
        String unique = "ADM-DEL-" + System.nanoTime();
        int newId = given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body(createEmployeeBody("AdminDel", unique, IDS.opsId()))
                .when()
                .post("/employees")
                .then()
                .statusCode(201)
                .extract()
                .path("empId");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .delete("/employees/" + newId)
                .then()
                .statusCode(200);
    }

    // ---- Create validation (EmployeeService throws BadRequestException → 400) ----

    @Test
    void create_missingFirstName_returns400() {
        given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "lastName": "Test",
                          "password": "password",
                          "laborGradeId": 1,
                          "supervisorId": %d,
                          "systemRole": "EMPLOYEE"
                        }
                        """.formatted(IDS.opsId()))
                .when()
                .post("/employees")
                .then()
                .statusCode(400);
    }

    @Test
    void create_blankLastName_returns400() {
        given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "firstName": "Valid",
                          "lastName": "   ",
                          "password": "password",
                          "laborGradeId": 1,
                          "supervisorId": %d,
                          "systemRole": "EMPLOYEE"
                        }
                        """.formatted(IDS.opsId()))
                .when()
                .post("/employees")
                .then()
                .statusCode(400);
    }

    @Test
    void create_missingPassword_returns400() {
        given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "firstName": "Valid",
                          "lastName": "Name",
                          "laborGradeId": 1,
                          "supervisorId": %d,
                          "systemRole": "EMPLOYEE"
                        }
                        """.formatted(IDS.opsId()))
                .when()
                .post("/employees")
                .then()
                .statusCode(400);
    }

    @Test
    void create_invalidSupervisorId_returns400() {
        given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "firstName": "Valid",
                          "lastName": "Name",
                          "password": "password",
                          "laborGradeId": 1,
                          "supervisorId": -1,
                          "systemRole": "EMPLOYEE"
                        }
                        """)
                .when()
                .post("/employees")
                .then()
                .statusCode(400);
    }

    @Test
    void create_nonExistentSupervisor_returns400() {
        given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "firstName": "Valid",
                          "lastName": "Name",
                          "password": "password",
                          "laborGradeId": 1,
                          "supervisorId": 999999,
                          "systemRole": "EMPLOYEE"
                        }
                        """)
                .when()
                .post("/employees")
                .then()
                .statusCode(400);
    }

    @Test
    void create_invalidLaborGradeId_returns400() {
        given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "firstName": "Valid",
                          "lastName": "Name",
                          "password": "password",
                          "laborGradeId": -1,
                          "supervisorId": %d,
                          "systemRole": "EMPLOYEE"
                        }
                        """.formatted(IDS.opsId()))
                .when()
                .post("/employees")
                .then()
                .statusCode(400);
    }

    @Test
    void create_missingSysRole_returns400() {
        given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "firstName": "Valid",
                          "lastName": "Name",
                          "password": "password",
                          "laborGradeId": 1,
                          "supervisorId": %d
                        }
                        """.formatted(IDS.opsId()))
                .when()
                .post("/employees")
                .then()
                .statusCode(400);
    }

    // ---- Partial update via EmployeeManagerUpdateDto ----

    @Test
    void update_partialOnlyFirstName_returns200_otherFieldsPreserved() {
        String origLast = "PartialOrig-" + System.nanoTime();
        int empId = given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body(createEmployeeBody("OrigFirst", origLast, IDS.opsId()))
                .when()
                .post("/employees")
                .then()
                .statusCode(201)
                .extract()
                .path("empId");

        given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body("""
                        { "firstName": "NewFirst" }
                        """)
                .when()
                .put("/employees/" + empId)
                .then()
                .statusCode(200)
                .body("empFirstName", equalTo("NewFirst"))
                .body("empLastName", equalTo(origLast));
    }

    @Test
    void update_blankFirstName_returns400() {
        String unique = "BlankFN-" + System.nanoTime();
        int empId = given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body(createEmployeeBody("BlankTest", unique, IDS.opsId()))
                .when()
                .post("/employees")
                .then()
                .statusCode(201)
                .extract()
                .path("empId");

        given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body("""
                        { "firstName": "   " }
                        """)
                .when()
                .put("/employees/" + empId)
                .then()
                .statusCode(400);
    }

    @Test
    void update_nonExistentEmployee_returns404() {
        given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body("""
                        { "firstName": "Ghost" }
                        """)
                .when()
                .put("/employees/999999")
                .then()
                .statusCode(404);
    }

    // ---- Self-update password (POST /employees/employee-self-update-password) ----

    @Test
    void selfUpdatePassword_returns204() {
        String unique = "SelfPw-" + System.nanoTime();
        int empId = given()
                .header("Authorization", "Bearer " + hrToken)
                .contentType(ContentType.JSON)
                .body(createEmployeeBody("SelfPw", unique, IDS.opsId()))
                .when()
                .post("/employees")
                .then()
                .statusCode(201)
                .extract()
                .path("empId");

        String selfToken = login(empId, DEFAULT_PASSWORD);

        given()
                .header("Authorization", "Bearer " + selfToken)
                .contentType(ContentType.JSON)
                .body("""
                        { "empPassword": "newSecurePass123" }
                        """)
                .when()
                .post("/employees/employee-self-update-password")
                .then()
                .statusCode(204);

        login(empId, "newSecurePass123");
    }

    @Test
    void selfUpdatePassword_blankPassword_returns400() {
        given()
                .header("Authorization", "Bearer " + tweetyToken)
                .contentType(ContentType.JSON)
                .body("""
                        { "empPassword": "   " }
                        """)
                .when()
                .post("/employees/employee-self-update-password")
                .then()
                .statusCode(400);
    }

    // ---- labor_grade_id serialization ----

    @Test
    void getAll_laborGradeIdFieldSerializesCorrectly() {
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
        boolean found = false;
        for (Map<String, Object> e : employees) {
            if (e.containsKey("labor_grade_id") && e.get("labor_grade_id") != null) {
                int lgId = ((Number) e.get("labor_grade_id")).intValue();
                assertTrue(lgId > 0, "labor_grade_id should be a positive integer");
                found = true;
                break;
            }
        }
        assertTrue(found, "At least one employee should have a non-null labor_grade_id in the JSON response");
    }
}
