package com.booking.identity.infrastructure

import com.booking.shared.config.AppProperties
import com.booking.shared.config.JacksonConfig
import com.booking.shared.presentation.ConfigController
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.ResponseEntity
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin")
class TestAdminController {
    @GetMapping("/bookings")
    fun getAdminBookings(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "ok"))
    }
}

@WebMvcTest(controllers = [ConfigController::class, TestAdminController::class])
@Import(SpringSecurityConfig::class, JacksonConfig::class, AppProperties::class, TestAdminController::class)
class SecurityFilterChainTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `public endpoint GET config is accessible without authentication`() {
        mockMvc.perform(get("/api/v1/config"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.timezone").value("Europe/Moscow"))
            .andExpect(jsonPath("$.currency").value("RUB"))
    }

    @Test
    fun `unauthenticated access to admin endpoint returns 401 UNAUTHORIZED in ApiErrorResponse format`() {
        mockMvc.perform(get("/api/v1/admin/bookings"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.message").value("Требуется аутентификация для выполнения данной операции."))
            .andExpect(jsonPath("$.timestamp").isNotEmpty)
    }

    @Test
    @WithMockUser(roles = ["CLIENT"])
    fun `client role access to admin endpoint returns 403 ACCESS_DENIED in ApiErrorResponse format`() {
        mockMvc.perform(get("/api/v1/admin/bookings"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.message").value("Доступ запрещен. Недостаточно прав для выполнения операции."))
            .andExpect(jsonPath("$.timestamp").isNotEmpty)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `admin role access to admin endpoint returns 200 OK`() {
        mockMvc.perform(get("/api/v1/admin/bookings"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ok"))
    }

    @Test
    fun `POST logout invalidates session and clears JSESSIONID cookie`() {
        mockMvc.perform(
            post("/api/v1/auth/logout")
                .cookie(Cookie("JSESSIONID", "test-session-id"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("LOGGED_OUT"))
            .andExpect(cookie().maxAge("JSESSIONID", 0))
    }
}
