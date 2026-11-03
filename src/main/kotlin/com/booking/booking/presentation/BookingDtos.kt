package com.booking.booking.presentation

import com.booking.booking.domain.Booking
import com.booking.booking.domain.BookingStatus
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class CreateBookingRequest(
    @field:NotNull(message = "serviceId обязателен.")
    val serviceId: UUID,

    @field:NotNull(message = "masterId обязателен.")
    val masterId: UUID,

    @field:NotNull(message = "startTime обязателен.")
    val startTime: Instant
)

data class BookingResponse(
    val id: UUID,
    val clientId: UUID,
    val clientName: String,
    val clientPhone: String,
    val clientEmail: String,
    val masterId: UUID,
    val masterName: String,
    val serviceId: UUID,
    val serviceName: String,
    val servicePrice: BigDecimal,
    val startTime: Instant,
    val endTime: Instant,
    val status: BookingStatus,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(booking: Booking): BookingResponse {
            return BookingResponse(
                id = booking.id,
                clientId = booking.client.id,
                clientName = booking.client.name,
                clientPhone = booking.client.phone,
                clientEmail = booking.client.email,
                masterId = booking.master.id,
                masterName = booking.master.name,
                serviceId = booking.service.id,
                serviceName = booking.serviceNameSnapshot,
                servicePrice = booking.servicePriceSnapshot,
                startTime = booking.startTime,
                endTime = booking.endTime,
                status = booking.status,
                createdAt = booking.createdAt,
                updatedAt = booking.updatedAt
            )
        }
    }
}

data class ClientBookingsResponse(
    val active: List<BookingResponse>,
    val archive: List<BookingResponse>
)
