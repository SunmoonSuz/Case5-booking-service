package com.booking.schedule.application

import com.booking.booking.domain.Booking
import com.booking.booking.domain.BookingStatus
import com.booking.booking.infrastructure.BookingRepository
import com.booking.catalog.domain.Master
import com.booking.catalog.domain.Service
import com.booking.catalog.infrastructure.MasterRepository
import com.booking.catalog.infrastructure.ServiceRepository
import com.booking.identity.domain.Role
import com.booking.identity.domain.User
import com.booking.schedule.domain.MasterSchedule
import com.booking.schedule.infrastructure.MasterScheduleRepository
import com.booking.shared.config.AppProperties
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Optional
import java.util.UUID

class AvailabilityCalculatorServiceTest {

    private val serviceRepository: ServiceRepository = mockk()
    private val masterRepository: MasterRepository = mockk()
    private val masterScheduleRepository: MasterScheduleRepository = mockk()
    private val bookingRepository: BookingRepository = mockk()

    private val venueZone = ZoneId.of("Europe/Moscow")
    private val appProperties = AppProperties(timezone = "Europe/Moscow")

    // Target date: Tuesday, 2026-11-10 (dayOfWeek = 2)
    private val targetDate = LocalDate.of(2026, 11, 10)

    // Clock set to 08:00 Moscow time on 2026-11-10 (05:00 UTC)
    private val fixedClock = Clock.fixed(
        ZonedDateTime.of(targetDate, LocalTime.of(8, 0), venueZone).toInstant(),
        venueZone
    )

    private lateinit var availabilityService: AvailabilityCalculatorService

    private val service60 = Service(
        id = UUID.randomUUID(),
        name = "Стрижка",
        durationMinutes = 60,
        price = BigDecimal("1500.00"),
        isActive = true
    )

    private val service30 = Service(
        id = UUID.randomUUID(),
        name = "Экспресс-укладка",
        durationMinutes = 30,
        price = BigDecimal("800.00"),
        isActive = true
    )

    private val master = Master(
        id = UUID.randomUUID(),
        name = "Мария",
        isActive = true,
        services = mutableSetOf(service60, service30)
    )

    private val dummyClient = User(
        email = "test@example.com",
        passwordHash = "hash",
        role = Role.CLIENT,
        name = "Клиент",
        phone = "+79991234567"
    )

    @BeforeEach
    fun setUp() {
        availabilityService = AvailabilityCalculatorService(
            serviceRepository = serviceRepository,
            masterRepository = masterRepository,
            masterScheduleRepository = masterScheduleRepository,
            bookingRepository = bookingRepository,
            clock = fixedClock,
            appProperties = appProperties
        )

        every { serviceRepository.findByIdAndIsActiveTrue(service60.id) } returns Optional.of(service60)
        every { serviceRepository.findByIdAndIsActiveTrue(service30.id) } returns Optional.of(service30)
        every { masterRepository.findByIdAndIsActiveTrue(master.id) } returns Optional.of(master)
        every { masterRepository.findAllActiveByServiceId(service60.id) } returns listOf(master)
    }

    @Test
    fun `generates discrete 60-min slots excluding lunch break`() {
        // Schedule: 09:00 - 13:00 and 14:00 - 18:00 (Lunch 13:00 - 14:00)
        val shifts = listOf(
            MasterSchedule(master = master, dayOfWeek = 2, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(13, 0)),
            MasterSchedule(master = master, dayOfWeek = 2, startTime = LocalTime.of(14, 0), endTime = LocalTime.of(18, 0))
        )

        every { masterScheduleRepository.findAllByMasterIdAndDayOfWeek(master.id, 2) } returns shifts
        every { bookingRepository.findActiveBookingsForDay(master.id, any(), any(), any()) } returns emptyList()

        val slots = availabilityService.calculateAvailableSlots(service60.id, targetDate, master.id)

        // Expected slots: 09:00, 10:00, 11:00, 12:00, 14:00, 15:00, 16:00, 17:00 (8 slots total, 13:00 excluded)
        assertThat(slots).hasSize(8)
        assertThat(slots.map { it.formattedTime }).containsExactly(
            "09:00", "10:00", "11:00", "12:00",
            "14:00", "15:00", "16:00", "17:00"
        )

        // Verify AC-010: 14:00 MSK (UTC+3) corresponds to 11:00 UTC
        val slot14 = slots.first { it.formattedTime == "14:00" }
        assertThat(slot14.startTime).isEqualTo(Instant.parse("2026-11-10T11:00:00Z"))
        assertThat(slot14.endTime).isEqualTo(Instant.parse("2026-11-10T12:00:00Z"))
    }

    @Test
    fun `generates discrete 30-min slots correctly`() {
        // Schedule: 10:00 - 12:00 -> 4 slots
        val shifts = listOf(
            MasterSchedule(master = master, dayOfWeek = 2, startTime = LocalTime.of(10, 0), endTime = LocalTime.of(12, 0))
        )

        every { masterScheduleRepository.findAllByMasterIdAndDayOfWeek(master.id, 2) } returns shifts
        every { bookingRepository.findActiveBookingsForDay(master.id, any(), any(), any()) } returns emptyList()

        val slots = availabilityService.calculateAvailableSlots(service30.id, targetDate, master.id)

        assertThat(slots).hasSize(4)
        assertThat(slots.map { it.formattedTime }).containsExactly("10:00", "10:30", "11:00", "11:30")
    }

    @Test
    fun `excludes slots in the past (BR-006, AC-EDGE-008)`() {
        // Now is 11:30 Moscow time
        val pastClock = Clock.fixed(
            ZonedDateTime.of(targetDate, LocalTime.of(11, 30), venueZone).toInstant(),
            venueZone
        )

        val serviceWithPastClock = AvailabilityCalculatorService(
            serviceRepository = serviceRepository,
            masterRepository = masterRepository,
            masterScheduleRepository = masterScheduleRepository,
            bookingRepository = bookingRepository,
            clock = pastClock,
            appProperties = appProperties
        )

        val shifts = listOf(
            MasterSchedule(master = master, dayOfWeek = 2, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(14, 0))
        )
        every { masterScheduleRepository.findAllByMasterIdAndDayOfWeek(master.id, 2) } returns shifts
        every { bookingRepository.findActiveBookingsForDay(master.id, any(), any(), any()) } returns emptyList()

        val slots = serviceWithPastClock.calculateAvailableSlots(service60.id, targetDate, master.id)

        // 09:00, 10:00, 11:00 are <= 11:30, so they are excluded! Available: 12:00, 13:00
        assertThat(slots.map { it.formattedTime }).containsExactly("12:00", "13:00")
    }

    @Test
    fun `excludes booked slots and allows adjacent slots (BR-001, BR-002)`() {
        val shifts = listOf(
            MasterSchedule(master = master, dayOfWeek = 2, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(13, 0))
        )
        every { masterScheduleRepository.findAllByMasterIdAndDayOfWeek(master.id, 2) } returns shifts

        // Active booking from 10:00 to 11:00 Moscow time
        val bookingStart = ZonedDateTime.of(targetDate, LocalTime.of(10, 0), venueZone).toInstant()
        val bookingEnd = ZonedDateTime.of(targetDate, LocalTime.of(11, 0), venueZone).toInstant()
        val activeBooking = Booking(
            client = dummyClient,
            master = master,
            service = service60,
            serviceNameSnapshot = service60.name,
            servicePriceSnapshot = service60.price,
            startTime = bookingStart,
            endTime = bookingEnd,
            status = BookingStatus.CONFIRMED
        )

        every { bookingRepository.findActiveBookingsForDay(master.id, any(), any(), any()) } returns listOf(activeBooking)

        val slots = availabilityService.calculateAvailableSlots(service60.id, targetDate, master.id)

        // 10:00 is booked, adjacent 09:00, 11:00, 12:00 are available
        assertThat(slots.map { it.formattedTime }).containsExactly("09:00", "11:00", "12:00")
    }
}
