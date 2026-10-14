package com.sozureke.ledger.auth;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabaseOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    private final AuthService authService;

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        UUID userId = authService.resolveUserId(token)
                .orElseThrow(() -> new BadOpaqueTokenException("Invalid token"));
        return new DefaultOAuth2AuthenticatedPrincipal(
                userId.toString(),
                Map.of("sub", userId.toString()),
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }
}
