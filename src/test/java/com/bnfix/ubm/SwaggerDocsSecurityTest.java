package com.bnfix.ubm;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SwaggerDocsSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentationRequiresBasicAuthentication() throws Exception {
        mockMvc.perform(get("/q/openapi")).andExpect(status().isUnauthorized());
    }

    @Test
    void documentationAcceptsConfiguredBasicAuthentication() throws Exception {
        mockMvc.perform(get("/q/openapi").with(httpBasic("test-docs", "test-docs-password")))
                .andExpect(status().isOk());
    }
}
