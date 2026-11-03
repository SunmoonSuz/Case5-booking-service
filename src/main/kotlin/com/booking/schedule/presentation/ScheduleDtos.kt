package com.booking.schedule.presentation

import com.booking.schedule.domain.MasterSchedule
import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

data class ScheduleIntervalDto(
    @field:Min(value = 1, message = "День недели должен быть от 1 (Пн) до 7 (Вс).")
    @field:Max(value = 7, message = "День недели должен быть от 1 (Пн) до 7 (Вс).")
    val dayOfWeek: Int,

    @field:NotNull(message = "Время начала обязательно.")
    @JsonFormat(pattern = "HH:mm")
    val startTime: LocalTime,

    @field:NotNull(message = "Время окончания обязательно.")
    @JsonFormat(pattern = "HH:mm")
    val endTime: LocalTime
) {
    companion object {
        fun from(schedule: MasterSchedule): ScheduleIntervalDto = ScheduleIntervalDto(
            dayOfWeek = schedule.dayOfWeek,
            startTime = schedule.startTime,
            endTime = schedule.endTime
        )
    }
}

data class SetScheduleRequest(
    @field:Valid
    @field:NotNull(message = "Список интервалов расписания обязателен.")
    val intervals: List<ScheduleIntervalDto>
)

data class AvailableSlotDto(
    val startTime: Instant,
    val endTime: Instant,
    val formattedTime: String,
    val masterId: UUID,
    val masterName: String,
    val serviceId: UUID
)
