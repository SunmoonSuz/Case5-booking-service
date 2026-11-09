package com.booking.shared.config

import com.booking.booking.domain.Booking
import com.booking.booking.infrastructure.BookingRepository
import com.booking.catalog.domain.Master
import com.booking.catalog.domain.Service
import com.booking.catalog.infrastructure.MasterRepository
import com.booking.catalog.infrastructure.ServiceRepository
import com.booking.identity.domain.User
import com.booking.identity.infrastructure.UserRepository
import com.booking.schedule.domain.MasterSchedule
import com.booking.schedule.infrastructure.MasterScheduleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

class DemoDataBootstrapRunnerTest {

    private val userRepository: UserRepository = mockk()
    private val serviceRepository: ServiceRepository = mockk()
    private val masterRepository: MasterRepository = mockk()
    private val masterScheduleRepository: MasterScheduleRepository = mockk()
    private val bookingRepository: BookingRepository = mockk()
    private val passwordEncoder: PasswordEncoder = mockk()
    private val appProperties = AppProperties()

    private val runner = DemoDataBootstrapRunner(
        userRepository = userRepository,
        serviceRepository = serviceRepository,
        masterRepository = masterRepository,
        masterScheduleRepository = masterScheduleRepository,
        bookingRepository = bookingRepository,
        passwordEncoder = passwordEncoder,
        appProperties = appProperties
    )

    @Test
    fun `seeds demo catalog, masters, schedules, test clients and bookings on empty database`() {
        every { serviceRepository.count() } returns 0L
        every { masterRepository.count() } returns 0L
        every { userRepository.findByEmailIgnoreCase(any()) } returns Optional.empty()
        every { passwordEncoder.encode(any()) } returns "hashed_demo_pass"
        every { userRepository.save(any<User>()) } answers { firstArg() }
        every { serviceRepository.save(any<Service>()) } answers { firstArg() }
        every { masterRepository.save(any<Master>()) } answers { firstArg() }
        every { masterScheduleRepository.save(any<MasterSchedule>()) } answers { firstArg() }
        every { bookingRepository.save(any<Booking>()) } answers { firstArg() }

        runner.run(null)

        verify(exactly = 2) { userRepository.save(any<User>()) }
        verify(exactly = 5) { serviceRepository.save(any<Service>()) }
        verify(exactly = 3) { masterRepository.save(any<Master>()) }
        verify(exactly = 42) { masterScheduleRepository.save(any<MasterSchedule>()) }
        verify(exactly = 4) { bookingRepository.save(any<Booking>()) }
    }

    @Test
    fun `skips seeding when catalog already contains services`() {
        every { serviceRepository.count() } returns 5L

        runner.run(null)

        verify(exactly = 0) { serviceRepository.save(any<Service>()) }
        verify(exactly = 0) { masterRepository.save(any<Master>()) }
        verify(exactly = 0) { bookingRepository.save(any<Booking>()) }
    }
}
