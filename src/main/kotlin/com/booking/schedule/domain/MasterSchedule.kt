package com.booking.schedule.domain

import com.booking.catalog.domain.Master
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(name = "master_schedules")
class MasterSchedule(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "master_id", nullable = false)
    val master: Master,

    @Column(name = "day_of_week", nullable = false)
    var dayOfWeek: Int, // 1 (Mon) .. 7 (Sun)

    @Column(name = "start_time", nullable = false)
    var startTime: LocalTime,

    @Column(name = "end_time", nullable = false)
    var endTime: LocalTime
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MasterSchedule) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "MasterSchedule(id=$id, masterId=${master.id}, dayOfWeek=$dayOfWeek, startTime=$startTime, endTime=$endTime)"
    }
}
