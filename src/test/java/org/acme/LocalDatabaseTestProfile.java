package org.acme;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class LocalDatabaseTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.datasource.db-kind", "postgresql",
                "quarkus.datasource.username", "postgres",
                "quarkus.datasource.password", "password",
                "quarkus.datasource.jdbc.url", "jdbc:postgresql://localhost:5432/benefix",
                "quarkus.datasource.reactive.url", "postgresql://localhost:5432/benefix",
                "quarkus.flyway.migrate-at-start", "true",
                "quarkus.http.test-port", "0",
                "mp.jwt.verify.publickey.location", "publicKey.pem",
                "smallrye.jwt.sign.key.location", "privateKey.pem",
                "mp.jwt.verify.issuer", "bn-api"
        );
    }
}
