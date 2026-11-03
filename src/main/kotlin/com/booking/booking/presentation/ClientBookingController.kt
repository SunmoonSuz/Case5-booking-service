package com.booking.booking.presentation

import com.booking.booking.application.BookingService
import com.booking.booking.application.ClientBookingService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class ClientBookingController(
    private val bookingService: BookingService,
    private val clientBookingService: ClientBookingService
) {

    @PostMapping("/bookings")
    fun createBooking(
        @Valid @RequestBody request: CreateBookingRequest,
        authentication: Authentication
    ): ResponseEntity<BookingResponse> {
        val created = bookingService.createBooking(authentication.name, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @GetMapping("/client/bookings")
    fun getClientBookings(authentication: Authentication): ResponseEntity<ClientBookingsResponse> {
        val bookings = clientBookingService.getClientBookings(authentication.name)
        return ResponseEntity.ok(bookings)
    }

    @PostMapping("/client/bookings/{id}/cancel")
    fun cancelBooking(
        @PathVariable id: UUID,
        authentication: Authentication
    ): ResponseEntity<BookingResponse> {
        val cancelled = clientBookingService.cancelBooking(authentication.name, id)
        return ResponseEntity.ok(cancelled)
    }
}
