package com.booking.booking.application

import com.booking.booking.domain.Booking
import com.booking.booking.domain.BookingStatus
import com.booking.booking.infrastructure.BookingRepository
import com.booking.booking.presentation.BookingResponse
import com.booking.booking.presentation.CreateBookingRequest
import com.booking.catalog.infrastructure.MasterRepository
import com.booking.catalog.infrastructure.ServiceRepository
import com.booking.identity.infrastructure.UserRepository
import com.booking.schedule.infrastructure.MasterScheduleRepository
import com.booking.shared.config.AppProperties
import com.booking.shared.exception.BookingInPastException
import com.booking.shared.exception.ResourceNotFoundException
import com.booking.shared.exception.ServiceNotOfferedByMasterException
import com.booking.shared.exception.SlotOutsideScheduleException
import com.booking.shared.exception.UnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class BookingService(
    private val bookingRepository: BookingRepository,
    private val userRepository: UserRepository,
    private val serviceRepository: ServiceRepository,
    private val masterRepository: MasterRepository,
    private val masterScheduleRepository: MasterScheduleRepository,
    private val clock: Clock,
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(BookingService::class.java)

    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun createBooking(clientEmail: String, request: CreateBookingRequest): BookingResponse {
        val client = userRepository.findByEmailIgnoreCase(clientEmail)
            .orElseThrow { UnauthorizedException("Пользователь не найден.") }

        val service = serviceRepository.findByIdAndIsActiveTrue(request.serviceId)
            .orElseThrow { ResourceNotFoundException("Активная услуга с ID ${request.serviceId} не найдена.") }

        val master = masterRepository.findByIdAndIsActiveTrue(request.masterId)
            .orElseThrow { ResourceNotFoundException("Активный специалист с ID ${request.masterId} не найден.") }

        // Rule 1: Master must offer this service
        if (master.services.none { it.id == service.id }) {
            throw ServiceNotOfferedByMasterException("Специалист ${master.name} не оказывает услугу ${service.name}.")
        }

        // Rule 2: Booking time must not be in the past (BR-006, AC-EDGE-008)
        val now = clock.instant()
        if (!request.startTime.isAfter(now)) {
            throw BookingInPastException("Время начала записи не может быть в прошлом.")
        }

        val duration = Duration.ofMinutes(service.durationMinutes.toLong())
        val endTime = request.startTime.plus(duration)

        // Rule 3: Slot must fit entirely within master's working hours (AC-EDGE-006)
        val venueZone = ZoneId.of(appProperties.timezone)
        val startVenue = ZonedDateTime.ofInstant(request.startTime, venueZone)
        val endVenue = ZonedDateTime.ofInstant(endTime, venueZone)
        val dayOfWeek = startVenue.dayOfWeek.value

        val shifts = masterScheduleRepository.findAllByMasterIdAndDayOfWeek(master.id, dayOfWeek)
        val fitsInShift = shifts.any { shift ->
            startVenue.toLocalDate() == endVenue.toLocalDate() &&
            !startVenue.toLocalTime().isBefore(shift.startTime) &&
            !endVenue.toLocalTime().isAfter(shift.endTime) &&
            startVenue.toLocalTime().isBefore(endVenue.toLocalTime())
        }

        if (!fitsInShift) {
            log.warn("Booking rejected: requested interval [{}, {}) falls outside master {} schedule on day {}",
                startVenue.toLocalTime(), endVenue.toLocalTime(), master.id, dayOfWeek)
            throw SlotOutsideScheduleException("Выбранное время выходит за пределы рабочего графика специалиста.")
        }

        // Rule 4: Snapshot service details (ADR-011)
        val booking = Booking(
            client = client,
            master = master,
            service = service,
            serviceNameSnapshot = service.name,
            servicePriceSnapshot = service.price,
            startTime = request.startTime,
            endTime = endTime,
            status = BookingStatus.PENDING
        )

        // Force flush to trigger PostgreSQL exclusion constraint within the transaction
        val saved = bookingRepository.saveAndFlush(booking)
        log.info("Created booking: id={}, master={}, client={}, slot=[{}, {})",
            saved.id, master.name, client.email, saved.startTime, saved.endTime)

        return BookingResponse.from(saved)
    }
}
