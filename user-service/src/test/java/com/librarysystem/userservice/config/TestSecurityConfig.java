package com.librarysystem.userservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@TestConfiguration
@EnableWebSecurity // Enables Spring Security's web security support
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true) // Enables method-level security like @PreAuthorize
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for testing simplicity
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Consistent with microservice
            // Permit all requests at the HTTP level, relying on method-level security (@PreAuthorize)
            .authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
            // Configure SecurityContextRepository to ensure SecurityContext is propagated correctly
            // when set by filters like our mocked RoleExtractionFilter.
            .securityContext(context -> context.securityContextRepository(securityContextRepository()));
        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        // RequestAttributeSecurityContextRepository is suitable for scenarios where the SecurityContext 
        // is populated per request, typically by a custom filter.
        return new RequestAttributeSecurityContextRepository();
    }
}
