package ch.semafor.intens.ws.config;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

@Profile("oauth2")
@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class OAuth2Security {
    final Logger logger = LoggerFactory.getLogger(OAuth2Security.class);
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    String jwkSetUri;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // OpenID Connect
        // .cors(Customizer.withDefaults()
        http.authorizeHttpRequests((authz) -> authz
                        .requestMatchers("/services/rest/**").authenticated()
                        // hasAuthority("SCOPE_profile")
                        /* Note Keycloak authorities:
                         "authorities": [
                              "SCOPE_profile",
                              "SCOPE_email",
                              "SCOPE_openid"
                         ]
                         */
                        .anyRequest().permitAll()) // for monitoring access
                //.cors(Customizer.withDefaults())
                .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
                .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling((exceptions) -> exceptions
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
                );
        http.headers((headers) -> headers
            .httpStrictTransportSecurity((httpStrictTransportSecurity) -> httpStrictTransportSecurity
                .maxAgeInSeconds(31536000)
            )
        );
        return http.build();
    }

    /* modify prefix SCOPE_ to ROLE_
    see https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html#oauth2resourceserver-jwt-authorization-extraction
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
          jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
                  Map<String, Collection<String>> realmAccess = jwt.getClaim("realm_access");
            Collection<String> roles = realmAccess.get("roles");
            return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        });
        return jwtAuthenticationConverter;
    }

    /* prevent UserDetailsServiceAutoConfiguration */
    @Bean
    AuthenticationManager noopAuthenticationManager() {
        return authentication -> {
            throw new AuthenticationServiceException("Authentication is disabled");
        };
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(this.jwkSetUri).build();
    }

}
