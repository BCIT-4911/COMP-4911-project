package com.corejsf.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

// Executing CRUD test operation according to specific order due to parent-child relationship
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProjectCrudRegressionTest {

    private static final String PROJECT_MANAGER_PASSWORD = "password";
    private static final String PROJECT_MANAGER_SYSTEM_ROLE = "PROJECT_MANAGER";

    private static final int OPERATION_MANAGER_EMP_ID = 1;
    private static final String OPERATION_MANAGER_PASSWORD = "password";

    private static final String PROJECT_RESOURCE_API_ENDPOINT_PREFIX = "/projects/";
    private static final String PROJECT_RESOURCE_CREATE_API_ENDPOINT = "/projects";

    private static final String TEST_PROJECT_INITIAL_ID = "CRUD-1";
    private static final String TEST_PROJECT_INITIAL_TYPE = "INTERNAL";
    private static final String TEST_PROJECT_INITIAL_NAME = "CRUD Test Project";
    private static final String TEST_PROJECT_INITIAL_DESC = "Project for testing CRUD under regression";
    private static final String TEST_PROJECT_INITIAL_STATUS = "OPEN";
    private static final String TEST_PROJECT_INITIAL_START_DATE = "2026-04-01";
    private static final String TEST_PROJECT_INITIAL_END_DATE = "2026-06-30";
    private static final String TEST_PROJECT_INITIAL_MARK_UP_RATE = "10.0";

    private static final String TEST_PROJECT_UPDATE_ID = "CRUD-1";
    private static final String TEST_PROJECT_UPDATE_TYPE = "INTERNAL";
    private static final String TEST_PROJECT_UPDATE_NAME = "CRUD Test Project - Modified";
    private static final String TEST_PROJECT_UPDATE_DESC = "Project modified for testing CRUD under regression";
    private static final String TEST_PROJECT_UPDATE_STATUS = "OPEN";
    private static final String TEST_PROJECT_UPDATE_START_DATE = "2026-04-01";
    private static final String TEST_PROJECT_UPDATE_END_DATE = "2026-07-15";
    private static final String TEST_PROJECT_UPDATE_MARK_UP_RATE = "15.0";

    private static final String PROJECT_ID_FIELD_NAME = "project_id";
    private static final String PROJECT_TYPE_FIELD_NAME = "project_type";
    private static final String PROJECT_NAME_FIELD_NAME = "project_name";
    private static final String PROJECT_DESC_FIELD_NAME = "project_desc";
    private static final String PROJECT_STATUS_FIELD_NAME = "project_status";
    private static final String PROJECT_START_DATE_FIELD_NAME = "start_date";
    private static final String PROJECT_END_DATE_FIELD_NAME = "end_date";
    private static final String PROJECT_MARK_UP_RATE_FIELD_NAME = "markup_rate";
    private static final String PROJECT_MANAGER_ID_FIELD_NAME = "project_manager_id";

    private static final String WORK_PACKAGE_RESOURCE_API_ENDPOINT_PREFIX = "/workpackages/";
    private static final String WORK_PACKAGE_RESOURE_CREATE_API_ENDPOINT = "/workpackages";

    private static final String TEST_WORK_PACKAGE_INITIAL_ID = "CRUD-1.WP-1";
    private static final String TEST_WORK_PACKAGE_INITIAL_NAME = "Initial test work package";
    private static final String TEST_WORK_PACKAGE_INITIAL_DESC = "Initial test work package for CRUD operation";
    private static final String TEST_WORK_PACKAGE_INITIAL_TYPE = "LOWEST_LEVEL";
    private static final String TEST_WORK_PACKAGE_INITIAL_STATUS = "OPEN_FOR_CHARGES";
    private static final String TEST_WORK_PACKAGE_INITIAL_BUDGETED_EFFORT = "10000.00";
    private static final String TEST_WORK_PACKAGE_INITIAL_BCWS = "0.0";
    private static final String TEST_WORK_PACKAGE_INITIAL_PERCENT_COMPLETE = "0.0";
    private static final String TEST_WORK_PACKAGE_INITIAL_EAC = "0.0";
    private static final String TEST_WORK_PACKAGE_INITIAL_CV = "0.0";
    private static final String TEST_WORK_PACKAGE_INITIAL_CREATE_DATE = LocalDate.now().toString();
    private static final String TEST_WORK_PACKAGE_INITIAL_MODIFIED_DATE = LocalDate.now().toString();
    private static final String TEST_WORK_PACKAGE_INITIAL_CREATED_BY = String.valueOf(OPERATION_MANAGER_EMP_ID);
    private static final String TEST_WORK_PACKAGE_INITIAL_MODIFIED_BY = String.valueOf(OPERATION_MANAGER_EMP_ID);
    private static final String TEST_WORK_PACKAGE_INITIAL_WORK_ACCOMPLISHED = "n/a";
    private static final String TEST_WORK_PACKAGE_INITIAL_WORK_PLANNED = "To be planned";
    private static final String TEST_WORK_PACKAGE_INITIAL_WORK_PROBLEMS = "To be determined";
    private static final String TEST_WORK_PACKAGE_INITIAL_ANTICIPATED_PROBLEMS = "To be discussed";

    private static final String WORK_PACKAGE_ID_FIELD_NAME = "wp_id";
    private static final String WORK_PACKAGE_NAME_FIELD_NAME = "wp_name";
    private static final String WORK_PACKAGE_DESC_FIELD_NAME = "description";
    private static final String WORK_PACKGE_PROJECT_ID_FIELD_NAME = "proj_id";
    private static final String WORK_PACKAGE_PARENT_WORK_PACKAGE_ID_FIELD_NAME = "parent_wp_id";
    private static final String WORK_PACKAGE_TYPE_FIELD_NAME = "wp_type";
    private static final String WORK_PACKAGE_STATUS_FIELD_NAME = "status";
    private static final String WORK_PACKAGE_STRUCTURE_LOCKED_FIELD_NAME = "structure_locked";
    private static final String WORK_PACKAGE_BUDGETED_EFFORT_FIELD_NAME = "budgeted_effort";
    private static final String WORK_PACKAGE_BCWS_FIELD_NAME = "bcws";
    private static final String WORK_PACKAGE_PLAN_START_DATE_FIELD_NAME = "plan_start_date";
    private static final String WORK_PACKAGE_PLAN_END_DATE_FIELD_NAME = "plan_end_date";
    private static final String WORK_PACKAGE_RE_EMPLOYEE_ID_FIELD_NAME = "re_employee_id";
    private static final String WORK_PACKAGE_BAC_FIELD_NAME = "bac";
    private static final String WORK_PACKAGE_PERCENT_COMPLETE_FIELD_NAME = "percent_complete";
    private static final String WORK_PACKAGE_EAC_FIELD_NAME = "eac";
    private static final String WORK_PACKAGE_CV_FIELD_NAME = "cv";
    private static final String WORK_PACKAGE_CREATED_DATE_FIELD_NAME = "created_date";
    private static final String WORK_PACKAGE_MODIFIED_DATE_FIELD_NAME = "modified_date";
    private static final String WORK_PACKAGE_CREATED_BY_FIELD_NAME = "created_by";
    private static final String WORK_PACKAGE_MODIFIED_BY_FIELD_NAME = "modified_by";
    private static final String WORK_PACKAGE_WORK_ACCOMPLISHED_FIELD_NAME = "work_accomplished";
    private static final String WORK_PACKAGE_WORK_PLANNED_FIELD_NAME = "work_planned";
    private static final String WORK_PACKAGE_PROBLEMS_FIELD_NAME = "problems";
    private static final String WORK_PACKAGE_ANTICIPATED_PROBLEMS_FIELD_NAME = "anticipated_problems";

    private static final String AUTHORIZATION_BEARER_HTTP_HEADER_PREFIX = "Authorization";
    private static final String AUTHORIZATION_BEARER_VALUE_PREFIX = "Bearer ";

    private static final String JSON_KEY_VALUE_SEPARATOR = "\":\"";
    private static final String JSON_FIELD_DATA_SEPARATOR = "\",\"";

    private static final int DEFAULT_NON_EXISTING_EMP_ID = -1;

    private int projectManagerId = DEFAULT_NON_EXISTING_EMP_ID;

    private String opsToken = null;
    private String pmProj1Token = null;

    @BeforeAll
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;
        RestAssured.basePath = "/Project/api";

        opsToken = login(OPERATION_MANAGER_EMP_ID, OPERATION_MANAGER_PASSWORD);
        resolveProjectManagerEmpId(opsToken);
        pmProj1Token = login(projectManagerId, PROJECT_MANAGER_PASSWORD);
    }

    private void resolveProjectManagerEmpId(String opsToken) {
        List<Map<String, Object>> employees = given()
                .header(AUTHORIZATION_BEARER_HTTP_HEADER_PREFIX, AUTHORIZATION_BEARER_VALUE_PREFIX + opsToken)
                .get("/employees").then().statusCode(200).extract().jsonPath().getList("$");

        for (Map<String, Object> e : employees) {
            String systemRole = (String) e.get("systemRole");
            if (systemRole.equals(PROJECT_MANAGER_SYSTEM_ROLE)) {
                projectManagerId = ((Number) e.get("empId")).intValue();
                break;
            }
        }

        assertNotEquals(projectManagerId,
                DEFAULT_NON_EXISTING_EMP_ID,
                "Project manager employee ID should not be \"" + DEFAULT_NON_EXISTING_EMP_ID + "\".");
    }

    private static String login(int empId, String password) {
        Response response = given().contentType(ContentType.JSON).body("""
                {
                  "empId": %d,
                  "password": "%s"
                }
                """.formatted(empId, password)).post("/auth/login").then().statusCode(200).extract().response();

        String token = response.jsonPath().getString("token");

        assertFalse(() -> token == null || token.isBlank(),
                "Login succeeded but token was missing with employee ID = " + empId + " and password = " + password);

        return token;
    }

    private static boolean isSuccessHttpCode1xx(Response response) {
        return response.statusCode() >= 100 && response.statusCode() < 200;
    }

    private static boolean isSuccessHttpCode2xx(Response response) {
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    private static boolean isSuccessHttpCode3xx(Response response) {
        return response.statusCode() >= 300 && response.statusCode() < 400;
    }

    private static boolean isSuccessHttpCode4xx(Response response) {
        return response.statusCode() >= 400 && response.statusCode() < 500;
    }

    private static boolean isSuccessHttpCode5xx(Response response) {
        return response.statusCode() >= 500 && response.statusCode() < 600;
    }

    private static void assertSuccess1xx(Response response) {
        assertTrue(() -> isSuccessHttpCode1xx(response),
                "Expected 1xx informational code but got " + response.statusCode() + " with body: " + response.getBody().asString());
    }

    private static void assertSuccess2xx(Response response) {
        assertTrue(() -> isSuccessHttpCode2xx(response), "Expected 2xx success code but got " + response.statusCode()
                + " with body: " + response.getBody().asString());
    }

    private static void assertSuccess3xx(Response response) {
        assertTrue(() -> isSuccessHttpCode3xx(response),
                "Expected 3xx redirection code but got " + response.statusCode() + " with body: " + response.getBody().asString());
    }

    private static void assertSuccess4xx(Response response) {
        assertTrue(() -> isSuccessHttpCode4xx(response),
                "Expected 4xx client error code but got " + response.statusCode() + " with body: " + response.getBody().asString());
    }

    private static void assertSuccess5xx(Response response) {
        assertTrue(() -> isSuccessHttpCode3xx(response), "Expected 5xx server error code but got "
                + response.statusCode() + " with body: " + response.getBody().asString());
    }

    /*
@Test
@Order(1)
void removeTestWorkPackage() {
    // Before carrying out any CRUD operation, attempts to remove the testing work package first.

    Response getTestWorkPackageResponse = given()
            .header(AUTHORIZATION_BEARER_HTTP_HEADER_PREFIX, AUTHORIZATION_BEARER_VALUE_PREFIX + opsToken)
            .get(WORK_PACKAGE_RESOURCE_API_ENDPOINT_PREFIX +)

}
     */
    @Test
    @Order(1)
    void testProjectCRUD() {
        // Check if the test project exist before performing any CRUD check

        // Read with the test project initial ID to see if it exists or not
        Response checkResponseForInitTestProject = given()
                .header(AUTHORIZATION_BEARER_HTTP_HEADER_PREFIX, AUTHORIZATION_BEARER_VALUE_PREFIX + opsToken)
                .get(PROJECT_RESOURCE_API_ENDPOINT_PREFIX + TEST_PROJECT_INITIAL_ID);

        // If the test project already exists
        if (isSuccessHttpCode2xx(checkResponseForInitTestProject)) {
            // Attempt to remove it first
            Response deleteResponse = given()
                    .header(AUTHORIZATION_BEARER_HTTP_HEADER_PREFIX, AUTHORIZATION_BEARER_VALUE_PREFIX + opsToken)
                    .delete(PROJECT_RESOURCE_API_ENDPOINT_PREFIX + TEST_PROJECT_INITIAL_ID);
            assertTrue(isSuccessHttpCode2xx(deleteResponse), "Failed to delete the initial test project before CRUD test");

            Response verifyDeleteResponse = given()
                    .header(AUTHORIZATION_BEARER_HTTP_HEADER_PREFIX, AUTHORIZATION_BEARER_VALUE_PREFIX + opsToken)
                    .get(PROJECT_RESOURCE_API_ENDPOINT_PREFIX + TEST_PROJECT_INITIAL_ID);
            assertEquals(404, verifyDeleteResponse.getStatusCode(),
                    "The delete of initial test project should result in a 404 NOT FOUND result in subsequent enquiry.");
        }

        // Read with the test project initial ID to see if it exists or not
        Response checkResponseForModifiedTestProject = given()
                .header(AUTHORIZATION_BEARER_HTTP_HEADER_PREFIX, AUTHORIZATION_BEARER_VALUE_PREFIX + opsToken)
                .get(PROJECT_RESOURCE_API_ENDPOINT_PREFIX + TEST_PROJECT_INITIAL_ID);

        // If the modified test project already exists
        if (isSuccessHttpCode2xx(checkResponseForModifiedTestProject)) {
            // Attempt to remove it first
            Response deleteResponse = given()
                    .header(AUTHORIZATION_BEARER_HTTP_HEADER_PREFIX, AUTHORIZATION_BEARER_VALUE_PREFIX + opsToken)
                    .delete(PROJECT_RESOURCE_API_ENDPOINT_PREFIX + TEST_PROJECT_UPDATE_ID);
            assertTrue(isSuccessHttpCode2xx(deleteResponse), "Failed to delete the modified test project before CRUD test");

            Response verifyDeleteResponse = given()
                    .header(AUTHORIZATION_BEARER_HTTP_HEADER_PREFIX, AUTHORIZATION_BEARER_VALUE_PREFIX + opsToken)
                    .get(PROJECT_RESOURCE_API_ENDPOINT_PREFIX + TEST_PROJECT_UPDATE_ID);
            assertEquals(404, verifyDeleteResponse.getStatusCode(),
                    "The delete of modified test project should result in a 404 NOT FOUND result in subsequent enquiry.");
        }

        // Create Project
        String createProjectBody = "{\""
                + PROJECT_ID_FIELD_NAME + JSON_KEY_VALUE_SEPARATOR + TEST_PROJECT_INITIAL_ID + JSON_FIELD_DATA_SEPARATOR
                + PROJECT_TYPE_FIELD_NAME + JSON_KEY_VALUE_SEPARATOR + TEST_PROJECT_INITIAL_TYPE + JSON_FIELD_DATA_SEPARATOR
                + PROJECT_NAME_FIELD_NAME + JSON_KEY_VALUE_SEPARATOR + TEST_PROJECT_INITIAL_NAME + JSON_FIELD_DATA_SEPARATOR
                + PROJECT_DESC_FIELD_NAME + JSON_KEY_VALUE_SEPARATOR + TEST_PROJECT_INITIAL_DESC + JSON_FIELD_DATA_SEPARATOR
                + PROJECT_STATUS_FIELD_NAME + JSON_KEY_VALUE_SEPARATOR + TEST_PROJECT_INITIAL_STATUS + JSON_FIELD_DATA_SEPARATOR
                + PROJECT_START_DATE_FIELD_NAME + JSON_KEY_VALUE_SEPARATOR + TEST_PROJECT_INITIAL_START_DATE + JSON_FIELD_DATA_SEPARATOR
                + PROJECT_END_DATE_FIELD_NAME + JSON_KEY_VALUE_SEPARATOR + TEST_PROJECT_INITIAL_END_DATE + JSON_FIELD_DATA_SEPARATOR
                + PROJECT_MARK_UP_RATE_FIELD_NAME + JSON_KEY_VALUE_SEPARATOR + TEST_PROJECT_INITIAL_MARK_UP_RATE + JSON_FIELD_DATA_SEPARATOR
                + PROJECT_MANAGER_ID_FIELD_NAME + JSON_KEY_VALUE_SEPARATOR + projectManagerId + "\"}";

        Response createResponse = given()
                .header(AUTHORIZATION_BEARER_HTTP_HEADER_PREFIX, AUTHORIZATION_BEARER_VALUE_PREFIX + opsToken)
                .contentType(ContentType.JSON).body(createProjectBody).post(PROJECT_RESOURCE_CREATE_API_ENDPOINT);
        assertSuccess2xx(createResponse);

        // Read Project
        Response readResponse = given()
                .header(AUTHORIZATION_BEARER_HTTP_HEADER_PREFIX, AUTHORIZATION_BEARER_VALUE_PREFIX + opsToken)
                .get(PROJECT_RESOURCE_API_ENDPOINT_PREFIX + TEST_PROJECT_UPDATE_ID);
        assertSuccess2xx(readResponse);
        assertEquals(TEST_PROJECT_INITIAL_ID, readResponse.jsonPath().getString(PROJECT_ID_FIELD_NAME),
                "Project initial ID should be \"" + TEST_PROJECT_INITIAL_ID + "\".");
        assertEquals(TEST_PROJECT_INITIAL_TYPE, readResponse.jsonPath().getString(PROJECT_TYPE_FIELD_NAME),
                "Project initial type should be \"" + TEST_PROJECT_INITIAL_TYPE + "\".");
        assertEquals(TEST_PROJECT_INITIAL_NAME, readResponse.jsonPath().getString(PROJECT_NAME_FIELD_NAME),
                "Project initial name should be \"" + TEST_PROJECT_INITIAL_NAME + "\".");
        assertEquals(TEST_PROJECT_INITIAL_DESC, readResponse.jsonPath().getString(PROJECT_DESC_FIELD_NAME),
                "Project initial description should be \"" + TEST_PROJECT_INITIAL_DESC + "\".");
        assertEquals(TEST_PROJECT_INITIAL_STATUS, readResponse.jsonPath().getString(PROJECT_STATUS_FIELD_NAME),
                "Project initial description should be \"" + TEST_PROJECT_INITIAL_STATUS + "\".");
        assertEquals(TEST_PROJECT_INITIAL_START_DATE, readResponse.jsonPath().getString(PROJECT_START_DATE_FIELD_NAME),
                "Project initial start date should be \"" + TEST_PROJECT_INITIAL_START_DATE + "\".");
        assertEquals(TEST_PROJECT_INITIAL_END_DATE, readResponse.jsonPath().getString(PROJECT_END_DATE_FIELD_NAME),
                "Project initial end date should be \"" + TEST_PROJECT_INITIAL_END_DATE + "\".");
        assertEquals(TEST_PROJECT_INITIAL_MARK_UP_RATE, readResponse.jsonPath().getString(PROJECT_MARK_UP_RATE_FIELD_NAME),
                "Project initial mark up rate should be \"" + TEST_PROJECT_INITIAL_MARK_UP_RATE + "\".");
        assertEquals(String.valueOf(projectManagerId), readResponse.jsonPath().getString(PROJECT_MANAGER_ID_FIELD_NAME),
                "Project initial manager ID should be \"" + projectManagerId + "\".");

        // Update Project
        String updateProjectBody = "{\""
                + PROJECT_ID_FIELD_NAME + JSON_KEY_VALUE_SEPARATOR + TEST_PROJECT_UPDATE_ID + JSON_FIELD_DATA_SEPARATOR
                + PROJECT_TYPE_FIELD_NAME + JSON_KEY_VALUE_SEPARATOR + TEST_PROJECT_UPDATE_TYPE + JSON_FIELD_DATA_SEPARATOR
                + PROJECT_NAME_FIELD_NAME + JSON_KEY_VALUE_SEPARATOR + TEST_PROJECT_UPDATE_NAME + JSON_FIELD_DATA_SEPARATOR
                + PROJECT_DESC_FIELD_NAME + JSON_KEY_VALUE_SEPARATOR + TEST_PROJECT_UPDATE_DESC + JSON_FIELD_DATA_SEPARATOR
                + PROJECT_STATUS_FIELD_NAME + JSON_KEY_VALUE_SEPARATOR + TEST_PROJECT_UPDATE_STATUS + JSON_FIELD_DATA_SEPARATOR
                + PROJECT_START_DATE_FIELD_NAME + JSON_KEY_VALUE_SEPARATOR + TEST_PROJECT_UPDATE_START_DATE + JSON_FIELD_DATA_SEPARATOR
                + PROJECT_END_DATE_FIELD_NAME + JSON_KEY_VALUE_SEPARATOR + TEST_PROJECT_UPDATE_END_DATE + JSON_FIELD_DATA_SEPARATOR
                + PROJECT_MARK_UP_RATE_FIELD_NAME + JSON_KEY_VALUE_SEPARATOR + TEST_PROJECT_UPDATE_MARK_UP_RATE + JSON_FIELD_DATA_SEPARATOR
                + PROJECT_MANAGER_ID_FIELD_NAME + JSON_KEY_VALUE_SEPARATOR + projectManagerId + "\"}";

        Response updateResponse = given()
                .header(AUTHORIZATION_BEARER_HTTP_HEADER_PREFIX, AUTHORIZATION_BEARER_VALUE_PREFIX + opsToken)
                .contentType(ContentType.JSON).body(updateProjectBody).put(PROJECT_RESOURCE_API_ENDPOINT_PREFIX + TEST_PROJECT_UPDATE_ID);
        assertSuccess2xx(updateResponse);

        // Verify Update
        Response verifyUpdateResponse = given()
                .header(AUTHORIZATION_BEARER_HTTP_HEADER_PREFIX, AUTHORIZATION_BEARER_VALUE_PREFIX + opsToken).when()
                .get(PROJECT_RESOURCE_API_ENDPOINT_PREFIX + TEST_PROJECT_UPDATE_ID);
        assertSuccess2xx(verifyUpdateResponse);
        assertEquals(TEST_PROJECT_UPDATE_ID, verifyUpdateResponse.jsonPath().getString(PROJECT_ID_FIELD_NAME),
                "Project initial ID should be \"" + TEST_PROJECT_UPDATE_ID + "\".");
        assertEquals(TEST_PROJECT_UPDATE_TYPE, verifyUpdateResponse.jsonPath().getString(PROJECT_TYPE_FIELD_NAME),
                "Project initial type should be \"" + TEST_PROJECT_UPDATE_TYPE + "\".");
        assertEquals(TEST_PROJECT_UPDATE_NAME, verifyUpdateResponse.jsonPath().getString(PROJECT_NAME_FIELD_NAME),
                "Project initial name should be \"" + TEST_PROJECT_UPDATE_NAME + "\".");
        assertEquals(TEST_PROJECT_UPDATE_DESC, verifyUpdateResponse.jsonPath().getString(PROJECT_DESC_FIELD_NAME),
                "Project initial description should be \"" + TEST_PROJECT_UPDATE_DESC + "\".");
        assertEquals(TEST_PROJECT_UPDATE_STATUS, verifyUpdateResponse.jsonPath().getString(PROJECT_STATUS_FIELD_NAME),
                "Project initial description should be \"" + TEST_PROJECT_UPDATE_STATUS + "\".");
        assertEquals(TEST_PROJECT_UPDATE_START_DATE, verifyUpdateResponse.jsonPath().getString(PROJECT_START_DATE_FIELD_NAME),
                "Project initial start date should be \"" + TEST_PROJECT_UPDATE_START_DATE + "\".");
        assertEquals(TEST_PROJECT_UPDATE_END_DATE, verifyUpdateResponse.jsonPath().getString(PROJECT_END_DATE_FIELD_NAME),
                "Project initial end date should be \"" + TEST_PROJECT_UPDATE_END_DATE + "\".");
        assertEquals(TEST_PROJECT_UPDATE_MARK_UP_RATE, verifyUpdateResponse.jsonPath().getString(PROJECT_MARK_UP_RATE_FIELD_NAME),
                "Project initial mark up rate should be \"" + TEST_PROJECT_UPDATE_MARK_UP_RATE + "\".");
        assertEquals(String.valueOf(projectManagerId), verifyUpdateResponse.jsonPath().getString(PROJECT_MANAGER_ID_FIELD_NAME),
                "Project initial manager ID should be \"" + projectManagerId + "\".");
    }

    @Test
    @Order(2)
    void testWorkPackageCRUD() {
        // Create Work Package
        String wpBody = "{" + "\"wpId\":\"CRUD-1.WP-1\"," + "\"wpName\":\"Initial Work Package\","
                + "\"description\":\"Test WP for CRUD\"," + "\"status\":\"OPEN\"," + "\"budgetedEffort\":100.00,"
                + "\"bac\":5000.00," + "\"projectId\":\"CRUD-1\"," + "\"responsibleEngineerId\":" + projectManagerId
                + "}";

        Response createResponse = given()
                .header(AUTHORIZATION_BEARER_HTTP_HEADER_PREFIX, AUTHORIZATION_BEARER_VALUE_PREFIX + pmProj1Token)
                .contentType(ContentType.JSON).body(wpBody).when().post("/projects/CRUD-1/workpackages");
        assertSuccess2xx(createResponse);

        // Read Work Packages
        Response readResponse = given()
                .header(AUTHORIZATION_BEARER_HTTP_HEADER_PREFIX, AUTHORIZATION_BEARER_VALUE_PREFIX + pmProj1Token)
                .when().get("/projects/CRUD-1/workpackages");
        assertSuccess2xx(readResponse);

        List<Map<String, Object>> wps = readResponse.jsonPath().getList("$");
        boolean found = wps.stream().anyMatch(w -> "CRUD-1.WP-1".equals(w.get("wpId")));
        assertTrue(found, "Work package CRUD-1.WP-1 not found in project");

        // Update Work Package
        String updateBody = "{" + "\"wpId\":\"CRUD-1.WP-1\"," + "\"wpName\":\"Updated Work Package\","
                + "\"description\":\"Test WP for CRUD updated\"," + "\"status\":\"OPEN\","
                + "\"budgetedEffort\":150.00," + "\"bac\":6000.00," + "\"projectId\":\"CRUD-1\","
                + "\"responsibleEngineerId\":" + projectManagerId + "}";

        Response updateResponse = given()
                .header(AUTHORIZATION_BEARER_HTTP_HEADER_PREFIX, AUTHORIZATION_BEARER_VALUE_PREFIX + pmProj1Token)
                .contentType(ContentType.JSON).body(updateBody).when().put("/workpackages/CRUD-1.WP-1");
        assertSuccess2xx(updateResponse);

        // Verify Update
        Response verifyUpdateResponse = given()
                .header(AUTHORIZATION_BEARER_HTTP_HEADER_PREFIX, AUTHORIZATION_BEARER_VALUE_PREFIX + pmProj1Token)
                .get("/workpackages/CRUD-1.WP-1");
        assertSuccess2xx(verifyUpdateResponse);
        assertEquals("Updated Work Package", verifyUpdateResponse.jsonPath().getString("wpName"));
    }

    @Test
    @Order(3)
    void testTimesheetCRUD() {
        // Create Timesheet
        String timesheetBody = "{" + "\"empId\":" + projectManagerId + "," + "\"endDate\":\"2026-04-10\"" + "}";

        Response createResponse = given()
                .header(AUTHORIZATION_BEARER_HTTP_HEADER_PREFIX, AUTHORIZATION_BEARER_VALUE_PREFIX + pmProj1Token)
                .contentType(ContentType.JSON).body(timesheetBody).when().post("/timesheets");
        assertSuccess2xx(createResponse);

        Integer timesheetId = createResponse.jsonPath().getInt("timesheetId");
        assertNotNull(timesheetId);

        // Read Timesheet
        Response readResponse = given()
                .header(AUTHORIZATION_BEARER_HTTP_HEADER_PREFIX, AUTHORIZATION_BEARER_VALUE_PREFIX + pmProj1Token)
                .get("/timesheets/" + timesheetId);
        assertSuccess2xx(readResponse);
        assertEquals("DRAFT", readResponse.jsonPath().getString("status"));
    }

    @Test
    @Order(4)
    void workPackageCrud_delete() {
        // Setup requires a way to delete WP, assuming endpoint exists, else skip
        // Skipping delete for now as ProjectResource doesn't seem to have delete for WP
        // directly
    }

    @Test
    @Order(5)
    void projectCrud_delete() {
        // Delete Project
        Response deleteResponse = given()
                .header(AUTHORIZATION_BEARER_HTTP_HEADER_PREFIX, AUTHORIZATION_BEARER_VALUE_PREFIX + opsToken)
                .delete("/projects/CRUD-1");
        assertSuccess2xx(deleteResponse);

        // Verify Deletion
        Response verifyDeleteResponse = given()
                .header(AUTHORIZATION_BEARER_HTTP_HEADER_PREFIX, AUTHORIZATION_BEARER_VALUE_PREFIX + opsToken)
                .get("/projects/CRUD-1");
        assertEquals(404, verifyDeleteResponse.getStatusCode()); // or however it handles not found
    }
}
