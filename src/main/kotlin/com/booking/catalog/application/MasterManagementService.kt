package com.booking.catalog.application

import com.booking.booking.domain.BookingStatus
import com.booking.booking.infrastructure.BookingRepository
import com.booking.catalog.domain.Master
import com.booking.catalog.infrastructure.MasterRepository
import com.booking.catalog.infrastructure.ServiceRepository
import com.booking.catalog.presentation.CreateMasterRequest
import com.booking.catalog.presentation.MasterDto
import com.booking.catalog.presentation.UpdateMasterRequest
import com.booking.schedule.domain.MasterSchedule
import com.booking.schedule.infrastructure.MasterScheduleRepository
import com.booking.schedule.presentation.ScheduleIntervalDto
import com.booking.schedule.presentation.SetScheduleRequest
import com.booking.shared.config.AppProperties
import com.booking.shared.exception.InvalidScheduleIntervalException
import com.booking.shared.exception.MasterHasActiveBookingsException
import com.booking.shared.exception.ResourceNotFoundException
import com.booking.shared.exception.ScheduleConflictsWithActiveBookingsException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

@Component
class MasterManagementService(
    private val masterRepository: MasterRepository,
    private val serviceRepository: ServiceRepository,
    private val masterScheduleRepository: MasterScheduleRepository,
    private val bookingRepository: BookingRepository,
    private val clock: Clock,
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(MasterManagementService::class.java)
    private val activeStatuses = setOf(BookingStatus.PENDING, BookingStatus.CONFIRMED)

    @Transactional(readOnly = true)
    fun getActiveMasters(serviceId: UUID? = null): List<MasterDto> {
        return if (serviceId != null) {
            masterRepository.findAllActiveByServiceId(serviceId).map { MasterDto.from(it) }
        } else {
            masterRepository.findAllByIsActiveTrue().map { MasterDto.from(it) }
        }
    }

    @Transactional(readOnly = true)
    fun getAllMasters(): List<MasterDto> {
        return masterRepository.findAll().map { MasterDto.from(it) }
    }

    @Transactional(readOnly = true)
    fun getMasterById(id: UUID): MasterDto {
        val master = masterRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Специалист с ID $id не найден.") }
        return MasterDto.from(master)
    }

    @Transactional
    fun createMaster(request: CreateMasterRequest): MasterDto {
        val master = Master(
            name = request.name.trim(),
            description = request.description?.trim(),
            isActive = true
        )
        val saved = masterRepository.save(master)
        log.info("Created master: id={}, name={}", saved.id, saved.name)
        return MasterDto.from(saved)
    }

    @Transactional
    fun updateMaster(id: UUID, request: UpdateMasterRequest): MasterDto {
        val existing = masterRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Специалист с ID $id не найден.") }

        // Deactivation guard: check active bookings (SCN-005 E3, ADR-012)
        if (request.isActive == false && existing.isActive) {
            val futureBookings = bookingRepository.countFutureActiveByMasterId(
                masterId = id,
                now = clock.instant(),
                activeStatuses = activeStatuses
            )
            if (futureBookings > 0) {
                log.warn("Cannot deactivate master {}: has {} upcoming active bookings", id, futureBookings)
                throw MasterHasActiveBookingsException(
                    "Невозможно деактивировать специалиста при наличии предстоящих активных записей."
                )
            }
        }

        existing.name = request.name.trim()
        existing.description = request.description?.trim()
        request.isActive?.let { existing.isActive = it }

        val updated = masterRepository.save(existing)
        log.info("Updated master: id={}, name={}", updated.id, updated.name)
        return MasterDto.from(updated)
    }

    @Transactional
    fun deleteMaster(id: UUID) {
        val existing = masterRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Специалист с ID $id не найден.") }

        val futureBookings = bookingRepository.countFutureActiveByMasterId(
            masterId = id,
            now = clock.instant(),
            activeStatuses = activeStatuses
        )
        if (futureBookings > 0) {
            log.warn("Cannot delete/deactivate master {}: has {} upcoming active bookings", id, futureBookings)
            throw MasterHasActiveBookingsException(
                "Невозможно деактивировать специалиста при наличии предстоящих активных записей."
            )
        }

        existing.isActive = false
        masterRepository.save(existing)
        log.info("Soft-deleted master: id={}", id)
    }

    @Transactional
    fun assignServices(masterId: UUID, serviceIds: List<UUID>): MasterDto {
        val master = masterRepository.findById(masterId)
            .orElseThrow { ResourceNotFoundException("Специалист с ID $masterId не найден.") }

        val services = serviceRepository.findAllById(serviceIds)
        master.services = services.toMutableSet()

        val updated = masterRepository.save(master)
        log.info("Assigned {} services to master {}", services.size, masterId)
        return MasterDto.from(updated)
    }

    @Transactional(readOnly = true)
    fun getMasterSchedule(masterId: UUID): List<ScheduleIntervalDto> {
        return masterScheduleRepository.findAllByMasterId(masterId)
            .sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
            .map { ScheduleIntervalDto.from(it) }
    }

    @Transactional
    fun setMasterSchedule(masterId: UUID, request: SetScheduleRequest): List<ScheduleIntervalDto> {
        val master = masterRepository.findById(masterId)
            .orElseThrow { ResourceNotFoundException("Специалист с ID $masterId не найден.") }

        // 1. Validate interval boundaries (AC-EDGE-009)
        for (interval in request.intervals) {
            if (!interval.startTime.isBefore(interval.endTime)) {
                throw InvalidScheduleIntervalException(
                    "Время окончания интервала (${interval.endTime}) должно быть строго позже времени начала (${interval.startTime})."
                )
            }
        }

        // 2. Validate interval collisions for the same day of week
        val intervalsByDay = request.intervals.groupBy { it.dayOfWeek }
        for ((_, intervals) in intervalsByDay) {
            val sorted = intervals.sortedBy { it.startTime }
            for (i in 0 until sorted.size - 1) {
                if (sorted[i].endTime.isAfter(sorted[i + 1].startTime)) {
                    throw InvalidScheduleIntervalException("Рабочие интервалы в рамках одного дня не должны пересекаться.")
                }
            }
        }

        // 3. Validate future active bookings fit within new schedule (ADR-010)
        val venueZone = ZoneId.of(appProperties.timezone)
        val now = clock.instant()
        val nextMonth = now.plus(java.time.Duration.ofDays(60))
        val activeBookings = bookingRepository.findActiveBookingsInInterval(
            masterId = masterId,
            intervalStart = now,
            intervalEnd = nextMonth,
            activeStatuses = activeStatuses
        )

        for (booking in activeBookings) {
            val bookingStartVenue = ZonedDateTime.ofInstant(booking.startTime, venueZone)
            val bookingEndVenue = ZonedDateTime.ofInstant(booking.endTime, venueZone)
            val dayOfWeek = bookingStartVenue.dayOfWeek.value
            val bookingStartTime = bookingStartVenue.toLocalTime()
            val bookingEndTime = bookingEndVenue.toLocalTime()

            val dayIntervals = intervalsByDay[dayOfWeek] ?: emptyList()
            val fitsInAnyShift = dayIntervals.any { shift ->
                !bookingStartTime.isBefore(shift.startTime) && !bookingEndTime.isAfter(shift.endTime)
            }

            if (!fitsInAnyShift) {
                log.warn("Schedule conflict for master {}: existing booking {} falls outside new schedule", masterId, booking.id)
                throw ScheduleConflictsWithActiveBookingsException(
                    "Новое расписание конфликтует с существующей активной записью на ${bookingStartVenue.toLocalDate()} в $bookingStartTime."
                )
            }
        }

        // Replace schedules
        masterScheduleRepository.deleteAllByMasterId(masterId)

        val newSchedules = request.intervals.map { dto ->
            MasterSchedule(
                master = master,
                dayOfWeek = dto.dayOfWeek,
                startTime = dto.startTime,
                endTime = dto.endTime
            )
        }

        val saved = masterScheduleRepository.saveAll(newSchedules)
        log.info("Updated schedule for master {}: saved {} intervals", masterId, saved.size)

        return saved.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
            .map { ScheduleIntervalDto.from(it) }
    }
}
