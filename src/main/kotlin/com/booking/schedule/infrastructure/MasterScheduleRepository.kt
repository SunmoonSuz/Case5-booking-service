package com.booking.schedule.infrastructure

import com.booking.schedule.domain.MasterSchedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MasterScheduleRepository : JpaRepository<MasterSchedule, UUID> {
    fun findAllByMasterId(masterId: UUID): List<MasterSchedule>

    @Query("SELECT s FROM MasterSchedule s WHERE s.master.id = :masterId AND s.dayOfWeek = :dayOfWeek ORDER BY s.startTime ASC")
    fun findAllByMasterIdAndDayOfWeek(
        @Param("masterId") masterId: UUID,
        @Param("dayOfWeek") dayOfWeek: Int
    ): List<MasterSchedule>

    @Modifying
    @Query("DELETE FROM MasterSchedule s WHERE s.master.id = :masterId")
    fun deleteAllByMasterId(@Param("masterId") masterId: UUID)
}
