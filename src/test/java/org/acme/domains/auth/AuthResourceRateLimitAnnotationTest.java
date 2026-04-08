package org.acme.domains.auth;

import io.quarkiverse.bucket4j.runtime.RateLimited;
import org.acme.domains.auth.dto.LoginRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthResourceRateLimitAnnotationTest {

    @Test
    void shouldKeepAuthGroupRateLimitOnLoginEndpoint() throws NoSuchMethodException {
        Method method = AuthResource.class.getMethod("login", LoginRequest.class);

        RateLimited annotation = method.getAnnotation(RateLimited.class);

        assertNotNull(annotation);
        assertEquals("auth-group", annotation.bucket());
    }
}
