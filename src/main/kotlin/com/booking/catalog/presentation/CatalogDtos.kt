package com.booking.catalog.presentation

import com.booking.catalog.domain.Master
import com.booking.catalog.domain.Service
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class ServiceDto(
    val id: UUID,
    val name: String,
    val description: String?,
    val durationMinutes: Int,
    val price: BigDecimal,
    val isActive: Boolean,
    val createdAt: Instant
) {
    companion object {
        fun from(service: Service): ServiceDto = ServiceDto(
            id = service.id,
            name = service.name,
            description = service.description,
            durationMinutes = service.durationMinutes,
            price = service.price,
            isActive = service.isActive,
            createdAt = service.createdAt
        )
    }
}

data class CreateServiceRequest(
    @field:NotBlank(message = "Название услуги обязательно.")
    @field:Size(max = 100, message = "Название услуги не должно превышать 100 символов.")
    val name: String,

    val description: String? = null,

    @field:NotNull(message = "Длительность услуги обязательна.")
    val durationMinutes: Int,

    @field:NotNull(message = "Цена услуги обязательна.")
    @field:DecimalMin(value = "0.00", inclusive = true, message = "Цена не может быть отрицательной.")
    val price: BigDecimal
)

data class UpdateServiceRequest(
    @field:NotBlank(message = "Название услуги обязательно.")
    @field:Size(max = 100, message = "Название услуги не должно превышать 100 символов.")
    val name: String,

    val description: String? = null,

    @field:NotNull(message = "Длительность услуги обязательна.")
    val durationMinutes: Int,

    @field:NotNull(message = "Цена услуги обязательна.")
    @field:DecimalMin(value = "0.00", inclusive = true, message = "Цена не может быть отрицательной.")
    val price: BigDecimal,

    val isActive: Boolean? = null
)

data class MasterDto(
    val id: UUID,
    val name: String,
    val description: String?,
    val isActive: Boolean,
    val services: List<ServiceDto> = emptyList(),
    val createdAt: Instant
) {
    companion object {
        fun from(master: Master): MasterDto = MasterDto(
            id = master.id,
            name = master.name,
            description = master.description,
            isActive = master.isActive,
            services = master.services.map { ServiceDto.from(it) },
            createdAt = master.createdAt
        )
    }
}

data class CreateMasterRequest(
    @field:NotBlank(message = "Имя мастера обязательно.")
    @field:Size(max = 100, message = "Имя мастера не должно превышать 100 символов.")
    val name: String,

    val description: String? = null
)

data class UpdateMasterRequest(
    @field:NotBlank(message = "Имя мастера обязательно.")
    @field:Size(max = 100, message = "Имя мастера не должно превышать 100 символов.")
    val name: String,

    val description: String? = null,

    val isActive: Boolean? = null
)

data class AssignServicesRequest(
    @field:NotNull(message = "Список услуг обязателен.")
    val serviceIds: List<UUID>
)
