package com.booking.catalog.presentation

import com.booking.catalog.application.MasterManagementService
import com.booking.catalog.application.ServiceCatalogService
import com.booking.schedule.presentation.ScheduleIntervalDto
import com.booking.schedule.presentation.SetScheduleRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin")
class AdminCatalogController(
    private val serviceCatalogService: ServiceCatalogService,
    private val masterManagementService: MasterManagementService
) {

    // --- Services Management ---

    @GetMapping("/services")
    fun getAllServices(): ResponseEntity<List<ServiceDto>> {
        val services = serviceCatalogService.getAllServices()
        return ResponseEntity.ok(services)
    }

    @GetMapping("/services/{id}")
    fun getServiceById(@PathVariable id: UUID): ResponseEntity<ServiceDto> {
        val service = serviceCatalogService.getServiceById(id)
        return ResponseEntity.ok(service)
    }

    @PostMapping("/services")
    fun createService(@Valid @RequestBody request: CreateServiceRequest): ResponseEntity<ServiceDto> {
        val created = serviceCatalogService.createService(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/services/{id}")
    fun updateService(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateServiceRequest
    ): ResponseEntity<ServiceDto> {
        val updated = serviceCatalogService.updateService(id, request)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/services/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteService(@PathVariable id: UUID) {
        serviceCatalogService.deleteService(id)
    }

    // --- Masters Management ---

    @GetMapping("/masters")
    fun getAllMasters(): ResponseEntity<List<MasterDto>> {
        val masters = masterManagementService.getAllMasters()
        return ResponseEntity.ok(masters)
    }

    @GetMapping("/masters/{id}")
    fun getMasterById(@PathVariable id: UUID): ResponseEntity<MasterDto> {
        val master = masterManagementService.getMasterById(id)
        return ResponseEntity.ok(master)
    }

    @PostMapping("/masters")
    fun createMaster(@Valid @RequestBody request: CreateMasterRequest): ResponseEntity<MasterDto> {
        val created = masterManagementService.createMaster(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/masters/{id}")
    fun updateMaster(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateMasterRequest
    ): ResponseEntity<MasterDto> {
        val updated = masterManagementService.updateMaster(id, request)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/masters/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteMaster(@PathVariable id: UUID) {
        masterManagementService.deleteMaster(id)
    }

    @PutMapping("/masters/{id}/services")
    fun assignServices(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AssignServicesRequest
    ): ResponseEntity<MasterDto> {
        val updated = masterManagementService.assignServices(id, request.serviceIds)
        return ResponseEntity.ok(updated)
    }

    // --- Schedule Management ---

    @GetMapping("/masters/{id}/schedule")
    fun getMasterSchedule(@PathVariable id: UUID): ResponseEntity<List<ScheduleIntervalDto>> {
        val schedule = masterManagementService.getMasterSchedule(id)
        return ResponseEntity.ok(schedule)
    }

    @PutMapping("/masters/{id}/schedule")
    fun setMasterSchedule(
        @PathVariable id: UUID,
        @Valid @RequestBody request: SetScheduleRequest
    ): ResponseEntity<List<ScheduleIntervalDto>> {
        val updated = masterManagementService.setMasterSchedule(id, request)
        return ResponseEntity.ok(updated)
    }
}
