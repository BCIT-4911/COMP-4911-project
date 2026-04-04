package com.corejsf.Api;

import com.corejsf.TestConfig;
import com.corejsf.TestConfig.StandardSeedIds;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Labor grade API tests.
 * <p>
 * Uncomment the block comment below when the feature team ships labor grade POST/PUT and role checks.
 */
class LaborGradeResourceTest extends TestConfig {

    private static StandardSeedIds IDS;
    private static String opsToken;
    private static String tweetyToken;

    @BeforeAll
    static void setup() {
        opsToken = loginAsSeedOps();
        IDS = resolveStandardSeedIds(opsToken);
        tweetyToken = login(IDS.tweetyId(), DEFAULT_PASSWORD);
        Objects.requireNonNull(tweetyToken, "seed Tweety login");
    }

    @Test
    void getAll_returnsLaborGrades() {
        List<?> grades = given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/labor-grades")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");
        assertFalse(grades.isEmpty());
    }

    @Test
    void getById_returnsSingleGrade() {
        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/labor-grades/1")
                .then()
                .statusCode(200)
                .body("laborGradeId", equalTo(1));
    }

    private static String uniqueGradeCode() {
        long n = Math.abs(System.nanoTime());
        char a = (char) ('A' + (n % 26));
        char b = (char) ('A' + ((n / 26) % 26));
        return "" + a + b;
    }

    // ---- CRUD RBAC ----

    @Test
    void create_asOperationsManager_returns201() {
        String code = uniqueGradeCode();
        given()
                .header("Authorization", "Bearer " + opsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "gradeCode": "%s",
                          "chargeRate": 125.50
                        }
                        """.formatted(code))
                .when()
                .post("/labor-grades")
                .then()
                .statusCode(201)
                .body("gradeCode", equalTo(code));
    }

    @Test
    void create_asRegularEmployee_returns403() {
        String code = uniqueGradeCode();
        given()
                .header("Authorization", "Bearer " + tweetyToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "gradeCode": "%s",
                          "chargeRate": 99.00
                        }
                        """.formatted(code))
                .when()
                .post("/labor-grades")
                .then()
                .statusCode(403);
    }

    @Test
    void update_asOperationsManager_returns200() {
        String code = uniqueGradeCode();
        Integer id = given()
                .header("Authorization", "Bearer " + opsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "gradeCode": "%s",
                          "chargeRate": 100.00
                        }
                        """.formatted(code))
                .when()
                .post("/labor-grades")
                .then()
                .statusCode(201)
                .extract()
                .path("laborGradeId");
        assertNotNull(id);

        given()
                .header("Authorization", "Bearer " + opsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "gradeCode": "%s",
                          "chargeRate": 175.25
                        }
                        """.formatted(code))
                .when()
                .put("/labor-grades/" + id)
                .then()
                .statusCode(200)
                .body("laborGradeId", equalTo(id))
                .body("chargeRate", equalTo(175.25f));
    }

    @Test
    void update_asRegularEmployee_returns403() {
        given()
                .header("Authorization", "Bearer " + tweetyToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "gradeCode": "ZZ",
                          "chargeRate": 1.00
                        }
                        """)
                .when()
                .put("/labor-grades/1")
                .then()
                .statusCode(403);
    }

    @Test
    void delete_asOperationsManager_returns200() {
        String code = uniqueGradeCode();
        Integer id = given()
                .header("Authorization", "Bearer " + opsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "gradeCode": "%s",
                          "chargeRate": 50.00
                        }
                        """.formatted(code))
                .when()
                .post("/labor-grades")
                .then()
                .statusCode(201)
                .extract()
                .path("laborGradeId");

        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .delete("/labor-grades/" + id)
                .then()
                .statusCode(200);
    }

    @Test
    void delete_asRegularEmployee_returns403() {
        given()
                .header("Authorization", "Bearer " + tweetyToken)
                .when()
                .delete("/labor-grades/1")
                .then()
                .statusCode(403);
    }

    // ---- Validation ----

    @Test
    void create_blankGradeCode_returns400() {
        given()
                .header("Authorization", "Bearer " + opsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "gradeCode": "   ",
                          "chargeRate": 50.00
                        }
                        """)
                .when()
                .post("/labor-grades")
                .then()
                .statusCode(400);
    }

    @Test
    void create_gradeCodeTooLong_returns400() {
        given()
                .header("Authorization", "Bearer " + opsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "gradeCode": "ABC",
                          "chargeRate": 50.00
                        }
                        """)
                .when()
                .post("/labor-grades")
                .then()
                .statusCode(400);
    }

    @Test
    void create_negativeChargeRate_returns400() {
        given()
                .header("Authorization", "Bearer " + opsToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "gradeCode": "ZZ",
                          "chargeRate": -10.00
                        }
                        """)
                .when()
                .post("/labor-grades")
                .then()
                .statusCode(400);
    }

    @Test
    void delete_seedGradeInUse_returns409() {
        given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .delete("/labor-grades/1")
                .then()
                .statusCode(409);
    }
}
