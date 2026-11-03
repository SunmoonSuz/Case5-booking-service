package com.booking.catalog.presentation

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
import com.booking.schedule.infrastructure.MasterScheduleRepository
import com.booking.schedule.presentation.ScheduleIntervalDto
import com.booking.schedule.presentation.SetScheduleRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@AutoConfigureMockMvc
class CatalogAndAvailabilityIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var serviceRepository: ServiceRepository

    @Autowired
    private lateinit var masterRepository: MasterRepository

    @Autowired
    private lateinit var masterScheduleRepository: MasterScheduleRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Autowired
    private lateinit var userRepository: UserRepository

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
    @WithMockUser(roles = ["ADMIN"])
    fun `AC-002 and AC-010 - Public catalog, master and availability slot calculation with Europe Moscow timezone`() {
        // 1. Admin creates a service
        val serviceRequest = CreateServiceRequest(
            name = "Стрижка классическая",
            description = "Модельная мужская или женская стрижка",
            durationMinutes = 60,
            price = BigDecimal("1500.00")
        )

        val serviceResult = mockMvc.perform(
            post("/api/v1/admin/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(serviceRequest))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val createdService: ServiceDto = objectMapper.readValue(
            serviceResult.response.contentAsString,
            ServiceDto::class.java
        )

        // 2. Admin creates a master
        val masterRequest = CreateMasterRequest(
            name = "Анна Смирнова",
            description = "Топ-стилист"
        )

        val masterResult = mockMvc.perform(
            post("/api/v1/admin/masters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(masterRequest))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val createdMaster: MasterDto = objectMapper.readValue(
            masterResult.response.contentAsString,
            MasterDto::class.java
        )

        // 3. Admin assigns service to master
        mockMvc.perform(
            put("/api/v1/admin/masters/${createdMaster.id}/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(AssignServicesRequest(listOf(createdService.id))))
        ).andExpect(status().isOk)

        // 4. Admin configures schedule for Tuesday (day 2): 10:00 - 15:00
        val scheduleRequest = SetScheduleRequest(
            intervals = listOf(
                ScheduleIntervalDto(
                    dayOfWeek = 2,
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(15, 0)
                )
            )
        )

        mockMvc.perform(
            put("/api/v1/admin/masters/${createdMaster.id}/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scheduleRequest))
        ).andExpect(status().isOk)

        // 5. Guest queries public catalog
        mockMvc.perform(get("/api/v1/services"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Стрижка классическая"))
            .andExpect(jsonPath("$[0].durationMinutes").value(60))

        mockMvc.perform(get("/api/v1/masters?serviceId=${createdService.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Анна Смирнова"))

        // 6. Target date: future Tuesday
        val futureTuesday = LocalDate.now().plusWeeks(2)
            .with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.TUESDAY))

        // 7. Guest queries availability
        mockMvc.perform(
            get("/api/v1/availability")
                .param("serviceId", createdService.id.toString())
                .param("date", futureTuesday.toString())
                .param("masterId", createdMaster.id.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(5))
            .andExpect(jsonPath("$[0].formattedTime").value("10:00"))
            .andExpect(jsonPath("$[4].formattedTime").value("14:00"))

        // Verify AC-010: 14:00 Europe/Moscow is exactly 11:00 UTC
        val slot14StartVenue = ZonedDateTime.of(futureTuesday, LocalTime.of(14, 0), venueZone).toInstant()
        mockMvc.perform(
            get("/api/v1/availability")
                .param("serviceId", createdService.id.toString())
                .param("date", futureTuesday.toString())
                .param("masterId", createdMaster.id.toString())
        )
            .andExpect(jsonPath("$[4].startTime").value(slot14StartVenue.toString()))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `AC-006 - Admin CRUD services and soft deletion`() {
        val createRequest = CreateServiceRequest(
            name = "Маникюр",
            description = "Аппаратный маникюр",
            durationMinutes = 60,
            price = BigDecimal("1200.00")
        )

        val createResult = mockMvc.perform(
            post("/api/v1/admin/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val serviceId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

        // Soft delete
        mockMvc.perform(delete("/api/v1/admin/services/$serviceId"))
            .andExpect(status().isNoContent)

        // Public catalog does not show deleted service
        mockMvc.perform(get("/api/v1/services"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))

        // Admin catalog shows inactive service
        mockMvc.perform(get("/api/v1/admin/services"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].isActive").value(false))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `AC-EDGE-009 - Schedule interval with End le Start is rejected with 400 INVALID_SCHEDULE_INTERVAL`() {
        val master = masterRepository.save(Master(name = "Игорь", isActive = true))

        val invalidSchedule = SetScheduleRequest(
            intervals = listOf(
                ScheduleIntervalDto(
                    dayOfWeek = 1,
                    startTime = LocalTime.of(18, 0),
                    endTime = LocalTime.of(9, 0) // Invalid: end < start
                )
            )
        )

        mockMvc.perform(
            put("/api/v1/admin/masters/${master.id}/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidSchedule))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("INVALID_SCHEDULE_INTERVAL"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `ADR-012 - Deactivating master with future active bookings is rejected with 400 MASTER_HAS_ACTIVE_BOOKINGS`() {
        val service = serviceRepository.save(
            Service(name = "Укладка", durationMinutes = 30, price = BigDecimal("500.00"), isActive = true)
        )
        val master = masterRepository.save(
            Master(name = "Елена", isActive = true, services = mutableSetOf(service))
        )
        val client = userRepository.save(
            User(email = "cl@example.com", passwordHash = "h", role = Role.CLIENT, name = "Кл", phone = "+79991112233")
        )

        // Create future active booking
        val futureStart = Instant.now().plus(java.time.Duration.ofDays(5))
        val futureEnd = futureStart.plus(java.time.Duration.ofMinutes(30))
        bookingRepository.save(
            Booking(
                client = client,
                master = master,
                service = service,
                serviceNameSnapshot = service.name,
                servicePriceSnapshot = service.price,
                startTime = futureStart,
                endTime = futureEnd,
                status = BookingStatus.CONFIRMED
            )
        )

        // Attempt deactivation
        val updateRequest = UpdateMasterRequest(name = "Елена", isActive = false)

        mockMvc.perform(
            put("/api/v1/admin/masters/${master.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("MASTER_HAS_ACTIVE_BOOKINGS"))
    }
}
