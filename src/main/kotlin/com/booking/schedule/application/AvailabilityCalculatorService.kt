package com.booking.schedule.application

import com.booking.booking.domain.BookingStatus
import com.booking.booking.infrastructure.BookingRepository
import com.booking.catalog.domain.Master
import com.booking.catalog.domain.Service
import com.booking.catalog.infrastructure.MasterRepository
import com.booking.catalog.infrastructure.ServiceRepository
import com.booking.schedule.infrastructure.MasterScheduleRepository
import com.booking.schedule.presentation.AvailableSlotDto
import com.booking.shared.config.AppProperties
import com.booking.shared.exception.ResourceNotFoundException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Component
class AvailabilityCalculatorService(
    private val serviceRepository: ServiceRepository,
    private val masterRepository: MasterRepository,
    private val masterScheduleRepository: MasterScheduleRepository,
    private val bookingRepository: BookingRepository,
    private val clock: Clock,
    private val appProperties: AppProperties
) {

    private val activeStatuses = setOf(BookingStatus.PENDING, BookingStatus.CONFIRMED)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    @Transactional(readOnly = true)
    fun calculateAvailableSlots(
        serviceId: UUID,
        date: LocalDate,
        masterId: UUID? = null
    ): List<AvailableSlotDto> {
        val service = serviceRepository.findByIdAndIsActiveTrue(serviceId)
            .orElseThrow { ResourceNotFoundException("Активная услуга с ID $serviceId не найдена.") }

        val masters: List<Master> = if (masterId != null) {
            val master = masterRepository.findByIdAndIsActiveTrue(masterId)
                .orElseThrow { ResourceNotFoundException("Специалист с ID $masterId не найден.") }
            if (master.services.none { it.id == service.id }) {
                emptyList()
            } else {
                listOf(master)
            }
        } else {
            masterRepository.findAllActiveByServiceId(serviceId)
        }

        if (masters.isEmpty()) {
            return emptyList()
        }

        val venueZone = ZoneId.of(appProperties.timezone)
        val now = clock.instant()
        val dayOfWeek = date.dayOfWeek.value
        val duration = Duration.ofMinutes(service.durationMinutes.toLong())

        val dayStartUtc = date.atStartOfDay(venueZone).toInstant()
        val dayEndUtc = date.plusDays(1).atStartOfDay(venueZone).toInstant()

        val resultSlots = mutableListOf<AvailableSlotDto>()

        for (master in masters) {
            val shifts = masterScheduleRepository.findAllByMasterIdAndDayOfWeek(master.id, dayOfWeek)
            if (shifts.isEmpty()) continue

            val activeBookings = bookingRepository.findActiveBookingsForDay(
                masterId = master.id,
                dayStart = dayStartUtc,
                dayEnd = dayEndUtc,
                activeStatuses = activeStatuses
            )

            for (shift in shifts) {
                var currentSlotStart: LocalTime = shift.startTime

                while (!currentSlotStart.plus(duration).isAfter(shift.endTime)) {
                    val currentSlotEnd = currentSlotStart.plus(duration)

                    val startVenue = ZonedDateTime.of(date, currentSlotStart, venueZone)
                    val endVenue = ZonedDateTime.of(date, currentSlotEnd, venueZone)

                    val startUtc = startVenue.toInstant()
                    val endUtc = endVenue.toInstant()

                    // Rule 1: Exclude slots in the past (BR-006, AC-EDGE-008)
                    val isPast = !startUtc.isAfter(now)

                    // Rule 2: Exclude slots overlapping active bookings (BR-001, BR-002)
                    // Overlap: slotStart < bookingEnd && slotEnd > bookingStart
                    val hasCollision = activeBookings.any { booking ->
                        startUtc.isBefore(booking.endTime) && endUtc.isAfter(booking.startTime)
                    }

                    if (!isPast && !hasCollision) {
                        resultSlots.add(
                            AvailableSlotDto(
                                startTime = startUtc,
                                endTime = endUtc,
                                formattedTime = timeFormatter.format(startVenue),
                                masterId = master.id,
                                masterName = master.name,
                                serviceId = service.id
                            )
                        )
                    }

                    currentSlotStart = currentSlotEnd
                }
            }
        }

        return resultSlots.sortedWith(compareBy({ it.startTime }, { it.masterName }))
    }
}
