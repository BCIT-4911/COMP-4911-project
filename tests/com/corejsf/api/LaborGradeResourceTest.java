package com.corejsf.Api;

import com.corejsf.TestConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LaborGradeResourceTest extends TestConfig {

    @Test
    void getAll_returnsLaborGrades() {
        String token = loginAsSeedOps();
        List<?> grades = given()
                .header("Authorization", "Bearer " + token)
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
        String token = loginAsSeedOps();
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/labor-grades/1")
                .then()
                .statusCode(200)
                .body("laborGradeId", org.hamcrest.Matchers.equalTo(1));
    }
}
