package com.booking.catalog.presentation

import com.booking.catalog.application.MasterManagementService
import com.booking.catalog.application.ServiceCatalogService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class PublicCatalogController(
    private val serviceCatalogService: ServiceCatalogService,
    private val masterManagementService: MasterManagementService
) {

    @GetMapping("/services")
    fun getServices(): ResponseEntity<List<ServiceDto>> {
        val services = serviceCatalogService.getActiveServices()
        return ResponseEntity.ok(services)
    }

    @GetMapping("/masters")
    fun getMasters(@RequestParam(required = false) serviceId: UUID?): ResponseEntity<List<MasterDto>> {
        val masters = masterManagementService.getActiveMasters(serviceId)
        return ResponseEntity.ok(masters)
    }
}
