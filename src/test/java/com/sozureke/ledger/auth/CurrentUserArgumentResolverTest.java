package com.sozureke.ledger.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

class CurrentUserArgumentResolverTest {

    private final CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void supportsOnlyTheCurrentUserType() {
        MethodParameter currentUser = mock(MethodParameter.class);
        when(currentUser.getParameterType()).thenAnswer(inv -> CurrentUser.class);
        MethodParameter other = mock(MethodParameter.class);
        when(other.getParameterType()).thenAnswer(inv -> String.class);

        assertThat(resolver.supportsParameter(currentUser)).isTrue();
        assertThat(resolver.supportsParameter(other)).isFalse();
    }

    @Test
    void resolvesTheUserIdFromTheVerifiedTokenPrincipal() {
        UUID id = UUID.randomUUID();
        DefaultOAuth2AuthenticatedPrincipal principal = new DefaultOAuth2AuthenticatedPrincipal(
                id.toString(), Map.of("sub", id.toString()), List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "raw", Instant.now(), Instant.now().plusSeconds(60));
        SecurityContextHolder.getContext().setAuthentication(
                new BearerTokenAuthentication(principal, token, principal.getAuthorities()));

        Object resolved = resolver.resolveArgument(null, null, null, null);

        assertThat(resolved).isEqualTo(new CurrentUser(id));
    }

    @Test
    void refusesWhenThereIsNoAuthentication() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> resolver.resolveArgument(null, null, null, null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void refusesWhenThePrincipalIsNotATokenPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("someone", "pw", List.of()));

        assertThatThrownBy(() -> resolver.resolveArgument(null, null, null, null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void refusesWhenThePrincipalNameIsNotAUuid() {
        DefaultOAuth2AuthenticatedPrincipal principal = new DefaultOAuth2AuthenticatedPrincipal(
                "not-a-uuid", Map.of("sub", "not-a-uuid"), List.of());
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "raw", Instant.now(), Instant.now().plusSeconds(60));
        SecurityContextHolder.getContext().setAuthentication(
                new BearerTokenAuthentication(principal, token, List.of()));

        assertThatThrownBy(() -> resolver.resolveArgument(null, null, null, null))
                .isInstanceOf(RuntimeException.class);
    }
}
