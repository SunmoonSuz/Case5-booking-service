package com.booking.shared.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val timezone: String = "Europe/Moscow",
    val currency: String = "RUB",
    val bootstrapAdmin: BootstrapAdminProperties = BootstrapAdminProperties(),
    val demoSeed: DemoSeedProperties = DemoSeedProperties()
)

data class BootstrapAdminProperties(
    val email: String = "admin@booking.local",
    val password: String = "admin12345"
)

data class DemoSeedProperties(
    val enabled: Boolean = true
)
