package com.booking.booking.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BookingStateMachineTest {

    @Test
    fun `PENDING can transition to CONFIRMED or REJECTED by admin, and CANCELLED by anyone`() {
        assertThat(BookingStatus.PENDING.canTransitionTo(BookingStatus.CONFIRMED, isAdmin = true)).isTrue()
        assertThat(BookingStatus.PENDING.canTransitionTo(BookingStatus.REJECTED, isAdmin = true)).isTrue()
        assertThat(BookingStatus.PENDING.canTransitionTo(BookingStatus.CANCELLED, isAdmin = true)).isTrue()
        assertThat(BookingStatus.PENDING.canTransitionTo(BookingStatus.CANCELLED, isAdmin = false)).isTrue()

        // Client cannot confirm or reject
        assertThat(BookingStatus.PENDING.canTransitionTo(BookingStatus.CONFIRMED, isAdmin = false)).isFalse()
        assertThat(BookingStatus.PENDING.canTransitionTo(BookingStatus.REJECTED, isAdmin = false)).isFalse()

        // PENDING cannot directly jump to COMPLETED
        assertThat(BookingStatus.PENDING.canTransitionTo(BookingStatus.COMPLETED, isAdmin = true)).isFalse()
    }

    @Test
    fun `CONFIRMED can transition to CANCELLED by anyone, and COMPLETED by admin`() {
        assertThat(BookingStatus.CONFIRMED.canTransitionTo(BookingStatus.CANCELLED, isAdmin = true)).isTrue()
        assertThat(BookingStatus.CONFIRMED.canTransitionTo(BookingStatus.CANCELLED, isAdmin = false)).isTrue()
        assertThat(BookingStatus.CONFIRMED.canTransitionTo(BookingStatus.COMPLETED, isAdmin = true)).isTrue()

        // Client cannot complete
        assertThat(BookingStatus.CONFIRMED.canTransitionTo(BookingStatus.COMPLETED, isAdmin = false)).isFalse()

        // CONFIRMED cannot be REJECTED
        assertThat(BookingStatus.CONFIRMED.canTransitionTo(BookingStatus.REJECTED, isAdmin = true)).isFalse()
    }

    @Test
    fun `terminal states REJECTED, CANCELLED, COMPLETED cannot transition to any state`() {
        val terminalStates = listOf(BookingStatus.REJECTED, BookingStatus.CANCELLED, BookingStatus.COMPLETED)
        val allStates = BookingStatus.entries

        for (terminal in terminalStates) {
            for (target in allStates) {
                assertThat(terminal.canTransitionTo(target, isAdmin = true))
                    .`as`("From $terminal to $target (admin)")
                    .isFalse()
                assertThat(terminal.canTransitionTo(target, isAdmin = false))
                    .`as`("From $terminal to $target (client)")
                    .isFalse()
            }
        }
    }
}
