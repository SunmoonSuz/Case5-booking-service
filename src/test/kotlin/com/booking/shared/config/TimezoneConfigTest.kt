package com.booking.shared.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.util.TimeZone

class TimezoneConfigTest {

    @Test
    fun `default timezone configuration resolves to Europe Moscow and sets JVM UTC`() {
        val properties = AppProperties()
        val config = TimezoneConfig(properties)

        config.initJvmTimezone()
        assertThat(TimeZone.getDefault().id).isEqualTo("UTC")

        val zoneId = config.zoneId()
        assertThat(zoneId).isEqualTo(ZoneId.of("Europe/Moscow"))

        val clock = config.clock(zoneId)
        assertThat(clock.zone).isEqualTo(ZoneId.of("Europe/Moscow"))
    }

    @Test
    fun `custom timezone property is respected`() {
        val properties = AppProperties(timezone = "Asia/Almaty")
        val config = TimezoneConfig(properties)

        val zoneId = config.zoneId()
        assertThat(zoneId).isEqualTo(ZoneId.of("Asia/Almaty"))
    }
}
