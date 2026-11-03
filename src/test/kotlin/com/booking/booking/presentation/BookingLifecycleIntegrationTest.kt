package com.booking.booking.presentation

import com.booking.AbstractIntegrationTest
import com.booking.booking.domain.Booking
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@AutoConfigureMockMvc
class BookingLifecycleIntegrationTest : AbstractIntegrationTest() {

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

    private lateinit var clientA: User
    private lateinit var clientB: User
    private lateinit var admin: User
    private lateinit var testService: Service
    private lateinit var testMaster: Master
    private lateinit var targetDate: LocalDate

    @BeforeEach
    fun setUp() {
        bookingRepository.deleteAll()
        masterScheduleRepository.deleteAll()
        masterRepository.deleteAll()
        serviceRepository.deleteAll()
        userRepository.deleteAll()

        clientA = userRepository.save(
            User(email = "clienta@example.com", passwordHash = "h", role = Role.CLIENT, name = "Клиент А", phone = "+79991111111")
        )
        clientB = userRepository.save(
            User(email = "clientb@example.com", passwordHash = "h", role = Role.CLIENT, name = "Клиент Б", phone = "+79992222222")
        )
        admin = userRepository.save(
            User(email = "admin@example.com", passwordHash = "h", role = Role.ADMIN, name = "Администратор", phone = "+70000000000")
        )

        testService = serviceRepository.save(
            Service(name = "Стрижка", durationMinutes = 60, price = BigDecimal("1500.00"), isActive = true)
        )
        testMaster = masterRepository.save(
            Master(name = "Ольга", isActive = true, services = mutableSetOf(testService))
        )

        // Target: next Monday (day 1)
        targetDate = LocalDate.now().plusWeeks(1)
            .with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY))

        // Schedule: 09:00 - 18:00
        masterScheduleRepository.save(
            MasterSchedule(
                master = testMaster,
                dayOfWeek = 1,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(18, 0)
            )
        )
    }

    @Test
    fun `AC-002 - Client successfully books a slot and snapshot is preserved`() {
        val slotStart = ZonedDateTime.of(targetDate, LocalTime.of(10, 0), venueZone).toInstant()
        val request = CreateBookingRequest(serviceId = testService.id, masterId = testMaster.id, startTime = slotStart)

        val result = mockMvc.perform(
            post("/api/v1/bookings")
                .with(user(clientA.email).roles("CLIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty)
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.serviceName").value("Стрижка"))
            .andExpect(jsonPath("$.servicePrice").value(1500.00))
            .andExpect(jsonPath("$.masterName").value("Ольга"))
            .andExpect(jsonPath("$.clientName").value("Клиент А"))
            .andReturn()

        val response: BookingResponse = objectMapper.readValue(result.response.contentAsString, BookingResponse::class.java)
        val inDb = bookingRepository.findById(response.id).orElseThrow()
        assertThat(inDb.status).isEqualTo(BookingStatus.PENDING)
        assertThat(inDb.serviceNameSnapshot).isEqualTo("Стрижка")
        assertThat(inDb.servicePriceSnapshot).isEqualByComparingTo(BigDecimal("1500.00"))
    }

    @Test
    fun `AC-EDGE-001 - Adjacent slots 10-11 and 11-12 do not conflict and both succeed`() {
        val slot1Start = ZonedDateTime.of(targetDate, LocalTime.of(10, 0), venueZone).toInstant()
        val slot2Start = ZonedDateTime.of(targetDate, LocalTime.of(11, 0), venueZone).toInstant()

        mockMvc.perform(
            post("/api/v1/bookings")
                .with(user(clientA.email).roles("CLIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateBookingRequest(testService.id, testMaster.id, slot1Start)))
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/v1/bookings")
                .with(user(clientB.email).roles("CLIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateBookingRequest(testService.id, testMaster.id, slot2Start)))
        ).andExpect(status().isCreated)

        assertThat(bookingRepository.count()).isEqualTo(2)
    }

    @Test
    fun `AC-EDGE-002 - Partially overlapping slots 10-11 and 10 30-11 30 conflict with 409`() {
        val service30 = serviceRepository.save(
            Service(name = "Экспресс", durationMinutes = 30, price = BigDecimal("800.00"), isActive = true)
        )
        testMaster.services.add(service30)
        masterRepository.save(testMaster)

        val slot60Start = ZonedDateTime.of(targetDate, LocalTime.of(10, 0), venueZone).toInstant()
        val slot30OverlappingStart = ZonedDateTime.of(targetDate, LocalTime.of(10, 30), venueZone).toInstant()

        // 1. Client A books 10:00 - 11:00
        mockMvc.perform(
            post("/api/v1/bookings")
                .with(user(clientA.email).roles("CLIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateBookingRequest(testService.id, testMaster.id, slot60Start)))
        ).andExpect(status().isCreated)

        // 2. Client B tries to book 10:30 - 11:00 (partially overlaps)
        mockMvc.perform(
            post("/api/v1/bookings")
                .with(user(clientB.email).roles("CLIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateBookingRequest(service30.id, testMaster.id, slot30OverlappingStart)))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").value("SLOT_ALREADY_BOOKED"))
    }

    @Test
    fun `AC-005 - Client cancels own booking before start time and slot becomes available again`() {
        val slotStart = ZonedDateTime.of(targetDate, LocalTime.of(12, 0), venueZone).toInstant()
        val createReq = CreateBookingRequest(testService.id, testMaster.id, slotStart)

        val createResult = mockMvc.perform(
            post("/api/v1/bookings")
                .with(user(clientA.email).roles("CLIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq))
        ).andExpect(status().isCreated).andReturn()

        val bookingId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

        // Cancel
        mockMvc.perform(
            post("/api/v1/client/bookings/$bookingId/cancel")
                .with(user(clientA.email).roles("CLIENT"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELLED"))

        // Now Client B can book the exact same slot!
        mockMvc.perform(
            post("/api/v1/bookings")
                .with(user(clientB.email).roles("CLIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq))
        ).andExpect(status().isCreated)
    }

    @Test
    fun `AC-EDGE-005 - Cancellation of booking in the past is rejected with 400 CANCELLATION_DEADLINE_EXCEEDED`() {
        val pastStart = Instant.now().minus(Duration.ofHours(2))
        val pastEnd = pastStart.plus(Duration.ofHours(1))

        val pastBooking = bookingRepository.save(
            Booking(
                client = clientA,
                master = testMaster,
                service = testService,
                serviceNameSnapshot = testService.name,
                servicePriceSnapshot = testService.price,
                startTime = pastStart,
                endTime = pastEnd,
                status = BookingStatus.PENDING
            )
        )

        mockMvc.perform(
            post("/api/v1/client/bookings/${pastBooking.id}/cancel")
                .with(user(clientA.email).roles("CLIENT"))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("CANCELLATION_DEADLINE_EXCEEDED"))
    }

    @Test
    fun `IDOR prevention - Client B cannot cancel Client A booking`() {
        val slotStart = ZonedDateTime.of(targetDate, LocalTime.of(15, 0), venueZone).toInstant()
        val createReq = CreateBookingRequest(testService.id, testMaster.id, slotStart)

        val createResult = mockMvc.perform(
            post("/api/v1/bookings")
                .with(user(clientA.email).roles("CLIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq))
        ).andExpect(status().isCreated).andReturn()

        val bookingId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

        // Client B tries to cancel Client A's booking
        mockMvc.perform(
            post("/api/v1/client/bookings/$bookingId/cancel")
                .with(user(clientB.email).roles("CLIENT"))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error").value("ACCESS_DENIED"))
    }

    @Test
    fun `AC-EDGE-006 - Booking outside master schedule is rejected with 400 OUTSIDE_WORKING_HOURS`() {
        // Master works 09:00 - 18:00. Request 20:00
        val outsideStart = ZonedDateTime.of(targetDate, LocalTime.of(20, 0), venueZone).toInstant()
        val req = CreateBookingRequest(testService.id, testMaster.id, outsideStart)

        mockMvc.perform(
            post("/api/v1/bookings")
                .with(user(clientA.email).roles("CLIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("OUTSIDE_WORKING_HOURS"))
    }

    @Test
    fun `AC-EDGE-008 - Booking in the past is rejected with 400 BOOKING_IN_PAST`() {
        val pastStart = Instant.now().minus(Duration.ofHours(5))
        val req = CreateBookingRequest(testService.id, testMaster.id, pastStart)

        mockMvc.perform(
            post("/api/v1/bookings")
                .with(user(clientA.email).roles("CLIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("BOOKING_IN_PAST"))
    }

    @Test
    fun `AC-007 and AC-009 - Admin moderation flow confirm, complete, and invalid transition`() {
        val slotStart = ZonedDateTime.of(targetDate, LocalTime.of(16, 0), venueZone).toInstant()
        val req = CreateBookingRequest(testService.id, testMaster.id, slotStart)

        val createResult = mockMvc.perform(
            post("/api/v1/bookings")
                .with(user(clientA.email).roles("CLIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        ).andExpect(status().isCreated).andReturn()

        val bookingId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

        // 1. Admin confirms PENDING -> CONFIRMED
        mockMvc.perform(
            post("/api/v1/admin/bookings/$bookingId/confirm")
                .with(user(admin.email).roles("ADMIN"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CONFIRMED"))

        // 2. Admin completes CONFIRMED -> COMPLETED
        mockMvc.perform(
            post("/api/v1/admin/bookings/$bookingId/complete")
                .with(user(admin.email).roles("ADMIN"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("COMPLETED"))

        // 3. AC-EDGE-007: Try to reject an already COMPLETED booking -> 400 INVALID_STATUS_TRANSITION
        mockMvc.perform(
            post("/api/v1/admin/bookings/$bookingId/reject")
                .with(user(admin.email).roles("ADMIN"))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("INVALID_STATUS_TRANSITION"))
    }

    @Test
    fun `F-501 - Booking spanning across midnight is rejected with 400 OUTSIDE_WORKING_HOURS`() {
        // Master works 09:00 - 18:00. Request 23:30 (ending at 00:30 next day)
        val midnightCrossingStart = ZonedDateTime.of(targetDate, LocalTime.of(23, 30), venueZone).toInstant()
        val req = CreateBookingRequest(testService.id, testMaster.id, midnightCrossingStart)

        mockMvc.perform(
            post("/api/v1/bookings")
                .with(user(clientA.email).roles("CLIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("OUTSIDE_WORKING_HOURS"))
    }

    @Test
    fun `F-502 and REQ-008 - GET client bookings separates active and archive with correct chronological sorting`() {
        val slot1 = ZonedDateTime.of(targetDate, LocalTime.of(10, 0), venueZone).toInstant()
        val slot2 = ZonedDateTime.of(targetDate, LocalTime.of(14, 0), venueZone).toInstant()
        val slot3 = ZonedDateTime.of(targetDate, LocalTime.of(11, 0), venueZone).toInstant()
        val slot4 = ZonedDateTime.of(targetDate, LocalTime.of(15, 0), venueZone).toInstant()

        // 1. Client A: active PENDING at 14:00
        val bActive2 = bookingRepository.save(
            Booking(
                client = clientA, master = testMaster, service = testService,
                serviceNameSnapshot = testService.name, servicePriceSnapshot = testService.price,
                startTime = slot2, endTime = slot2.plus(Duration.ofHours(1)), status = BookingStatus.PENDING
            )
        )
        // 2. Client A: active CONFIRMED at 10:00 (should appear first in active list because 10:00 < 14:00)
        val bActive1 = bookingRepository.save(
            Booking(
                client = clientA, master = testMaster, service = testService,
                serviceNameSnapshot = testService.name, servicePriceSnapshot = testService.price,
                startTime = slot1, endTime = slot1.plus(Duration.ofHours(1)), status = BookingStatus.CONFIRMED
            )
        )
        // 3. Client A: archive CANCELLED at 11:00
        val bArchive1 = bookingRepository.save(
            Booking(
                client = clientA, master = testMaster, service = testService,
                serviceNameSnapshot = testService.name, servicePriceSnapshot = testService.price,
                startTime = slot3, endTime = slot3.plus(Duration.ofHours(1)), status = BookingStatus.CANCELLED
            )
        )
        // 4. Client A: archive COMPLETED at 15:00 (should appear first in archive list because 15:00 > 11:00)
        val bArchive2 = bookingRepository.save(
            Booking(
                client = clientA, master = testMaster, service = testService,
                serviceNameSnapshot = testService.name, servicePriceSnapshot = testService.price,
                startTime = slot4, endTime = slot4.plus(Duration.ofHours(1)), status = BookingStatus.COMPLETED
            )
        )

        mockMvc.perform(
            get("/api/v1/client/bookings")
                .with(user(clientA.email).roles("CLIENT"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.active").isArray)
            .andExpect(jsonPath("$.active.length()").value(2))
            .andExpect(jsonPath("$.archive").isArray)
            .andExpect(jsonPath("$.archive.length()").value(2))
            // Active is sorted ASC: slot1 (10:00) before slot2 (14:00)
            .andExpect(jsonPath("$.active[0].id").value(bActive1.id.toString()))
            .andExpect(jsonPath("$.active[0].status").value("CONFIRMED"))
            .andExpect(jsonPath("$.active[1].id").value(bActive2.id.toString()))
            .andExpect(jsonPath("$.active[1].status").value("PENDING"))
            // Archive is sorted DESC: slot4 (15:00) before slot3 (11:00)
            .andExpect(jsonPath("$.archive[0].id").value(bArchive2.id.toString()))
            .andExpect(jsonPath("$.archive[0].status").value("COMPLETED"))
            .andExpect(jsonPath("$.archive[1].id").value(bArchive1.id.toString()))
            .andExpect(jsonPath("$.archive[1].status").value("CANCELLED"))

        // Verify Client B sees nothing
        mockMvc.perform(
            get("/api/v1/client/bookings")
                .with(user(clientB.email).roles("CLIENT"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.active").isEmpty)
            .andExpect(jsonPath("$.archive").isEmpty)
    }

    @Test
    fun `F-502 and REQ-013 - GET admin bookings with status, master, and date filters`() {
        val master2 = masterRepository.save(
            Master(name = "Мастер Анна", isActive = true, services = mutableSetOf(testService))
        )

        val date1 = targetDate
        val date2 = targetDate.plusDays(1)

        val time1 = ZonedDateTime.of(date1, LocalTime.of(10, 0), venueZone).toInstant()
        val time2 = ZonedDateTime.of(date1, LocalTime.of(14, 0), venueZone).toInstant()
        val time3 = ZonedDateTime.of(date2, LocalTime.of(11, 0), venueZone).toInstant()

        // Booking 1: master1, date1, PENDING
        val b1 = bookingRepository.save(
            Booking(
                client = clientA, master = testMaster, service = testService,
                serviceNameSnapshot = testService.name, servicePriceSnapshot = testService.price,
                startTime = time1, endTime = time1.plus(Duration.ofHours(1)), status = BookingStatus.PENDING
            )
        )
        // Booking 2: master1, date1, CONFIRMED
        val b2 = bookingRepository.save(
            Booking(
                client = clientA, master = testMaster, service = testService,
                serviceNameSnapshot = testService.name, servicePriceSnapshot = testService.price,
                startTime = time2, endTime = time2.plus(Duration.ofHours(1)), status = BookingStatus.CONFIRMED
            )
        )
        // Booking 3: master2, date2, CANCELLED
        val b3 = bookingRepository.save(
            Booking(
                client = clientB, master = master2, service = testService,
                serviceNameSnapshot = testService.name, servicePriceSnapshot = testService.price,
                startTime = time3, endTime = time3.plus(Duration.ofHours(1)), status = BookingStatus.CANCELLED
            )
        )

        // 1. Without filters: returns all 3 bookings sorted by startTime DESC (time3, time2, time1)
        mockMvc.perform(
            get("/api/v1/admin/bookings")
                .with(user(admin.email).roles("ADMIN"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].id").value(b3.id.toString()))
            .andExpect(jsonPath("$[1].id").value(b2.id.toString()))
            .andExpect(jsonPath("$[2].id").value(b1.id.toString()))

        // 2. Filter by status=PENDING -> only b1
        mockMvc.perform(
            get("/api/v1/admin/bookings")
                .with(user(admin.email).roles("ADMIN"))
                .param("status", "PENDING")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(b1.id.toString()))
            .andExpect(jsonPath("$[0].status").value("PENDING"))

        // 3. Filter by masterId=testMaster.id -> b1 and b2
        mockMvc.perform(
            get("/api/v1/admin/bookings")
                .with(user(admin.email).roles("ADMIN"))
                .param("masterId", testMaster.id.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(b2.id.toString()))
            .andExpect(jsonPath("$[1].id").value(b1.id.toString()))

        // 4. Filter by date=date1 -> b1 and b2
        mockMvc.perform(
            get("/api/v1/admin/bookings")
                .with(user(admin.email).roles("ADMIN"))
                .param("date", date1.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(b2.id.toString()))
            .andExpect(jsonPath("$[1].id").value(b1.id.toString()))

        // 5. Combined filter: masterId=testMaster.id, status=CONFIRMED, date=date1 -> only b2
        mockMvc.perform(
            get("/api/v1/admin/bookings")
                .with(user(admin.email).roles("ADMIN"))
                .param("masterId", testMaster.id.toString())
                .param("status", "CONFIRMED")
                .param("date", date1.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(b2.id.toString()))
            .andExpect(jsonPath("$[0].status").value("CONFIRMED"))

        // 6. Access control: Client role cannot access admin bookings -> 403 Forbidden
        mockMvc.perform(
            get("/api/v1/admin/bookings")
                .with(user(clientA.email).roles("CLIENT"))
        )
            .andExpect(status().isForbidden)
    }
}
