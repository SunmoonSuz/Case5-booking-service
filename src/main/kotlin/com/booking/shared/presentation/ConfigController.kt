package com.booking.shared.presentation

import com.booking.shared.config.AppProperties
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class AppConfigResponse(
    val timezone: String,
    val currency: String
)

@RestController
@RequestMapping("/api/v1/config")
class ConfigController(
    private val appProperties: AppProperties
) {

    @GetMapping
    fun getConfig(): ResponseEntity<AppConfigResponse> {
        return ResponseEntity.ok(
            AppConfigResponse(
                timezone = appProperties.timezone,
                currency = appProperties.currency
            )
        )
    }
}
