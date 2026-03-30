package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

@QuarkusTest
@TestProfile(LocalDatabaseTestProfile.class)
class CriticalIntegrationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger((int) (System.currentTimeMillis() % 1_000_000));

    @Test
    void shouldOnboardAndLoginManager() {
        OnboardedTenant tenant = onboardTenant("onboard");
        LoginSession managerSession = loginSession(tenant.managerEmail, tenant.managerPassword);

        given()
                .header("Cookie", managerSession.cookiePair())
                .when()
                .get("/companies/me")
                .then()
                .statusCode(200)
                .body("id", equalTo(tenant.companyId.intValue()))
                .body("name", equalTo(tenant.companyName))
                .body("cnpj", equalTo(tenant.companyCnpj));

        given()
                .header("Authorization", "Bearer " + managerSession.token())
                .when()
                .get("/companies/me")
                .then()
                .statusCode(401);
    }

    @Test
    void shouldAuthenticateUsingJwtCookieWithoutAuthorizationHeader() {
        OnboardedTenant tenant = onboardTenant("cookie-auth");
        LoginSession managerSession = loginSession(tenant.managerEmail, tenant.managerPassword);

        given()
                .header("Cookie", managerSession.cookiePair())
                .when()
                .get("/companies/me")
                .then()
                .statusCode(200)
                .body("id", equalTo(tenant.companyId.intValue()))
                .body("name", equalTo(tenant.companyName))
                .body("cnpj", equalTo(tenant.companyCnpj));
    }

    @Test
    void shouldHandlePartnershipLifecycle() {
        OnboardedTenant provider = onboardTenant("provider");
        OnboardedTenant client = onboardTenant("client");

        String providerCookie = login(provider.managerEmail, provider.managerPassword);
        String clientCookie = login(client.managerEmail, client.managerPassword);

        long firstBenefitId = createBenefit(providerCookie, provider.companyId, "Gym Plan");
        long secondBenefitId = createBenefit(providerCookie, provider.companyId, "Meal Plan");

        long partnershipToDisable = requestPartnership(clientCookie, firstBenefitId);
        given()
                .header("Cookie", providerCookie)
                .queryParam("partnershipId", partnershipToDisable)
                .when()
                .put("/partnerships/accept")
                .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"));

        given()
                .header("Cookie", providerCookie)
                .queryParam("partnershipId", partnershipToDisable)
                .when()
                .put("/partnerships/disable")
                .then()
                .statusCode(200)
                .body("status", equalTo("DISABLED"));

        long partnershipToReject = requestPartnership(clientCookie, secondBenefitId);
        given()
                .header("Cookie", providerCookie)
                .queryParam("partnershipId", partnershipToReject)
                .when()
                .put("/partnerships/reject")
                .then()
                .statusCode(200)
                .body("status", equalTo("REJECTED"));
    }

    @Test
    void shouldRequireActivePartnershipToSubscribe() {
        OnboardedTenant provider = onboardTenant("provider-subscription");
        OnboardedTenant client = onboardTenant("client-subscription");

        String providerCookie = login(provider.managerEmail, provider.managerPassword);
        String clientCookie = login(client.managerEmail, client.managerPassword);

        long benefitId = createBenefit(providerCookie, provider.companyId, "Transport Benefit");
        long partnershipId = requestPartnership(clientCookie, benefitId);
        String employeeEmail = "employee-sub+" + SEQUENCE.incrementAndGet() + "@bn.dev";
        String employeePassword = "employee-pass-123";
        long employeeId = createEmployee(clientCookie, client.companyId, employeeEmail, employeePassword);
        activateEmployee(clientCookie, employeeId);
        String employeeCookie = login(employeeEmail, employeePassword);

        given()
                .header("Cookie", employeeCookie)
                .contentType("application/json")
                .body("""
                        {
                          "benefitId": %d
                        }
                        """.formatted(benefitId))
                .when()
                .post("/subscriptions")
                .then()
                .statusCode(400)
                .body("message", containsString("No active partnership"));

        given()
                .header("Cookie", providerCookie)
                .queryParam("partnershipId", partnershipId)
                .when()
                .put("/partnerships/accept")
                .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"));

        given()
                .header("Cookie", employeeCookie)
                .contentType("application/json")
                .body("""
                        {
                          "benefitId": %d
                        }
                        """.formatted(benefitId))
                .when()
                .post("/subscriptions")
                .then()
                .statusCode(201)
                .body("id", notNullValue());
    }

    @Test
    void shouldEnforceTenantAndRoleAuthorization() {
        OnboardedTenant provider = onboardTenant("provider-authz");
        OnboardedTenant client = onboardTenant("client-authz");

        String providerCookie = login(provider.managerEmail, provider.managerPassword);
        String clientCookie = login(client.managerEmail, client.managerPassword);

        String employeeEmail = "employee-role+" + SEQUENCE.incrementAndGet() + "@bn.dev";
        String employeePassword = "employee-pass-123";
        long employeeId = createEmployee(clientCookie, client.companyId, employeeEmail, employeePassword);
        activateEmployee(clientCookie, employeeId);
        String employeeCookie = login(employeeEmail, employeePassword);

        long benefitId = createBenefit(providerCookie, provider.companyId, "Dental Plan");

        given()
                .header("Cookie", employeeCookie)
                .contentType("application/json")
                .body("""
                        {
                          "benefitId": %d
                        }
                        """.formatted(benefitId))
                .when()
                .post("/partnerships")
                .then()
                .statusCode(403);

        given()
                .header("Cookie", clientCookie)
                .contentType("application/json")
                .body("""
                        {
                          "name": "Tenant Mismatch Employee",
                          "cpf": "%s",
                          "email": "tenant-mismatch+%d@bn.dev",
                          "password": "employee-pass-123",
                          "companyId": %d
                        }
                        """.formatted(
                        generateValidCpf(SEQUENCE.incrementAndGet() + 7000),
                        SEQUENCE.get(),
                        provider.companyId))
                .when()
                .post("/employees")
                .then()
                .statusCode(403)
                .body("message", containsString("Tenant mismatch"));
    }

    private OnboardedTenant onboardTenant(String prefix) {
        int id = SEQUENCE.incrementAndGet();
        String managerEmail = prefix + "+" + id + "@bn.dev";
        String managerPassword = "manager-pass-123";
        String managerCpf = generateValidCpf(id);
        String cnpj = generateValidCnpj(id);
        String companyName = "Company " + id;

        given()
                .contentType("application/json")
                .body("""
                        {
                          "company": {
                            "name": "%s",
                            "cnpj": "%s"
                          },
                          "manager": {
                            "name": "Manager %d",
                            "cpf": "%s",
                            "email": "%s",
                            "password": "%s"
                          }
                        }
                        """.formatted(companyName, cnpj, id, managerCpf, managerEmail, managerPassword))
                .when()
                .post("/onboarding")
                .then()
                .statusCode(201);

        String token = login(managerEmail, managerPassword);
        Long companyId = given()
                .header("Cookie", token)
                .when()
                .get("/companies/me")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getLong("id");

        return new OnboardedTenant(companyId, companyName, cnpj, managerEmail, managerPassword);
    }

    private String login(String email, String password) {
        return loginSession(email, password).cookiePair();
    }

    private LoginSession loginSession(String email, String password) {
        io.restassured.response.Response response = given()
                .contentType("application/json")
                .body("""
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(email, password))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .header("Set-Cookie", allOf(
                        containsString("jwt="),
                        containsString("HttpOnly"),
                        containsString("Secure"),
                        containsString("SameSite=Strict"),
                        startsWith("jwt=")
                ))
                .extract()
                .response();
        String token = response.jsonPath().getString("token");
        String setCookie = response.getHeader("Set-Cookie");
        String cookiePair = setCookie.split(";", 2)[0];
        return new LoginSession(token, cookiePair);
    }

    private long createBenefit(String managerCookie, long companyId, String benefitName) {
        return given()
                .header("Cookie", managerCookie)
                .contentType("application/json")
                .body("""
                        {
                          "name": "%s",
                          "description": "Description for %s",
                          "companyId": %d
                        }
                        """.formatted(benefitName, benefitName, companyId))
                .when()
                .post("/benefits")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }

    private long requestPartnership(String managerCookie, long benefitId) {
        return given()
                .header("Cookie", managerCookie)
                .contentType("application/json")
                .body("""
                        {
                          "benefitId": %d
                        }
                        """.formatted(benefitId))
                .when()
                .post("/partnerships")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }

    private long createEmployee(String managerCookie, long companyId, String email, String password) {
        int id = SEQUENCE.incrementAndGet();
        return given()
                .header("Cookie", managerCookie)
                .contentType("application/json")
                .body("""
                        {
                          "name": "Employee %d",
                          "cpf": "%s",
                          "email": "%s",
                          "password": "%s",
                          "companyId": %d
                        }
                        """.formatted(id, generateValidCpf(id + 3000), email, password, companyId))
                .when()
                .post("/employees")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }

    private void activateEmployee(String managerCookie, long employeeId) {
        given()
                .header("Cookie", managerCookie)
                .queryParam("employeeId", employeeId)
                .when()
                .put("/employees/activate")
                .then()
                .statusCode(200)
                .body("active", equalTo("ACTIVE"));
    }

    private String generateValidCpf(int seed) {
        String nineDigits = String.format("%09d", Math.abs(seed) % 1_000_000_000);
        int firstDigit = cpfDigit(nineDigits, 10);
        int secondDigit = cpfDigit(nineDigits + firstDigit, 11);
        return nineDigits + firstDigit + secondDigit;
    }

    private int cpfDigit(String base, int weightStart) {
        int sum = 0;
        int weight = weightStart;
        for (int i = 0; i < base.length(); i++) {
            sum += Character.getNumericValue(base.charAt(i)) * weight--;
        }
        int result = 11 - (sum % 11);
        return result >= 10 ? 0 : result;
    }

    private String generateValidCnpj(int seed) {
        String twelveDigits = String.format("%012d", 100_000_000_000L + Math.abs(seed));
        int firstDigit = cnpjDigit(twelveDigits, new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        int secondDigit = cnpjDigit(twelveDigits + firstDigit, new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return twelveDigits + firstDigit + secondDigit;
    }

    private int cnpjDigit(String base, int[] weights) {
        int sum = 0;
        for (int i = 0; i < base.length(); i++) {
            sum += Character.getNumericValue(base.charAt(i)) * weights[i];
        }
        int result = sum % 11;
        return result < 2 ? 0 : 11 - result;
    }

    private record OnboardedTenant(
            Long companyId,
            String companyName,
            String companyCnpj,
            String managerEmail,
            String managerPassword
    ) {
    }

    private record LoginSession(
            String token,
            String cookiePair
    ) {
    }
}
