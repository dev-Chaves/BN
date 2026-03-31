package org.acme;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.HashMap;
import java.util.Map;

public class SwaggerDocsSecurityTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> overrides = new HashMap<>(new LocalDatabaseTestProfile().getConfigOverrides());
        overrides.put("quarkus.http.auth.basic", "true");
        overrides.put("quarkus.http.auth.permission.docs.paths", "/q/swagger-ui/*,/q/openapi*");
        overrides.put("quarkus.http.auth.permission.docs.policy", "authenticated");
        overrides.put("quarkus.smallrye-openapi.enable", "true");
        overrides.put("smallrye.swagger-ui.always-include", "true");
        overrides.put("app.swagger.auth.username", "docs-user");
        overrides.put("app.swagger.auth.password", "docs-pass");
        return overrides;
    }
}
