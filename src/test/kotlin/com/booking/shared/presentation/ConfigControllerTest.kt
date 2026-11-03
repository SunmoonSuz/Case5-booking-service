package com.booking.shared.presentation

import com.booking.shared.config.AppProperties
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(ConfigController::class)
@Import(AppProperties::class)
@AutoConfigureMockMvc(addFilters = false)
class ConfigControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `GET config returns default timezone and currency`() {
        mockMvc.perform(get("/api/v1/config"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.timezone").value("Europe/Moscow"))
            .andExpect(jsonPath("$.currency").value("RUB"))
    }
}
