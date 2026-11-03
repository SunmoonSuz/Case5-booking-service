package com.booking.shared.exception

import jakarta.validation.ConstraintViolationException
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(DomainException::class)
    fun handleDomainException(ex: DomainException): ResponseEntity<ApiErrorResponse> {
        log.warn("Domain exception [{}]: {}", ex.errorCode, ex.message)
        val response = ApiErrorResponse(
            error = ex.errorCode,
            message = ex.message,
            details = ex.details,
            timestamp = Instant.now()
        )
        return ResponseEntity.status(ex.status).body(response)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> {
        val details = ex.bindingResult.fieldErrors.map { fieldError ->
            FieldErrorDetail(
                field = fieldError.field,
                issue = fieldError.defaultMessage ?: "Некорректное значение"
            )
        }
        log.warn("Validation failed for {}: {}", ex.target?.javaClass?.simpleName, details)
        val response = ApiErrorResponse(
            error = "VALIDATION_FAILED",
            message = "Ошибка валидации входных данных.",
            details = details,
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<ApiErrorResponse> {
        val details = ex.constraintViolations.map { violation ->
            FieldErrorDetail(
                field = violation.propertyPath.toString(),
                issue = violation.message ?: "Некорректное значение"
            )
        }
        log.warn("Constraint violation: {}", details)
        val response = ApiErrorResponse(
            error = "VALIDATION_FAILED",
            message = "Ошибка валидации входных данных.",
            details = details,
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(ex: DataIntegrityViolationException): ResponseEntity<ApiErrorResponse> {
        val rootCause = ex.rootCause
        val rootMessage = rootCause?.message ?: ex.message ?: ""

        // Check for PostgreSQL exclusion violation (SQLSTATE 23P01) or constraint name
        val isExclusionViolation = (rootCause is PSQLException && rootCause.sqlState == "23P01") ||
                rootMessage.contains("no_overlapping_active_bookings", ignoreCase = true) ||
                rootMessage.contains("exclusion_violation", ignoreCase = true)

        if (isExclusionViolation) {
            log.warn("Detected exclusion violation (overbooking collision): {}", rootMessage)
            val response = ApiErrorResponse(
                error = "SLOT_ALREADY_BOOKED",
                message = "Выбранный слот уже занят другим клиентом. Пожалуйста, выберите другое время.",
                timestamp = Instant.now()
            )
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response)
        }

        // Check for Unique Constraint violation (SQLSTATE 23505) - specifically email duplication
        val isEmailConstraint = rootMessage.contains("idx_users_email_ci", ignoreCase = true) ||
                rootMessage.contains("users_email_key", ignoreCase = true)
        val isUniqueEmailViolation = (rootCause is PSQLException && rootCause.sqlState == "23505" && isEmailConstraint) || isEmailConstraint

        if (isUniqueEmailViolation) {
            log.warn("Detected unique email violation: {}", rootMessage)
            val response = ApiErrorResponse(
                error = "EMAIL_ALREADY_EXISTS",
                message = "Пользователь с таким email уже зарегистрирован.",
                timestamp = Instant.now()
            )
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response)
        }

        log.error("Data integrity violation: {}", rootMessage, ex)
        val response = ApiErrorResponse(
            error = "DATA_INTEGRITY_VIOLATION",
            message = "Нарушение ограничений целостности данных.",
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ApiErrorResponse> {
        log.warn("Malformed HTTP request body: {}", ex.message)
        val response = ApiErrorResponse(
            error = "VALIDATION_FAILED",
            message = "Некорректный формат тела запроса.",
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<ApiErrorResponse> {
        log.warn("Access denied: {}", ex.message)
        val response = ApiErrorResponse(
            error = "ACCESS_DENIED",
            message = "Доступ запрещен. Недостаточно прав для выполнения операции.",
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response)
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(ex: AuthenticationException): ResponseEntity<ApiErrorResponse> {
        log.warn("Authentication failed: {}", ex.message)
        val response = ApiErrorResponse(
            error = "UNAUTHORIZED",
            message = "Требуется аутентификация.",
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ApiErrorResponse> {
        log.error("Unhandled internal server error", ex)
        val response = ApiErrorResponse(
            error = "INTERNAL_SERVER_ERROR",
            message = "Внутренняя ошибка сервера. Пожалуйста, попробуйте позже.",
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }
}
