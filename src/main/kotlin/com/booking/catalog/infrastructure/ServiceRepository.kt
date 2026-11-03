package com.booking.catalog.infrastructure

import com.booking.catalog.domain.Service
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface ServiceRepository : JpaRepository<Service, UUID> {
    fun findAllByIsActiveTrue(): List<Service>
    fun findByIdAndIsActiveTrue(id: UUID): Optional<Service>
}
