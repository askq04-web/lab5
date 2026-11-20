package com.example.lab4.config;

import com.example.lab4.model.UserStatus;
import com.example.lab4.repository.UserRepository;
import com.example.lab4.service.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public UserDetailsService userDetailsService(UserRepository users) {
        return username -> {
            var user = users.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
            return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                    .password(user.getPassword()).roles(user.getRole().name())
                    .disabled(user.getStatus() != UserStatus.ACTIVE).build();
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwt, UserDetailsService users)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error", "/auth/login", "/auth/register", "/auth/refresh").permitAll()
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/analysis-methods/**").authenticated()
                        .requestMatchers("/api/analysis-methods/**", "/api/analyses/**", "/api/measurements/**")
                        .hasAnyRole("LAB", "ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtFilter(jwt, users), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) -> response.sendError(401))
                        .accessDeniedHandler((request, response, ex) -> response.sendError(403)));
        return http.build();
    }
}
