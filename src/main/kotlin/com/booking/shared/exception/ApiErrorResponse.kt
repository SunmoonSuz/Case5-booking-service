package com.booking.shared.exception

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiErrorResponse(
    val error: String,
    val message: String,
    val details: List<FieldErrorDetail>? = null,
    val timestamp: Instant = Instant.now()
)

data class FieldErrorDetail(
    val field: String,
    val issue: String
)
