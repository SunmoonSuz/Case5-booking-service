package com.booking.schedule.presentation

import com.booking.schedule.application.AvailabilityCalculatorService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1/availability")
class AvailabilityController(
    private val availabilityCalculatorService: AvailabilityCalculatorService
) {

    @GetMapping
    fun getAvailability(
        @RequestParam serviceId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestParam(required = false) masterId: UUID? = null
    ): ResponseEntity<List<AvailableSlotDto>> {
        val slots = availabilityCalculatorService.calculateAvailableSlots(
            serviceId = serviceId,
            date = date,
            masterId = masterId
        )
        return ResponseEntity.ok(slots)
    }
}
