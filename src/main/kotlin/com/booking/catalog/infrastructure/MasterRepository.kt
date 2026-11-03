package com.booking.catalog.infrastructure

import com.booking.catalog.domain.Master
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface MasterRepository : JpaRepository<Master, UUID> {
    fun findAllByIsActiveTrue(): List<Master>
    fun findByIdAndIsActiveTrue(id: UUID): Optional<Master>

    @Query("SELECT DISTINCT m FROM Master m JOIN m.services s WHERE m.isActive = true AND s.id = :serviceId AND s.isActive = true")
    fun findAllActiveByServiceId(@Param("serviceId") serviceId: UUID): List<Master>
}
