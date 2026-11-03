package com.booking.identity.presentation

import com.booking.identity.domain.Role
import com.booking.identity.domain.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class RegisterRequest(
    @field:NotBlank(message = "Email обязателен для заполнения.")
    @field:Email(message = "Некорректный формат email.")
    @field:Size(max = 255, message = "Email не должен превышать 255 символов.")
    val email: String,

    @field:NotBlank(message = "Пароль обязателен для заполнения.")
    @field:Size(min = 8, message = "Пароль должен содержать не менее 8 символов.")
    @field:Size(max = 100, message = "Пароль не должен превышать 100 символов.")
    val password: String,

    @field:NotBlank(message = "Имя обязательно для заполнения.")
    @field:Size(max = 100, message = "Имя не должно превышать 100 символов.")
    val name: String,

    @field:NotBlank(message = "Номер телефона обязателен для заполнения.")
    @field:Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Номер телефона должен содержать от 10 до 15 цифр.")
    val phone: String
)

data class LoginRequest(
    @field:NotBlank(message = "Email обязателен для заполнения.")
    @field:Email(message = "Некорректный формат email.")
    val email: String,

    @field:NotBlank(message = "Пароль обязателен для заполнения.")
    val password: String
)

data class UserResponse(
    val id: UUID,
    val email: String,
    val name: String,
    val phone: String,
    val role: Role,
    val createdAt: Instant
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id,
                email = user.email,
                name = user.name,
                phone = user.phone,
                role = user.role,
                createdAt = user.createdAt
            )
        }
    }
}
