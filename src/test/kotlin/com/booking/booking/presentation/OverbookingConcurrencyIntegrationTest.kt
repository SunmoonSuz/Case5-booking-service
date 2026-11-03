package com.booking.booking.presentation

import com.booking.AbstractIntegrationTest
import com.booking.booking.domain.BookingStatus
import com.booking.booking.infrastructure.BookingRepository
import com.booking.catalog.domain.Master
import com.booking.catalog.domain.Service
import com.booking.catalog.infrastructure.MasterRepository
import com.booking.catalog.infrastructure.ServiceRepository
import com.booking.identity.domain.Role
import com.booking.identity.domain.User
import com.booking.identity.infrastructure.UserRepository
import com.booking.schedule.domain.MasterSchedule
import com.booking.schedule.infrastructure.MasterScheduleRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@AutoConfigureMockMvc
class OverbookingConcurrencyIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var serviceRepository: ServiceRepository

    @Autowired
    private lateinit var masterRepository: MasterRepository

    @Autowired
    private lateinit var masterScheduleRepository: MasterScheduleRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    private val venueZone = ZoneId.of("Europe/Moscow")

    @BeforeEach
    fun cleanDatabase() {
        bookingRepository.deleteAll()
        masterScheduleRepository.deleteAll()
        masterRepository.deleteAll()
        serviceRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `AC-003 and NFR-001 - 20 concurrent requests for the exact same slot result in exactly 1 Created, 19 Conflicts and 1 DB row`() {
        val concurrencyLevel = 20

        // 1. Setup Service (60 min)
        val service = serviceRepository.save(
            Service(
                name = "Стрижка модельная",
                durationMinutes = 60,
                price = BigDecimal("1500.00"),
                isActive = true
            )
        )

        // 2. Setup Master with Service
        val master = masterRepository.save(
            Master(
                name = "Мастер Виктория",
                isActive = true,
                services = mutableSetOf(service)
            )
        )

        // 3. Target date: future Wednesday (dayOfWeek = 3)
        val targetDate = LocalDate.now().plusWeeks(1)
            .with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.WEDNESDAY))

        // Schedule: 10:00 - 18:00
        masterScheduleRepository.save(
            MasterSchedule(
                master = master,
                dayOfWeek = 3,
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(18, 0)
            )
        )

        // Slot: 14:00 - 15:00 Moscow time
        val slotStartUtc = ZonedDateTime.of(targetDate, LocalTime.of(14, 0), venueZone).toInstant()

        // 4. Create 20 unique clients
        val clientEmails = (1..concurrencyLevel).map { i -> "client$i@example.com" }
        for (email in clientEmails) {
            userRepository.save(
                User(
                    email = email,
                    passwordHash = "hash$email",
                    role = Role.CLIENT,
                    name = "Клиент $email",
                    phone = "+79990000000"
                )
            )
        }

        // 5. Concurrency harness with CyclicBarrier
        val barrier = CyclicBarrier(concurrencyLevel)
        val completionLatch = CountDownLatch(concurrencyLevel)
        val executor = Executors.newFixedThreadPool(concurrencyLevel)

        data class Result(val statusCode: Int, val body: String)
        val results = ConcurrentLinkedQueue<Result>()

        val requestBody = objectMapper.writeValueAsString(
            CreateBookingRequest(
                serviceId = service.id,
                masterId = master.id,
                startTime = slotStartUtc
            )
        )

        for (email in clientEmails) {
            executor.submit {
                try {
                    // Synchronize all threads so they fire requests almost at the exact same microsecond
                    barrier.await(10, TimeUnit.SECONDS)

                    val mvcResult = mockMvc.perform(
                        post("/api/v1/bookings")
                            .with(user(email).roles("CLIENT"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                    ).andReturn()

                    results.add(Result(mvcResult.response.status, mvcResult.response.contentAsString))
                } catch (e: Exception) {
                    results.add(Result(500, e.message ?: "Exception"))
                } finally {
                    completionLatch.countDown()
                }
            }
        }

        val completed = completionLatch.await(30, TimeUnit.SECONDS)
        executor.shutdown()
        assertThat(completed).isTrue()

        // 6. Verify NFR-001 & AC-003 criteria
        val statuses = results.map { it.statusCode }
        val createdCount = statuses.count { it == 201 }
        val conflictCount = statuses.count { it == 409 }

        assertThat(createdCount).`as`("Exactly 1 request must succeed with 201 Created").isEqualTo(1)
        assertThat(conflictCount).`as`("Exactly 19 requests must fail with 409 Conflict").isEqualTo(19)

        // Verify conflict response bodies contain SLOT_ALREADY_BOOKED
        val conflictResponses = results.filter { it.statusCode == 409 }
        for (conflict in conflictResponses) {
            assertThat(conflict.body).contains("SLOT_ALREADY_BOOKED")
        }

        // 7. Verify DB invariant: exactly 1 active booking in DB for this master and time
        val activeBookings = bookingRepository.findAll()
            .filter { it.master.id == master.id && it.status in setOf(BookingStatus.PENDING, BookingStatus.CONFIRMED) }

        assertThat(activeBookings).`as`("Exactly 1 active booking must exist in the database").hasSize(1)
        val singleBooking = activeBookings.first()
        assertThat(singleBooking.startTime).isEqualTo(slotStartUtc)
        assertThat(singleBooking.status).isEqualTo(BookingStatus.PENDING)
    }
}
