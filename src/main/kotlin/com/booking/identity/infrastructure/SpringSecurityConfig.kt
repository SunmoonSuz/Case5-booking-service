package com.booking.identity.infrastructure

import com.booking.shared.exception.ApiErrorResponse
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import java.time.Instant

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SpringSecurityConfig(
    private val objectMapper: ObjectMapper
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder(12)
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            }
            .exceptionHandling { handling ->
                // Return 401 Unauthorized for unauthenticated requests with standard ApiErrorResponse
                handling.authenticationEntryPoint { _, response, _ ->
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                    response.contentType = MediaType.APPLICATION_JSON_VALUE
                    response.characterEncoding = "UTF-8"
                    val errorResponse = ApiErrorResponse(
                        error = "UNAUTHORIZED",
                        message = "Требуется аутентификация для выполнения данной операции.",
                        timestamp = Instant.now()
                    )
                    response.writer.write(objectMapper.writeValueAsString(errorResponse))
                }
                // Return 403 Forbidden with standard ApiErrorResponse
                handling.accessDeniedHandler { _, response, _ ->
                    response.status = HttpServletResponse.SC_FORBIDDEN
                    response.contentType = MediaType.APPLICATION_JSON_VALUE
                    response.characterEncoding = "UTF-8"
                    val errorResponse = ApiErrorResponse(
                        error = "ACCESS_DENIED",
                        message = "Доступ запрещен. Недостаточно прав для выполнения операции.",
                        timestamp = Instant.now()
                    )
                    response.writer.write(objectMapper.writeValueAsString(errorResponse))
                }
            }
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints
                    .requestMatchers(
                        "/api/v1/auth/**",
                        "/api/v1/services/**",
                        "/api/v1/masters/**",
                        "/api/v1/availability/**",
                        "/api/v1/config",
                        "/error"
                    ).permitAll()
                    // Admin endpoints
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    // Client endpoints
                    .requestMatchers("/api/v1/client/**").hasRole("CLIENT")
                    .requestMatchers("/api/v1/bookings", "/api/v1/bookings/**").hasAnyRole("CLIENT", "ADMIN")
                    .anyRequest().authenticated()
            }
            .logout { logout ->
                logout
                    .logoutUrl("/api/v1/auth/logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler { _, response, _ ->
                        response.status = HttpServletResponse.SC_OK
                        response.contentType = MediaType.APPLICATION_JSON_VALUE
                        response.characterEncoding = "UTF-8"
                        response.writer.write("""{"status":"LOGGED_OUT"}""")
                    }
            }

        return http.build()
    }
}
