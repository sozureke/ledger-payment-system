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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.sozureke.ledger.auth.AuthWebConfig;
import com.sozureke.ledger.auth.CurrentUserArgumentResolver;
import com.sozureke.ledger.common.SecurityConfig;
import com.sozureke.ledger.common.SecurityProblemHandler;

@WebMvcTest(UserController.class)
@Import({ SecurityConfig.class, SecurityProblemHandler.class, AuthWebConfig.class, CurrentUserArgumentResolver.class })
public class UserControllerTest {

    private static final String REGISTER_BODY =
            "{\"email\":\"ann@example.com\",\"name\":\"Ann\",\"password\":\"secret123\"}";

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    UserService userService;
    @MockitoBean
    OpaqueTokenIntrospector introspector;

    private static RequestPostProcessor asUser(UUID id) {
        return opaqueToken().attributes(attributes -> attributes.put("sub", id.toString()));
    }

    private static User userWithId(UUID id) {
        User user = User.customer("ann@example.com", "Ann", "hashed");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void createCustomerReturns201WithBodyAndNoSecrets() throws Exception {
        when(userService.createCustomer("ann@example.com", "Ann", "secret123"))
                .thenReturn(User.customer("ann@example.com", "Ann", "hashed"));

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ann"))
                .andExpect(jsonPath("$.email").value("ann@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString("hashed"))));
    }

    @Test
    void createCustomerReturns409WhenEmailAlreadyRegistered() throws Exception {
        when(userService.createCustomer("ann@example.com", "Ann", "secret123"))
                .thenThrow(new EmailAlreadyRegisteredException());

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
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
    void createCustomerReturns400WhenEmailIsNotAnEmail() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\",\"name\":\"Ann\",\"password\":\"secret123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
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
    void unsupportedMediaTypeReturns415WithCode() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.TEXT_PLAIN)
                .content("name=Ann"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void meReturnsTheProfileOfTheTokenOwner() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getById(id)).thenReturn(userWithId(id));

        mockMvc.perform(get("/api/v1/users/me").with(asUser(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("ann@example.com"))
                .andExpect(jsonPath("$.name").value("Ann"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void meReturns401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void meReturns404WhenTheTokenOwnerNoLongerExists() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getById(id)).thenThrow(new UserNotFoundException());

        mockMvc.perform(get("/api/v1/users/me").with(asUser(id)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void securityExceptionFromControllerBecomes401NotInternalError() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getById(id)).thenThrow(new AuthenticationCredentialsNotFoundException("no auth"));

        mockMvc.perform(get("/api/v1/users/me").with(asUser(id)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void accessDeniedFromControllerBecomes403NotInternalError() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getById(id)).thenThrow(new AccessDeniedException("denied"));

        mockMvc.perform(get("/api/v1/users/me").with(asUser(id)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void unexpectedErrorReturns500WithoutInternalDetails() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getById(id)).thenThrow(new IllegalStateException("db password is hunter2"));

        mockMvc.perform(get("/api/v1/users/me").with(asUser(id)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(content().string(not(containsString("hunter2"))));
    }

    @Test
    void listingOrReadingUsersByIdIsNotExposed() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", UUID.randomUUID()).with(asUser(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void unknownPathReturns404WithCode() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist").with(asUser(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void unsupportedMethodReturns405WithCode() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me").with(asUser(UUID.randomUUID())))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.traceId").exists());
    }
}
