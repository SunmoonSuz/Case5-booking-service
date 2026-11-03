package com.booking.booking.application

import com.booking.booking.domain.BookingStatus
import com.booking.booking.infrastructure.BookingRepository
import com.booking.booking.presentation.BookingResponse
import com.booking.booking.presentation.ClientBookingsResponse
import com.booking.identity.infrastructure.UserRepository
import com.booking.shared.exception.CancellationDeadlineExceededException
import com.booking.shared.exception.InvalidBookingStatusTransitionException
import com.booking.shared.exception.ResourceNotFoundException
import com.booking.shared.exception.UnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

@Service
class ClientBookingService(
    private val bookingRepository: BookingRepository,
    private val userRepository: UserRepository,
    private val clock: Clock
) {

    private val log = LoggerFactory.getLogger(ClientBookingService::class.java)

    @Transactional(readOnly = true)
    fun getClientBookings(clientEmail: String): ClientBookingsResponse {
        val client = userRepository.findByEmailIgnoreCase(clientEmail)
            .orElseThrow { UnauthorizedException("Пользователь не найден.") }

        val allBookings = bookingRepository.findAllByClientIdOrderByStartTimeDesc(client.id)

        val active = mutableListOf<BookingResponse>()
        val archive = mutableListOf<BookingResponse>()

        for (booking in allBookings) {
            val response = BookingResponse.from(booking)
            if (booking.status in setOf(BookingStatus.PENDING, BookingStatus.CONFIRMED)) {
                active.add(response)
            } else {
                archive.add(response)
            }
        }

        active.sortBy { it.startTime }
        archive.sortByDescending { it.startTime }

        return ClientBookingsResponse(active = active, archive = archive)
    }

    @Transactional
    fun cancelBooking(clientEmail: String, bookingId: UUID): BookingResponse {
        val client = userRepository.findByEmailIgnoreCase(clientEmail)
            .orElseThrow { UnauthorizedException("Пользователь не найден.") }

        val booking = bookingRepository.findById(bookingId)
            .orElseThrow { ResourceNotFoundException("Запись с ID $bookingId не найдена.") }

        // IDOR check: only booking owner can cancel it
        if (booking.client.id != client.id) {
            log.warn("IDOR attempt: user {} attempted to cancel booking {} of client {}", client.email, bookingId, booking.client.id)
            throw AccessDeniedException("Доступ запрещен. Нельзя отменить чужую запись.")
        }

        // Rule: cannot cancel past booking (BR-004, AC-EDGE-005)
        val now = clock.instant()
        if (!booking.startTime.isAfter(now)) {
            log.warn("Cancellation rejected: booking {} startTime {} is in the past", bookingId, booking.startTime)
            throw CancellationDeadlineExceededException("Невозможно отменить запись, время начала которой уже наступило.")
        }

        // State machine transition check
        if (!booking.status.canTransitionTo(BookingStatus.CANCELLED, isAdmin = false)) {
            throw InvalidBookingStatusTransitionException(
                "Невозможно отменить запись в статусе ${booking.status}."
            )
        }

        booking.status = BookingStatus.CANCELLED
        booking.updatedAt = now

        val saved = bookingRepository.save(booking)
        log.info("Booking {} successfully cancelled by client {}", bookingId, client.email)

        return BookingResponse.from(saved)
    }
}
