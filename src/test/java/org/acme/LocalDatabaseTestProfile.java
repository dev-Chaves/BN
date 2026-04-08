package org.acme;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class LocalDatabaseTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.ofEntries(
                Map.entry("quarkus.datasource.db-kind", "postgresql"),
                Map.entry("quarkus.datasource.username", "postgres"),
                Map.entry("quarkus.datasource.password", "password"),
                Map.entry("quarkus.datasource.jdbc.url", "jdbc:postgresql://localhost:5432/benefix"),
                Map.entry("quarkus.datasource.reactive.url", "postgresql://localhost:5432/benefix"),
                Map.entry("quarkus.flyway.migrate-at-start", "true"),
                Map.entry("quarkus.http.test-port", "0"),
                Map.entry("mp.jwt.verify.publickey.location", "publicKey.pem"),
                Map.entry("smallrye.jwt.sign.key.location", "privateKey.pem"),
                Map.entry("mp.jwt.verify.issuer", "bn-api"),

                Map.entry("AUTH_RATE_LIMIT_PERIOD_SECOND", "1S"),
                Map.entry("AUTH_RATE_LIMIT_USES_SECOND", "1000"),
                Map.entry("AUTH_RATE_LIMIT_PERIOD_MINUTE", "1M"),
                Map.entry("AUTH_RATE_LIMIT_USES_MINUTE", "5000"),
                Map.entry("ONBOARDING_RATE_LIMIT_PERIOD_SECOND", "1S"),
                Map.entry("ONBOARDING_RATE_LIMIT_USES_SECOND", "1000"),
                Map.entry("ONBOARDING_RATE_LIMIT_PERIOD_MINUTE", "1M"),
                Map.entry("ONBOARDING_RATE_LIMIT_USES_MINUTE", "5000")
        );
    }
}
