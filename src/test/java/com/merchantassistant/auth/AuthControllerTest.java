package com.merchantassistant.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.security.enabled=true")
class AuthControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void shouldRequireLoginForBusinessApi() throws Exception {
        mockMvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("token").asText();
        mockMvc.perform(get("/api/products").header("X-Session-Token", token)).andExpect(status().isOk());
    }

    @Test
    void shouldRejectInvalidPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldManageUsersAndInvalidateChangedPasswordSession() throws Exception {
        String adminToken = login("admin", "admin123").get("token").asText();
        String created = mockMvc.perform(post("/api/users").header("X-Session-Token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"security-test\",\"password\":\"test123\",\"role\":\"CLERK\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("CLERK"))
                .andReturn().getResponse().getContentAsString();
        long userId = objectMapper.readTree(created).get("id").asLong();
        String clerkToken = login("security-test", "test123").get("token").asText();

        mockMvc.perform(get("/api/users").header("X-Session-Token", clerkToken)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/recovery-code").header("X-Session-Token", clerkToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"test123\",\"recoveryCode\":\"security-recover\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/change-password").header("X-Session-Token", clerkToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"test123\",\"newPassword\":\"newtest123\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/products").header("X-Session-Token", clerkToken)).andExpect(status().isUnauthorized());
        login("security-test", "newtest123");

        mockMvc.perform(put("/api/users/" + userId + "/status").header("X-Session-Token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void shouldRegisterClerkAndRecoverPasswordWithRecoveryCode() throws Exception {
        String registered = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new_clerk\",\"password\":\"start123\",\"recoveryCode\":\"recover-2026\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new_clerk"))
                .andExpect(jsonPath("$.role").value("CLERK"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andReturn().getResponse().getContentAsString();
        long userId = objectMapper.readTree(registered).get("id").asLong();
        String adminToken = login("admin", "admin123").get("token").asText();
        mockMvc.perform(put("/api/users/" + userId + "/status").header("X-Session-Token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));
        login("new_clerk", "start123");

        mockMvc.perform(post("/api/auth/recover-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new_clerk\",\"recoveryCode\":\"wrong-code\",\"newPassword\":\"changed123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名或找回码错误"));
        mockMvc.perform(post("/api/auth/recover-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new_clerk\",\"recoveryCode\":\"recover-2026\",\"newPassword\":\"changed123\"}"))
                .andExpect(status().isNoContent());
        login("new_clerk", "changed123");
    }

    private JsonNode login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("username", username, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }
}
