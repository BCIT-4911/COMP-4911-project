package com.corejsf;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Shared REST Assured configuration for API integration tests.
 * <p>
 * Base URL and path come from JVM system properties {@code api.baseUri} and {@code api.basePath}.
 * {@code backend/pom.xml} passes them via Surefire (defaults: local WildFly {@code http://localhost:8080}
 * + {@code /Project/api}).
 * <p>
 * Against the squad OKD deployment (avoids Windows CLI mangling unquoted {@code -D} flags with {@code https:}):
 * <pre>
 * cd backend
 * mvn test -Premote-okd
 * </pre>
 * From PowerShell, if you set properties on the CLI you must quote each {@code -D} argument, for example:
 * <pre>
 * mvn test "-Dapi.baseUri=https://backend-liul-labs.apps.okd4.infoteach.ca" "-Dapi.basePath=/api"
 * </pre>
 */
public abstract class TestConfig {

    protected static final String DEFAULT_PASSWORD = "password";

    /** Resolved Looney-Tunes seed employees (see EmptyDbSeeder in backend sources). */
    public record StandardSeedIds(
            int opsId,
            int hrId,
            int pmProj1Id,
            int daffyId,
            int tweetyId,
            int sylvesterId,
            int marvinPmProj2Id,
            int elmerId
    ) {
    }

    @BeforeAll
    static void configureRestAssuredBase() {
        configureRestAssured();
    }

    public static void configureRestAssured() {
        String rawUri = System.getProperty("api.baseUri", "http://localhost:8080");
        if (!rawUri.contains("://")) {
            rawUri = "http://" + rawUri;
        }
        URI uri = URI.create(rawUri);
        RestAssured.baseURI = uri.getScheme() + "://" + uri.getHost();
        int port = uri.getPort();
        if (port == -1) {
            RestAssured.port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
        } else {
            RestAssured.port = port;
        }

        RestAssured.basePath = System.getProperty("api.basePath", "/Project/api");

        boolean relaxSsl = "https".equalsIgnoreCase(uri.getScheme())
                || Boolean.parseBoolean(System.getProperty("api.relaxedTls", "false"));
        if (relaxSsl) {
            RestAssured.config = RestAssuredConfig.config()
                    .sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation());
        }
    }

    /**
     * First login used to resolve seed users. Default empId {@code 1} matches a fresh {@code EmptyDbSeeder} DB.
     * Override with {@code -Dapi.seedOpsEmpId=...} if your environment differs.
     */
    public static String loginAsSeedOps() {
        int id = Integer.parseInt(System.getProperty("api.seedOpsEmpId", "1"));
        return login(id, DEFAULT_PASSWORD);
    }

    public static String login(int empId, String password) {
        Response response = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "empId": %d,
                          "password": "%s"
                        }
                        """.formatted(empId, password))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .response();

        String token = response.jsonPath().getString("token");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Login succeeded but token was missing for empId=" + empId);
        }
        return token;
    }

    /**
     * Login that expects HTTP 401 (wrong password, etc.).
     */
    public static Response loginRaw(int empId, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "empId": %d,
                          "password": "%s"
                        }
                        """.formatted(empId, password))
                .when()
                .post("/auth/login");
    }

    public static StandardSeedIds resolveStandardSeedIds(String opsToken) {
        List<Map<String, Object>> employees = given()
                .header("Authorization", "Bearer " + opsToken)
                .when()
                .get("/employees")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");

        Integer opsId = null;
        Integer hrId = null;
        Integer pmProj1Id = null;
        Integer daffyId = null;
        Integer tweetyId = null;
        Integer sylvesterId = null;
        Integer marvinId = null;
        Integer elmerId = null;

        for (Map<String, Object> e : employees) {
            String first = (String) e.get("empFirstName");
            String last = (String) e.get("empLastName");
            int id = ((Number) e.get("empId")).intValue();
            if ("Wile".equals(first) && "Coyote".equals(last)) {
                opsId = id;
            } else if ("Elmer".equals(first) && "Fudd".equals(last)) {
                elmerId = id;
            } else if ("Road".equals(first) && "Runner".equals(last)) {
                hrId = id;
            } else if ("Bugs".equals(first) && "Bunny".equals(last)) {
                pmProj1Id = id;
            } else if ("Daffy".equals(first) && "Duck".equals(last)) {
                daffyId = id;
            } else if ("Tweety".equals(first) && "Bird".equals(last)) {
                tweetyId = id;
            } else if ("Sylvester".equals(first) && "Cat".equals(last)) {
                sylvesterId = id;
            } else if ("Marvin".equals(first) && "Martian".equals(last)) {
                marvinId = id;
            }
        }

        assertNotNull(opsId, "Seed data missing: Wile Coyote (ADMIN). Ensure DB is seeded.");
        assertNotNull(elmerId, "Seed data missing: Elmer Fudd (OPS)");
        assertNotNull(hrId, "Seed data missing: Road Runner (HR)");
        assertNotNull(pmProj1Id, "Seed data missing: Bugs Bunny (PM PROJ-1)");
        assertNotNull(daffyId, "Seed data missing: Daffy Duck");
        assertNotNull(tweetyId, "Seed data missing: Tweety Bird");
        assertNotNull(sylvesterId, "Seed data missing: Sylvester Cat");
        assertNotNull(marvinId, "Seed data missing: Marvin Martian");

        return new StandardSeedIds(opsId, hrId, pmProj1Id, daffyId, tweetyId, sylvesterId, marvinId, elmerId);
    }
}
