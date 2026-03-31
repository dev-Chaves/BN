package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestProfile(SwaggerDocsSecurityTestProfile.class)
class SwaggerDocsSecurityTest {

    @Test
    void shouldReturnUnauthorizedWithoutBasicAuthForSwaggerUi() {
        given()
                .when()
                .get("/q/swagger-ui")
                .then()
                .statusCode(401);
    }

    @Test
    void shouldReturnUnauthorizedWithoutBasicAuthForOpenApi() {
        given()
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(401);
    }

    @Test
    void shouldAllowSwaggerUiWithValidBasicAuth() {
        given()
                .auth().preemptive().basic("docs-user", "docs-pass")
                .when()
                .get("/q/swagger-ui")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldAllowOpenApiWithValidBasicAuth() {
        given()
                .auth().preemptive().basic("docs-user", "docs-pass")
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(200);
    }
}
