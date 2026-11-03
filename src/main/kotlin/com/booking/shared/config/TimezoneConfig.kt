package com.booking.shared.config

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneId
import java.util.TimeZone

@Configuration
class TimezoneConfig(
    private val appProperties: AppProperties
) {

    @PostConstruct
    fun initJvmTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @Bean
    fun zoneId(): ZoneId {
        return ZoneId.of(appProperties.timezone)
    }

    @Bean
    fun clock(zoneId: ZoneId): Clock {
        return Clock.system(zoneId)
    }
}
