package com.nqt.common_starter.config;

import com.nqt.common_starter.security.CustomAccessDeniedHandler;
import com.nqt.common_starter.security.CustomAuthenticationEntryPoint;
import com.nqt.common_starter.security.GrpcJwtAuthenticationProvider;
import com.nqt.common_starter.security.JwtAuthenticationFilter;
import com.nqt.common_starter.security.JwtTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class SecurityConfig {

        JwtTokenVerifier jwtTokenVerifier;
        CustomAccessDeniedHandler accessDeniedHandler;
        CustomAuthenticationEntryPoint authenticationEntryPoint;
        SecurityProperties securityProperties;
        GrpcJwtAuthenticationProvider grpcJwtAuthenticationProvider;

        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter() {
                return new JwtAuthenticationFilter(
                                securityProperties,
                                jwtTokenVerifier);
        }

        @Bean
        public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
                        JwtAuthenticationFilter filter) {
                FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);

                registration.setEnabled(false);

                return registration;
        }

        @Bean
        public SecurityFilterChain filterChain(
                        HttpSecurity http,
                        JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {

                List<String> publicEndpoints = securityProperties.getPublicEndpoints();

                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)
                                .formLogin(AbstractHttpConfigurer::disable)

                                .authenticationProvider(grpcJwtAuthenticationProvider)

                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(authenticationEntryPoint)
                                                .accessDeniedHandler(accessDeniedHandler))

                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(publicEndpoints.toArray(new String[0])).permitAll()
                                                .anyRequest().authenticated())

                                .addFilterBefore(
                                                jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        ObjectProvider<UserDetailsService> userDetailsServiceProvider,
                        ObjectProvider<PasswordEncoder> passwordEncoderProvider) {
                List<AuthenticationProvider> providers = new ArrayList<>();

                UserDetailsService userDetailsService = userDetailsServiceProvider.getIfAvailable();

                PasswordEncoder passwordEncoder = passwordEncoderProvider.getIfAvailable();

                if (userDetailsService != null && passwordEncoder != null) {
                        DaoAuthenticationProvider daoProvider = new DaoAuthenticationProvider();
                        daoProvider.setUserDetailsService(userDetailsService);
                        daoProvider.setPasswordEncoder(passwordEncoder);

                        providers.add(daoProvider);
                }

                providers.add(grpcJwtAuthenticationProvider);

                return new ProviderManager(providers);
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(10);
        }
}