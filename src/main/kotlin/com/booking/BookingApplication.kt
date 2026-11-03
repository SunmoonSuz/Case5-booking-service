package com.booking

import com.booking.shared.config.AppProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

import java.util.TimeZone

@SpringBootApplication
@EnableConfigurationProperties(AppProperties::class)
class BookingApplication

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    runApplication<BookingApplication>(*args)
}
