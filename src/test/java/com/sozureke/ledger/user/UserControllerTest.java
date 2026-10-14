package com.sozureke.ledger.user;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.sozureke.ledger.common.SecurityConfig;
import com.sozureke.ledger.common.SecurityProblemHandler;

@WebMvcTest(UserController.class)
@Import({ SecurityConfig.class, SecurityProblemHandler.class })
public class UserControllerTest {
    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    UserService userService;
    @MockitoBean
    OpaqueTokenIntrospector introspector;

    @Test
    void createCustomerReturns201WithLocation() throws Exception {
        when(userService.createCustomer("ann@example.com", "Ann", "secret123"))
                .thenReturn(User.customer("ann@example.com", "Ann", "hashed"));

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ann@example.com\",\"name\":\"Ann\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Ann"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void getByIdReturns200WithUser() throws Exception {
        UUID id = UUID.randomUUID();
        User user = User.customer("ann@example.com", "Ann", "hashed");
        ReflectionTestUtils.setField(user, "id", id);
        when(userService.getById(id)).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/{id}", id).with(opaqueToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Ann"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void getByIdReturns404WhenUserNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getById(id)).thenThrow(new UserNotFoundException());

        mockMvc.perform(get("/api/v1/users/{id}", id).with(opaqueToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void createCustomerReturns400WithoutLeakingPasswordWhenInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ann@example.com\",\"name\":\"\",\"password\":\"tiny-pw\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(content().string(not(containsString("tiny-pw"))));
    }

    @Test
    void createCustomerReturns400WhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void getByIdReturns400WhenIdIsNotUuid() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", "not-a-uuid").with(opaqueToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ID"));
    }

    @Test
    void unexpectedErrorReturns500WithoutInternalDetails() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getById(id)).thenThrow(new IllegalStateException("db password is hunter2"));

        mockMvc.perform(get("/api/v1/users/{id}", id).with(opaqueToken()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(content().string(not(containsString("hunter2"))));
    }

    @Test
    void unknownPathReturns404NotInternalError() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist").with(opaqueToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void unsupportedMethodReturns405NotInternalError() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", UUID.randomUUID()).with(opaqueToken()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void unsupportedMediaTypeReturns415WithCode() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.TEXT_PLAIN)
                .content("name=Ann"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void createCustomerReturns409WhenEmailAlreadyRegistered() throws Exception {
        when(userService.createCustomer("ann@example.com", "Ann", "secret123"))
                .thenThrow(new EmailAlreadyRegisteredException());

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ann@example.com\",\"name\":\"Ann\",\"password\":\"secret123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void createCustomerReturns400WhenEmailIsNotAnEmail() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\",\"name\":\"Ann\",\"password\":\"secret123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getByIdReturns401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.traceId").exists());
    }
}
