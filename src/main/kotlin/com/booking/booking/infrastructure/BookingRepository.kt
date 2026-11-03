package com.booking.booking.infrastructure

import com.booking.booking.domain.Booking
import com.booking.booking.domain.BookingStatus
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Collection
import java.util.UUID

@Repository
interface BookingRepository : JpaRepository<Booking, UUID> {

    @EntityGraph(attributePaths = ["client", "master", "service"])
    fun findAllByClientIdOrderByStartTimeDesc(clientId: UUID): List<Booking>

    @Query("""
        SELECT count(b) FROM Booking b 
        WHERE b.master.id = :masterId 
          AND b.endTime > :now 
          AND b.status IN :activeStatuses
    """)
    fun countFutureActiveByMasterId(
        @Param("masterId") masterId: UUID,
        @Param("now") now: Instant,
        @Param("activeStatuses") activeStatuses: Collection<BookingStatus>
    ): Long

    @Query("""
        SELECT count(b) FROM Booking b 
        WHERE b.service.id = :serviceId 
          AND b.endTime > :now 
          AND b.status IN :activeStatuses
    """)
    fun countFutureActiveByServiceId(
        @Param("serviceId") serviceId: UUID,
        @Param("now") now: Instant,
        @Param("activeStatuses") activeStatuses: Collection<BookingStatus>
    ): Long

    @Query("""
        SELECT b FROM Booking b 
        WHERE b.master.id = :masterId 
          AND b.status IN :activeStatuses 
          AND b.startTime < :intervalEnd 
          AND b.endTime > :intervalStart
    """)
    fun findActiveBookingsInInterval(
        @Param("masterId") masterId: UUID,
        @Param("intervalStart") intervalStart: Instant,
        @Param("intervalEnd") intervalEnd: Instant,
        @Param("activeStatuses") activeStatuses: Collection<BookingStatus>
    ): List<Booking>

    @Query("""
        SELECT b FROM Booking b 
        WHERE b.master.id = :masterId 
          AND b.status IN :activeStatuses 
          AND b.startTime >= :dayStart 
          AND b.startTime < :dayEnd
    """)
    fun findActiveBookingsForDay(
        @Param("masterId") masterId: UUID,
        @Param("dayStart") dayStart: Instant,
        @Param("dayEnd") dayEnd: Instant,
        @Param("activeStatuses") activeStatuses: Collection<BookingStatus>
    ): List<Booking>

    @EntityGraph(attributePaths = ["client", "master", "service"])
    @Query("""
        SELECT b FROM Booking b 
        WHERE (:status IS NULL OR b.status = :status)
          AND (:masterId IS NULL OR b.master.id = :masterId)
          AND (:dayStart IS NULL OR b.startTime >= :dayStart)
          AND (:dayEnd IS NULL OR b.startTime < :dayEnd)
        ORDER BY b.startTime DESC
    """)
    fun findAdminBookings(
        @Param("status") status: BookingStatus?,
        @Param("masterId") masterId: UUID?,
        @Param("dayStart") dayStart: Instant?,
        @Param("dayEnd") dayEnd: Instant?
    ): List<Booking>
}
