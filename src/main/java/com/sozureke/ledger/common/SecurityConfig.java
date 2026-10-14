package com.sozureke.ledger.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http,
                        SecurityProblemHandler problemHandler,
                        OpaqueTokenIntrospector introspector) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(requests -> requests
                                                .requestMatchers(HttpMethod.POST, "/api/v1/users", "/api/v1/auth/login")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .exceptionHandling(handling -> handling
                                                .authenticationEntryPoint(problemHandler)
                                                .accessDeniedHandler(problemHandler))
                                .oauth2ResourceServer(resourceServer -> resourceServer
                                                .authenticationEntryPoint(problemHandler)
                                                .accessDeniedHandler(problemHandler)
                                                .opaqueToken(token -> token.introspector(introspector)));
                return http.build();
        }
}
