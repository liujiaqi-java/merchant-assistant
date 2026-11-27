package com.merchantassistant.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ai_fallback;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "openai.api-key=test-key",
        "openai.api-url=http://127.0.0.1:1/v1/responses"
})
class AiFallbackControllerTest {
    @Autowired private MockMvc mockMvc;

    @Test
    void shouldUseRulesWhenOpenAiCannotBeReached() throws Exception {
        mockMvc.perform(post("/api/analytics/weekly-report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("RULES"))
                .andExpect(jsonPath("$.statusMessage").value("无法连接 OpenAI API，已使用规则分析。"))
                .andExpect(jsonPath("$.summary").isNotEmpty());

        mockMvc.perform(post("/api/analytics/replenishment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("RULES"))
                .andExpect(jsonPath("$.statusMessage").value("无法连接 OpenAI API，已使用规则分析。"))
                .andExpect(jsonPath("$.items.length()").value(0));
    }
}
