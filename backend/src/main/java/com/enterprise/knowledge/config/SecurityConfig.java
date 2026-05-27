package com.enterprise.knowledge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/assets/**", "/static/**", "/favicon.ico").permitAll()
                .requestMatchers("/api/v1/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().permitAll()
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );
        
        return http.build();
    }
}