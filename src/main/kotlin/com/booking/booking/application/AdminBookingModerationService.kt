package com.booking.booking.application

import com.booking.booking.domain.BookingStatus
import com.booking.booking.infrastructure.BookingRepository
import com.booking.booking.presentation.BookingResponse
import com.booking.shared.config.AppProperties
import com.booking.shared.exception.InvalidBookingStatusTransitionException
import com.booking.shared.exception.ResourceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
class AdminBookingModerationService(
    private val bookingRepository: BookingRepository,
    private val clock: Clock,
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(AdminBookingModerationService::class.java)

    @Transactional(readOnly = true)
    fun getBookings(
        status: BookingStatus?,
        masterId: UUID?,
        date: LocalDate?
    ): List<BookingResponse> {
        val (dayStart, dayEnd) = if (date != null) {
            val venueZone = ZoneId.of(appProperties.timezone)
            val start = date.atStartOfDay(venueZone).toInstant()
            val end = date.plusDays(1).atStartOfDay(venueZone).toInstant()
            start to end
        } else {
            null to null
        }

        return bookingRepository.findAdminBookings(status, masterId, dayStart, dayEnd)
            .map { BookingResponse.from(it) }
    }

    @Transactional
    fun confirmBooking(bookingId: UUID): BookingResponse {
        return transitionTo(bookingId, BookingStatus.CONFIRMED)
    }

    @Transactional
    fun rejectBooking(bookingId: UUID): BookingResponse {
        return transitionTo(bookingId, BookingStatus.REJECTED)
    }

    @Transactional
    fun cancelBooking(bookingId: UUID): BookingResponse {
        return transitionTo(bookingId, BookingStatus.CANCELLED)
    }

    @Transactional
    fun completeBooking(bookingId: UUID): BookingResponse {
        return transitionTo(bookingId, BookingStatus.COMPLETED)
    }

    private fun transitionTo(bookingId: UUID, targetStatus: BookingStatus): BookingResponse {
        val booking = bookingRepository.findById(bookingId)
            .orElseThrow { ResourceNotFoundException("Запись с ID $bookingId не найдена.") }

        if (!booking.status.canTransitionTo(targetStatus, isAdmin = true)) {
            log.warn("Invalid transition: booking {} status {} cannot transition to {}",
                bookingId, booking.status, targetStatus)
            throw InvalidBookingStatusTransitionException(
                "Недопустимый перевод статуса из ${booking.status} в $targetStatus."
            )
        }

        booking.status = targetStatus
        booking.updatedAt = clock.instant()

        val saved = bookingRepository.save(booking)
        log.info("Admin updated booking {} status to {}", bookingId, targetStatus)

        return BookingResponse.from(saved)
    }
}
