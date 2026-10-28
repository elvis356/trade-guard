package com.tradeguard.config;

import com.tradeguard.security.JwtCookieAuthenticationFilter;
import com.tradeguard.security.OriginValidationFilter;
import com.tradeguard.security.SecurityErrorWriter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableConfigurationProperties({
        AuthProperties.class, MarketProperties.class, SurveillanceProperties.class
})
public class SecurityConfiguration {

    @Bean
    FilterRegistrationBean<JwtCookieAuthenticationFilter> jwtFilterServletRegistration(
            JwtCookieAuthenticationFilter filter) {
        FilterRegistrationBean<JwtCookieAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<OriginValidationFilter> originFilterServletRegistration(
            OriginValidationFilter filter) {
        FilterRegistrationBean<OriginValidationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AuthProperties properties) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.getAllowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "X-Requested-With"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtCookieAuthenticationFilter jwtFilter,
            OriginValidationFilter originFilter,
            SecurityErrorWriter errorWriter) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errorWriter.authenticationEntryPoint())
                        .accessDeniedHandler(errorWriter.accessDeniedHandler()))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'"))
                        .referrerPolicy(referrer -> referrer
                                .policy(org.springframework.security.web.header.writers
                                        .ReferrerPolicyHeaderWriter.ReferrerPolicy
                                        .STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/logout",
                                "/api/health", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/market/instruments",
                                "/api/market/instruments/**", "/api/market/*/info",
                                "/api/market/quotes", "/api/market/*/book",
                                "/api/market/*/trades", "/api/market/*/trades/**")
                                .permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/market/instruments/**")
                                .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/market/instruments/**")
                                .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/market/instruments/**")
                                .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/market/instruments/**")
                                .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/orders")
                                .hasRole("TRADER")
                        .requestMatchers(HttpMethod.DELETE, "/api/orders/*")
                                .hasRole("TRADER")
                        .requestMatchers(HttpMethod.GET, "/api/orders/mine",
                                "/api/orders/mine/**")
                                .hasRole("TRADER")
                        .requestMatchers(HttpMethod.GET, "/api/alerts", "/api/alerts/**",
                                "/api/cases", "/api/cases/**")
                                .hasAnyRole("SURVEILLANCE", "AUDITOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/alerts/*/case")
                                .hasRole("SURVEILLANCE")
                        .requestMatchers(HttpMethod.POST, "/api/cases/*/review")
                                .hasRole("SURVEILLANCE")
                        .requestMatchers("/api/audit/**")
                                .hasAnyRole("AUDITOR", "ADMIN")
                        .requestMatchers("/api/regulations/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(originFilter, JwtCookieAuthenticationFilter.class);
        return http.build();
    }
}
