package com.booking.shared.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val timezone: String = "Europe/Moscow",
    val currency: String = "RUB",
    val bootstrapAdmin: BootstrapAdminProperties = BootstrapAdminProperties()
)

data class BootstrapAdminProperties(
    val email: String = "admin@example.com",
    val password: String = "admin12345"
)
