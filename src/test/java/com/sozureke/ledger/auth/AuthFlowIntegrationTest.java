package com.sozureke.ledger.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.sozureke.ledger.support.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthFlowIntegrationTest {

        private static final String PASSWORD = "correct-horse";

        @Autowired
        MockMvc mockMvc;

        @Test
        void registerLoginUseTokenAndLogout() throws Exception {
                String email = UUID.randomUUID() + "@example.com";
                String userId = register(email);
                String token = login(email, PASSWORD);

                mockMvc.perform(get("/api/v1/users/{id}", userId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(userId))
                                .andExpect(jsonPath("$.passwordHash").doesNotExist());

                mockMvc.perform(post("/api/v1/auth/logout")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                                .andExpect(status().isNoContent());

                mockMvc.perform(get("/api/v1/users/{id}", userId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        }

        @Test
        void requestWithoutTokenIsRejected() throws Exception {
                mockMvc.perform(get("/api/v1/users/{id}", UUID.randomUUID()))
                                .andExpect(status().isUnauthorized())
                                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        }

        @Test
        void forgedTokenIsRejected() throws Exception {
                mockMvc.perform(get("/api/v1/users/{id}", UUID.randomUUID())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + UUID.randomUUID()))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        }

        @Test
        void wrongPasswordAndUnknownEmailGiveTheSameResponseShape() throws Exception {
                String email = UUID.randomUUID() + "@example.com";
                register(email);

                String wrongPassword = mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"" + email + "\",\"password\":\"not-the-password\"}"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                                .andReturn().getResponse().getContentAsString();

                String unknownEmail = mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"" + UUID.randomUUID()
                                                + "@example.com\",\"password\":\"not-the-password\"}"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                                .andReturn().getResponse().getContentAsString();

                assertThat((String) JsonPath.read(wrongPassword, "$.detail"))
                                .isEqualTo(JsonPath.read(unknownEmail, "$.detail"));
                assertThat((String) JsonPath.read(wrongPassword, "$.title"))
                                .isEqualTo(JsonPath.read(unknownEmail, "$.title"));
        }

        @Test
        void loginIsCaseInsensitiveForEmail() throws Exception {
                String email = UUID.randomUUID() + "@example.com";
                register(email);

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"" + PASSWORD
                                                + "\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").exists());
        }

        private String register(String email) throws Exception {
                String body = mockMvc.perform(post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"" + email + "\",\"name\":\"Ann\",\"password\":\"" + PASSWORD
                                                + "\"}"))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();
                return JsonPath.read(body, "$.id");
        }

        private String login(String email, String password) throws Exception {
                String body = mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                return JsonPath.read(body, "$.accessToken");
        }
}
