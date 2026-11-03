package com.booking.booking.domain

enum class BookingStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    CANCELLED,
    COMPLETED;

    fun canTransitionTo(target: BookingStatus, isAdmin: Boolean = false): Boolean {
        return when (this) {
            PENDING -> when (target) {
                CONFIRMED -> isAdmin
                REJECTED -> isAdmin
                CANCELLED -> true
                else -> false
            }
            CONFIRMED -> when (target) {
                CANCELLED -> true
                COMPLETED -> isAdmin
                else -> false
            }
            REJECTED, CANCELLED, COMPLETED -> false
        }
    }
}
