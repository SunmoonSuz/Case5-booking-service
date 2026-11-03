package com.booking.catalog.application

import com.booking.booking.domain.BookingStatus
import com.booking.booking.infrastructure.BookingRepository
import com.booking.catalog.domain.Service
import com.booking.catalog.infrastructure.ServiceRepository
import com.booking.catalog.presentation.CreateServiceRequest
import com.booking.catalog.presentation.ServiceDto
import com.booking.catalog.presentation.UpdateServiceRequest
import com.booking.shared.exception.CannotModifyDurationWithActiveBookingsException
import com.booking.shared.exception.DomainException
import com.booking.shared.exception.ResourceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

@Component
class ServiceCatalogService(
    private val serviceRepository: ServiceRepository,
    private val bookingRepository: BookingRepository,
    private val clock: Clock
) {

    private val log = LoggerFactory.getLogger(ServiceCatalogService::class.java)
    private val activeStatuses = setOf(BookingStatus.PENDING, BookingStatus.CONFIRMED)

    @Transactional(readOnly = true)
    fun getActiveServices(): List<ServiceDto> {
        return serviceRepository.findAllByIsActiveTrue().map { ServiceDto.from(it) }
    }

    @Transactional(readOnly = true)
    fun getAllServices(): List<ServiceDto> {
        return serviceRepository.findAll().map { ServiceDto.from(it) }
    }

    @Transactional(readOnly = true)
    fun getServiceById(id: UUID): ServiceDto {
        val service = serviceRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Услуга с ID $id не найдена.") }
        return ServiceDto.from(service)
    }

    @Transactional
    fun createService(request: CreateServiceRequest): ServiceDto {
        validateDuration(request.durationMinutes)

        val service = Service(
            name = request.name.trim(),
            description = request.description?.trim(),
            durationMinutes = request.durationMinutes,
            price = request.price,
            isActive = true
        )

        val saved = serviceRepository.save(service)
        log.info("Created service: id={}, name={}", saved.id, saved.name)
        return ServiceDto.from(saved)
    }

    @Transactional
    fun updateService(id: UUID, request: UpdateServiceRequest): ServiceDto {
        validateDuration(request.durationMinutes)

        val existing = serviceRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Услуга с ID $id не найдена.") }

        // If duration is changed, verify there are no future active bookings (ADR-012)
        if (existing.durationMinutes != request.durationMinutes) {
            val futureBookingsCount = bookingRepository.countFutureActiveByServiceId(
                serviceId = id,
                now = clock.instant(),
                activeStatuses = activeStatuses
            )
            if (futureBookingsCount > 0) {
                log.warn("Cannot change duration for service {}: has {} active bookings", id, futureBookingsCount)
                throw CannotModifyDurationWithActiveBookingsException(
                    "Невозможно изменить длительность услуги при наличии предстоящих активных записей."
                )
            }
        }

        existing.name = request.name.trim()
        existing.description = request.description?.trim()
        existing.durationMinutes = request.durationMinutes
        existing.price = request.price
        request.isActive?.let { existing.isActive = it }

        val updated = serviceRepository.save(existing)
        log.info("Updated service: id={}, name={}", updated.id, updated.name)
        return ServiceDto.from(updated)
    }

    @Transactional
    fun deleteService(id: UUID) {
        val existing = serviceRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Услуга с ID $id не найдена.") }

        // Soft delete (ADR-012)
        existing.isActive = false
        serviceRepository.save(existing)
        log.info("Soft-deleted service: id={}", id)
    }

    private fun validateDuration(durationMinutes: Int) {
        if (durationMinutes !in listOf(30, 60)) {
            throw DomainException(
                errorCode = "VALIDATION_FAILED",
                message = "Длительность услуги должна составлять 30 или 60 минут."
            )
        }
    }
}
