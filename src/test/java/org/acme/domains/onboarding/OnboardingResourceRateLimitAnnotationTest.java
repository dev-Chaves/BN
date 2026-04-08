package org.acme.domains.onboarding;

import io.quarkiverse.bucket4j.runtime.RateLimited;
import org.acme.domains.onboarding.dto.OnboardingRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OnboardingResourceRateLimitAnnotationTest {

    @Test
    void shouldKeepOnboardingGroupRateLimitOnOnboardingEndpoint() throws NoSuchMethodException {
        Method method = OnboardingResource.class.getMethod("onboardingCompany", OnboardingRequest.class);

        RateLimited annotation = method.getAnnotation(RateLimited.class);

        assertNotNull(annotation);
        assertEquals("onboarding-group", annotation.bucket());
    }
}
