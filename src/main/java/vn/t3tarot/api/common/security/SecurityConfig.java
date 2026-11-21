package vn.t3tarot.api.common.security;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import vn.t3tarot.api.common.security.jwt.AccessTokenAuthenticationConverter;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    static final long HSTS_MAX_AGE_SECONDS = 63_072_000L;
    static final String CONTENT_SECURITY_POLICY = "default-src 'none'; frame-ancestors 'none'";
    static final String PERMISSIONS_POLICY = "geolocation=(), camera=(), microphone=()";

    private static final String[] SWAGGER_PATHS = {
        "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"
    };

    @Bean
    @Order(1)
    SecurityFilterChain managementFilterChain(
            HttpSecurity http,
            @Value("${management.server.port:-1}") int managementPort,
            @Value("${server.port:8080}") int serverPort)
            throws Exception {
        http.securityMatcher(new ManagementPortRequestMatcher(managementPort, serverPort))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return statelessDefaults(http).build();
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    SecurityFilterChain apiFilterChain(
            HttpSecurity http,
            SecurityProperties properties,
            ProblemDetailSecurityHandler problemHandler,
            AccessTokenAuthenticationConverter authenticationConverter)
            throws Exception {
        statelessDefaults(http)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> {
                    String[] publicPaths = publicPaths(properties);
                    if (publicPaths.length > 0) {
                        authorize.requestMatchers(publicPaths).permitAll();
                    }
                    authorize.anyRequest().authenticated();
                })
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter))
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            SecurityProperties properties, @Value("${t3tarot.security.cors.excluded-paths:}") List<String> excludedPaths) {
        SecurityProperties.Cors cors = properties.cors();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(cors.allowedOrigins());
        configuration.setAllowedMethods(cors.allowedMethods());
        configuration.setAllowedHeaders(cors.allowedHeaders());
        configuration.setExposedHeaders(cors.exposedHeaders());
        configuration.setMaxAge(cors.maxAgeSeconds());
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return request -> excludedPaths.contains(request.getRequestURI()) ? null : source.getCorsConfiguration(request);
    }

    private static HttpSecurity statelessDefaults(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .headers(SecurityConfig::configureHeaders);
    }

    private static void configureHeaders(HeadersConfigurer<HttpSecurity> headers) {
        headers.contentTypeOptions(Customizer.withDefaults())
                .cacheControl(Customizer.withDefaults())
                .xssProtection(xss -> xss.disable())
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(HSTS_MAX_AGE_SECONDS))
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                .permissionsPolicyHeader(permissions -> permissions.policy(PERMISSIONS_POLICY));
    }

    private static String[] publicPaths(SecurityProperties properties) {
        Stream<String> swaggerPaths = properties.swaggerPublic() ? Arrays.stream(SWAGGER_PATHS) : Stream.empty();
        return Stream.concat(properties.publicPaths().stream(), swaggerPaths).toArray(String[]::new);
    }
}
