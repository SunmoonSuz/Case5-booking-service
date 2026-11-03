package com.booking.identity.application

import com.booking.identity.domain.Role
import com.booking.identity.domain.User
import com.booking.identity.infrastructure.UserRepository
import com.booking.identity.presentation.LoginRequest
import com.booking.identity.presentation.RegisterRequest
import com.booking.identity.presentation.UserResponse
import com.booking.shared.exception.EmailAlreadyExistsException
import com.booking.shared.exception.InvalidCredentialsException
import com.booking.shared.exception.UnauthorizedException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    private val log = LoggerFactory.getLogger(AuthService::class.java)
    private val securityContextRepository = HttpSessionSecurityContextRepository()

    @Transactional
    fun register(
        request: RegisterRequest,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse
    ): UserResponse {
        val normalizedEmail = request.email.trim().lowercase()

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            log.warn("Registration rejected: email {} is already registered", normalizedEmail)
            throw EmailAlreadyExistsException("Пользователь с таким email уже зарегистрирован.")
        }

        val user = User(
            email = normalizedEmail,
            passwordHash = passwordEncoder.encode(request.password),
            role = Role.CLIENT,
            name = request.name.trim(),
            phone = request.phone.trim()
        )

        val savedUser = userRepository.save(user)
        log.info("New user registered: id={}, email={}", savedUser.id, savedUser.email)

        establishSession(savedUser, httpRequest, httpResponse)
        return UserResponse.from(savedUser)
    }

    @Transactional(readOnly = true)
    fun login(
        request: LoginRequest,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse
    ): UserResponse {
        val normalizedEmail = request.email.trim().lowercase()

        val user = userRepository.findByEmailIgnoreCase(normalizedEmail)
            .orElseThrow {
                log.warn("Login failed: user not found for email {}", normalizedEmail)
                InvalidCredentialsException("Неверный email или пароль.")
            }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            log.warn("Login failed: invalid password for user {}", normalizedEmail)
            throw InvalidCredentialsException("Неверный email или пароль.")
        }

        log.info("User logged in successfully: id={}, email={}", user.id, user.email)
        establishSession(user, httpRequest, httpResponse)
        return UserResponse.from(user)
    }

    @Transactional(readOnly = true)
    fun getCurrentUser(): UserResponse {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication == null ||
            !authentication.isAuthenticated ||
            authentication is AnonymousAuthenticationToken
        ) {
            throw UnauthorizedException("Требуется аутентификация для выполнения данной операции.")
        }

        val email = authentication.name
        val user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow {
                log.warn("Authenticated user {} not found in database", email)
                UnauthorizedException("Пользователь не найден.")
            }

        return UserResponse.from(user)
    }

    private fun establishSession(
        user: User,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse
    ) {
        val authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
        val authentication = UsernamePasswordAuthenticationToken(user.email, null, authorities)

        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = authentication
        SecurityContextHolder.setContext(context)

        securityContextRepository.saveContext(context, httpRequest, httpResponse)

        val session = httpRequest.getSession(true)
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context)
    }
}
