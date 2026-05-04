package com.nqt.common_starter.config;

import com.nqt.common_starter.security.CustomAccessDeniedHandler;
import com.nqt.common_starter.security.CustomAuthenticationEntryPoint;
import com.nqt.common_starter.security.GatewaySignatureFilter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true,level = lombok.AccessLevel.PRIVATE)
public class SecurityConfig {

    GatewaySignatureFilter gatewaySignatureFilter;
    CustomAccessDeniedHandler accessDeniedHandler;
    CustomAuthenticationEntryPoint authenticationEntryPoint;
    SecurityProperties securityProperties;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        List<String> endpoints = securityProperties.getPublicEndpoints();
        http
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(gatewaySignatureFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(endpoints.toArray(new String[0])).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
