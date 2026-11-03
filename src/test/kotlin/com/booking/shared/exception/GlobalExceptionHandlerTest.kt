package com.booking.shared.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.postgresql.util.PSQLException
import org.postgresql.util.ServerErrorMessage
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import java.sql.SQLException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `handleDomainException returns correct status and body for SlotAlreadyBookedException`() {
        val ex = SlotAlreadyBookedException()
        val response = handler.handleDomainException(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.error).isEqualTo("SLOT_ALREADY_BOOKED")
        assertThat(response.body!!.message).contains("Выбранный слот уже занят")
        assertThat(response.body!!.timestamp).isNotNull()
    }

    @Test
    fun `handleDomainException returns correct status and body for EmailAlreadyExistsException`() {
        val ex = EmailAlreadyExistsException()
        val response = handler.handleDomainException(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.error).isEqualTo("EMAIL_ALREADY_EXISTS")
        assertThat(response.body!!.message).contains("Пользователь с таким email уже зарегистрирован")
    }

    @Test
    fun `handleDataIntegrityViolation maps 23P01 exclusion violation to 409 SLOT_ALREADY_BOOKED`() {
        // PSQLException with 23P01
        val psqlEx = SQLException("ERROR: conflicting key value violates exclusion constraint \"no_overlapping_active_bookings\"", "23P01")
        val dataIntegrityEx = DataIntegrityViolationException("Database constraint violation", psqlEx)

        val response = handler.handleDataIntegrityViolation(dataIntegrityEx)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.error).isEqualTo("SLOT_ALREADY_BOOKED")
        assertThat(response.body!!.message).isEqualTo("Выбранный слот уже занят другим клиентом. Пожалуйста, выберите другое время.")
    }

    @Test
    fun `handleDataIntegrityViolation maps idx_users_email_ci to 409 EMAIL_ALREADY_EXISTS`() {
        val psqlEx = SQLException("ERROR: duplicate key value violates unique constraint \"idx_users_email_ci\"", "23505")
        val dataIntegrityEx = DataIntegrityViolationException("Database constraint violation", psqlEx)

        val response = handler.handleDataIntegrityViolation(dataIntegrityEx)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.error).isEqualTo("EMAIL_ALREADY_EXISTS")
        assertThat(response.body!!.message).isEqualTo("Пользователь с таким email уже зарегистрирован.")
    }

    @Test
    fun `handleDataIntegrityViolation maps non-email 23505 unique violation to 409 DATA_INTEGRITY_VIOLATION`() {
        val psqlEx = SQLException("ERROR: duplicate key value violates unique constraint \"idx_services_name_unique\"", "23505")
        val dataIntegrityEx = DataIntegrityViolationException("Database constraint violation", psqlEx)

        val response = handler.handleDataIntegrityViolation(dataIntegrityEx)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.error).isEqualTo("DATA_INTEGRITY_VIOLATION")
        assertThat(response.body!!.message).isEqualTo("Нарушение ограничений целостности данных.")
    }

    @Test
    fun `handleDataIntegrityViolation maps other data integrity violation to 409 DATA_INTEGRITY_VIOLATION`() {
        val dataIntegrityEx = DataIntegrityViolationException("Foreign key violation: master_id not found")

        val response = handler.handleDataIntegrityViolation(dataIntegrityEx)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.error).isEqualTo("DATA_INTEGRITY_VIOLATION")
        assertThat(response.body!!.message).isEqualTo("Нарушение ограничений целостности данных.")
    }

    @Test
    fun `handleAccessDenied returns 403 ACCESS_DENIED`() {
        val ex = AccessDeniedException("Forbidden")
        val response = handler.handleAccessDenied(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.error).isEqualTo("ACCESS_DENIED")
    }

    @Test
    fun `handleAuthentication returns 401 UNAUTHORIZED`() {
        val ex = BadCredentialsException("Bad credentials")
        val response = handler.handleAuthentication(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.error).isEqualTo("UNAUTHORIZED")
    }

    @Test
    fun `handleGenericException returns 500 INTERNAL_SERVER_ERROR without leaking details`() {
        val ex = RuntimeException("Sensitive database password connection failed")
        val response = handler.handleGenericException(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.error).isEqualTo("INTERNAL_SERVER_ERROR")
        assertThat(response.body!!.message).isEqualTo("Внутренняя ошибка сервера. Пожалуйста, попробуйте позже.")
        assertThat(response.body!!.message).doesNotContain("password")
        assertThat(response.body!!.details).isNull()
    }
}
