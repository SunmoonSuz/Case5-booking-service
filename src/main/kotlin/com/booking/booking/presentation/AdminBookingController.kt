package com.booking.booking.presentation

import com.booking.booking.application.AdminBookingModerationService
import com.booking.booking.domain.BookingStatus
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/bookings")
class AdminBookingController(
    private val moderationService: AdminBookingModerationService
) {

    @GetMapping
    fun getBookings(
        @RequestParam(required = false) status: BookingStatus? = null,
        @RequestParam(required = false) masterId: UUID? = null,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate? = null
    ): ResponseEntity<List<BookingResponse>> {
        val bookings = moderationService.getBookings(status, masterId, date)
        return ResponseEntity.ok(bookings)
    }

    @PostMapping("/{id}/confirm")
    fun confirmBooking(@PathVariable id: UUID): ResponseEntity<BookingResponse> {
        val confirmed = moderationService.confirmBooking(id)
        return ResponseEntity.ok(confirmed)
    }

    @PostMapping("/{id}/reject")
    fun rejectBooking(@PathVariable id: UUID): ResponseEntity<BookingResponse> {
        val rejected = moderationService.rejectBooking(id)
        return ResponseEntity.ok(rejected)
    }

    @PostMapping("/{id}/cancel")
    fun cancelBooking(@PathVariable id: UUID): ResponseEntity<BookingResponse> {
        val cancelled = moderationService.cancelBooking(id)
        return ResponseEntity.ok(cancelled)
    }

    @PostMapping("/{id}/complete")
    fun completeBooking(@PathVariable id: UUID): ResponseEntity<BookingResponse> {
        val completed = moderationService.completeBooking(id)
        return ResponseEntity.ok(completed)
    }
}
