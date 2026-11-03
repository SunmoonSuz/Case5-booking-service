package com.booking.identity.application

import com.booking.identity.domain.Role
import com.booking.identity.domain.User
import com.booking.identity.infrastructure.UserRepository
import com.booking.identity.presentation.LoginRequest
import com.booking.identity.presentation.RegisterRequest
import com.booking.shared.exception.EmailAlreadyExistsException
import com.booking.shared.exception.InvalidCredentialsException
import com.booking.shared.exception.UnauthorizedException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional
import java.util.UUID

class AuthServiceTest {

    private val userRepository: UserRepository = mockk()
    private val passwordEncoder: PasswordEncoder = mockk()
    private val authService = AuthService(userRepository, passwordEncoder)

    private val request = MockHttpServletRequest()
    private val response = MockHttpServletResponse()

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `register successfully creates CLIENT user with normalized email and password hash`() {
        val registerReq = RegisterRequest(
            email = "  Client@Example.COM ",
            password = "password123",
            name = "Иван Иванов",
            phone = "+79991234567"
        )

        every { userRepository.existsByEmailIgnoreCase("client@example.com") } returns false
        every { passwordEncoder.encode("password123") } returns "hashed_password123"

        val userSlot = slot<User>()
        every { userRepository.save(capture(userSlot)) } answers {
            User(
                id = UUID.randomUUID(),
                email = userSlot.captured.email,
                passwordHash = userSlot.captured.passwordHash,
                role = userSlot.captured.role,
                name = userSlot.captured.name,
                phone = userSlot.captured.phone
            )
        }

        val result = authService.register(registerReq, request, response)

        assertThat(result.email).isEqualTo("client@example.com")
        assertThat(result.name).isEqualTo("Иван Иванов")
        assertThat(result.phone).isEqualTo("+79991234567")
        assertThat(result.role).isEqualTo(Role.CLIENT)

        assertThat(userSlot.captured.email).isEqualTo("client@example.com")
        assertThat(userSlot.captured.passwordHash).isEqualTo("hashed_password123")
        assertThat(userSlot.captured.role).isEqualTo(Role.CLIENT)

        // Verify session was established
        val auth = SecurityContextHolder.getContext().authentication
        assertThat(auth).isNotNull
        assertThat(auth.name).isEqualTo("client@example.com")
        assertThat(auth.authorities).extracting("authority").contains("ROLE_CLIENT")
    }

    @Test
    fun `register with duplicate email throws EmailAlreadyExistsException`() {
        val registerReq = RegisterRequest(
            email = "client@example.com",
            password = "password123",
            name = "Иван",
            phone = "+79991234567"
        )

        every { userRepository.existsByEmailIgnoreCase("client@example.com") } returns true

        assertThatThrownBy { authService.register(registerReq, request, response) }
            .isInstanceOf(EmailAlreadyExistsException::class.java)
            .hasMessageContaining("уже зарегистрирован")

        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `login with valid credentials establishes session and returns UserResponse`() {
        val loginReq = LoginRequest(
            email = "Client@example.COM",
            password = "password123"
        )

        val existingUser = User(
            id = UUID.randomUUID(),
            email = "client@example.com",
            passwordHash = "encoded_hash",
            role = Role.CLIENT,
            name = "Иван",
            phone = "+79991234567"
        )

        every { userRepository.findByEmailIgnoreCase("client@example.com") } returns Optional.of(existingUser)
        every { passwordEncoder.matches("password123", "encoded_hash") } returns true

        val result = authService.login(loginReq, request, response)

        assertThat(result.id).isEqualTo(existingUser.id)
        assertThat(result.email).isEqualTo("client@example.com")
        assertThat(result.role).isEqualTo(Role.CLIENT)

        val auth = SecurityContextHolder.getContext().authentication
        assertThat(auth).isNotNull
        assertThat(auth.name).isEqualTo("client@example.com")
        assertThat(auth.authorities).extracting("authority").contains("ROLE_CLIENT")
    }

    @Test
    fun `login with wrong password throws InvalidCredentialsException`() {
        val loginReq = LoginRequest(
            email = "client@example.com",
            password = "wrongPassword"
        )

        val existingUser = User(
            id = UUID.randomUUID(),
            email = "client@example.com",
            passwordHash = "encoded_hash",
            role = Role.CLIENT,
            name = "Иван",
            phone = "+79991234567"
        )

        every { userRepository.findByEmailIgnoreCase("client@example.com") } returns Optional.of(existingUser)
        every { passwordEncoder.matches("wrongPassword", "encoded_hash") } returns false

        assertThatThrownBy { authService.login(loginReq, request, response) }
            .isInstanceOf(InvalidCredentialsException::class.java)
            .hasMessage("Неверный email или пароль.")

        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test
    fun `login with non-existent email throws InvalidCredentialsException`() {
        val loginReq = LoginRequest(
            email = "unknown@example.com",
            password = "password123"
        )

        every { userRepository.findByEmailIgnoreCase("unknown@example.com") } returns Optional.empty()

        assertThatThrownBy { authService.login(loginReq, request, response) }
            .isInstanceOf(InvalidCredentialsException::class.java)
            .hasMessage("Неверный email или пароль.")
    }

    @Test
    fun `getCurrentUser returns profile when authenticated`() {
        val user = User(
            id = UUID.randomUUID(),
            email = "client@example.com",
            passwordHash = "encoded_hash",
            role = Role.CLIENT,
            name = "Иван",
            phone = "+79991234567"
        )

        val auth = UsernamePasswordAuthenticationToken(
            "client@example.com",
            null,
            listOf(SimpleGrantedAuthority("ROLE_CLIENT"))
        )
        SecurityContextHolder.getContext().authentication = auth

        every { userRepository.findByEmailIgnoreCase("client@example.com") } returns Optional.of(user)

        val result = authService.getCurrentUser()

        assertThat(result.id).isEqualTo(user.id)
        assertThat(result.email).isEqualTo("client@example.com")
        assertThat(result.name).isEqualTo("Иван")
    }

    @Test
    fun `getCurrentUser without authentication throws UnauthorizedException`() {
        SecurityContextHolder.clearContext()

        assertThatThrownBy { authService.getCurrentUser() }
            .isInstanceOf(UnauthorizedException::class.java)
            .hasMessage("Требуется аутентификация для выполнения данной операции.")
    }

    @Test
    fun `getCurrentUser with anonymous token throws UnauthorizedException`() {
        val anonymousToken = AnonymousAuthenticationToken(
            "key",
            "anonymousUser",
            listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        )
        SecurityContextHolder.getContext().authentication = anonymousToken

        assertThatThrownBy { authService.getCurrentUser() }
            .isInstanceOf(UnauthorizedException::class.java)
    }
}
