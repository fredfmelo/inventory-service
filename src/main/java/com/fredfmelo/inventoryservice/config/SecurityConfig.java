package com.fredfmelo.inventoryservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fredfmelo.inventoryservice.security.JwtAuthFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/products", "/products/{productId}").permitAll()
                        .requestMatchers(HttpMethod.POST, "/products").hasAuthority("SELLER")
                        .requestMatchers(HttpMethod.GET, "/products/me").hasAuthority("SELLER")
                        .requestMatchers(HttpMethod.PUT, "/products/{productId}").hasAnyAuthority("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/products/{productId}").hasAnyAuthority("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/products/{productId}/inventory").hasAnyAuthority("SELLER", "ADMIN")
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
