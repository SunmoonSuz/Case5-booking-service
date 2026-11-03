package com.booking.identity.presentation

import com.booking.AbstractIntegrationTest
import com.booking.identity.infrastructure.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
class AuthControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun cleanDatabase() {
        userRepository.deleteAll()
    }

    @Test
    fun `AC-001 - successful registration creates CLIENT user and establishes session`() {
        val request = RegisterRequest(
            email = "client@example.com",
            password = "password123",
            name = "Иван Иванов",
            phone = "+79991234567"
        )

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty)
            .andExpect(jsonPath("$.email").value("client@example.com"))
            .andExpect(jsonPath("$.name").value("Иван Иванов"))
            .andExpect(jsonPath("$.phone").value("+79991234567"))
            .andExpect(jsonPath("$.role").value("CLIENT"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty)
            .andExpect(cookie().exists("JSESSIONID"))

        val user = userRepository.findByEmailIgnoreCase("client@example.com").orElse(null)
        assertThat(user).isNotNull
        assertThat(user!!.role.name).isEqualTo("CLIENT")
        assertThat(user.passwordHash).isNotEqualTo("password123")
    }

    @Test
    fun `AC-EDGE-003 - duplicate email registration in different case returns 409 EMAIL_ALREADY_EXISTS`() {
        val firstRequest = RegisterRequest(
            email = "client@example.com",
            password = "password123",
            name = "Иван",
            phone = "+79991234567"
        )

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest))
        ).andExpect(status().isCreated)

        // Second registration with different casing
        val secondRequest = RegisterRequest(
            email = "Client@Example.COM",
            password = "differentPassword123",
            name = "Другой Иван",
            phone = "+79997654321"
        )

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRequest))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_EXISTS"))
            .andExpect(jsonPath("$.message").isNotEmpty)

        // Only 1 user should exist
        assertThat(userRepository.count()).isEqualTo(1)
    }

    @Test
    fun `AC-EDGE-004 - short password rejection returns 400 VALIDATION_FAILED`() {
        val shortPasswordRequest = RegisterRequest(
            email = "short@example.com",
            password = "short", // < 8 characters
            name = "Петр",
            phone = "+79991234567"
        )

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(shortPasswordRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.details[?(@.field == 'password')].issue").value("Пароль должен содержать не менее 8 символов."))

        assertThat(userRepository.count()).isEqualTo(0)
    }

    @Test
    fun `successful login returns 200 OK and establishes session`() {
        val registerRequest = RegisterRequest(
            email = "login_test@example.com",
            password = "password123",
            name = "Алексей",
            phone = "+79991234567"
        )

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
        ).andExpect(status().isCreated)

        val loginRequest = LoginRequest(
            email = "LOGIN_TEST@example.com",
            password = "password123"
        )

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("login_test@example.com"))
            .andExpect(cookie().exists("JSESSIONID"))
    }

    @Test
    fun `login with invalid password returns 401 INVALID_CREDENTIALS`() {
        val registerRequest = RegisterRequest(
            email = "login_fail@example.com",
            password = "password123",
            name = "Алексей",
            phone = "+79991234567"
        )

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
        ).andExpect(status().isCreated)

        val loginRequest = LoginRequest(
            email = "login_fail@example.com",
            password = "wrongPassword123"
        )

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
            .andExpect(jsonPath("$.message").value("Неверный email или пароль."))
    }

    @Test
    fun `GET me with active session returns current user profile`() {
        val registerRequest = RegisterRequest(
            email = "session_user@example.com",
            password = "password123",
            name = "Сергей",
            phone = "+79991234567"
        )

        val registerResult = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val sessionCookie = registerResult.response.getCookie("JSESSIONID")
        assertThat(sessionCookie).isNotNull

        mockMvc.perform(
            get("/api/v1/auth/me")
                .cookie(sessionCookie!!)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("session_user@example.com"))
            .andExpect(jsonPath("$.name").value("Сергей"))
            .andExpect(jsonPath("$.role").value("CLIENT"))
    }

    @Test
    fun `GET me without session returns 401 UNAUTHORIZED`() {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
    }
}
