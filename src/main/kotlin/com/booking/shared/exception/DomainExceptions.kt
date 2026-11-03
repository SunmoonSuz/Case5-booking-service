package com.booking.shared.exception

import org.springframework.http.HttpStatus

open class DomainException(
    val errorCode: String,
    override val message: String,
    val status: HttpStatus = HttpStatus.BAD_REQUEST,
    val details: List<FieldErrorDetail>? = null
) : RuntimeException(message)

class SlotAlreadyBookedException(
    message: String = "Выбранный слот уже занят другим клиентом. Пожалуйста, выберите другое время."
) : DomainException(
    errorCode = "SLOT_ALREADY_BOOKED",
    message = message,
    status = HttpStatus.CONFLICT
)

class EmailAlreadyExistsException(
    message: String = "Пользователь с таким email уже зарегистрирован."
) : DomainException(
    errorCode = "EMAIL_ALREADY_EXISTS",
    message = message,
    status = HttpStatus.CONFLICT
)

class ResourceNotFoundException(
    message: String = "Запрашиваемый ресурс не найден."
) : DomainException(
    errorCode = "RESOURCE_NOT_FOUND",
    message = message,
    status = HttpStatus.NOT_FOUND
)

class InvalidCredentialsException(
    message: String = "Неверный email или пароль."
) : DomainException(
    errorCode = "INVALID_CREDENTIALS",
    message = message,
    status = HttpStatus.UNAUTHORIZED
)

class UnauthorizedException(
    message: String = "Требуется аутентификация для выполнения данной операции."
) : DomainException(
    errorCode = "UNAUTHORIZED",
    message = message,
    status = HttpStatus.UNAUTHORIZED
)

class InvalidScheduleIntervalException(
    message: String = "Время окончания интервала должно быть строго позже времени начала."
) : DomainException(
    errorCode = "INVALID_SCHEDULE_INTERVAL",
    message = message,
    status = HttpStatus.BAD_REQUEST
)

class MasterHasActiveBookingsException(
    message: String = "Невозможно деактивировать специалиста при наличии предстоящих активных записей."
) : DomainException(
    errorCode = "MASTER_HAS_ACTIVE_BOOKINGS",
    message = message,
    status = HttpStatus.BAD_REQUEST
)

class CannotModifyDurationWithActiveBookingsException(
    message: String = "Невозможно изменить длительность услуги при наличии предстоящих активных записей."
) : DomainException(
    errorCode = "CANNOT_MODIFY_DURATION_WITH_ACTIVE_BOOKINGS",
    message = message,
    status = HttpStatus.BAD_REQUEST
)

class ScheduleConflictsWithActiveBookingsException(
    message: String = "Новое расписание конфликтует с существующими активными записями специалиста."
) : DomainException(
    errorCode = "SCHEDULE_CONFLICTS_WITH_ACTIVE_BOOKINGS",
    message = message,
    status = HttpStatus.BAD_REQUEST
)

class BookingInPastException(
    message: String = "Время начала записи не может быть в прошлом."
) : DomainException(
    errorCode = "BOOKING_IN_PAST",
    message = message,
    status = HttpStatus.BAD_REQUEST
)

class SlotOutsideScheduleException(
    message: String = "Выбранное время выходит за пределы рабочего графика специалиста."
) : DomainException(
    errorCode = "OUTSIDE_WORKING_HOURS",
    message = message,
    status = HttpStatus.BAD_REQUEST
)

class ServiceNotOfferedByMasterException(
    message: String = "Выбранный специалист не оказывает данную услугу."
) : DomainException(
    errorCode = "SERVICE_NOT_OFFERED_BY_MASTER",
    message = message,
    status = HttpStatus.BAD_REQUEST
)

class CancellationDeadlineExceededException(
    message: String = "Невозможно отменить запись, время начала которой уже наступило."
) : DomainException(
    errorCode = "CANCELLATION_DEADLINE_EXCEEDED",
    message = message,
    status = HttpStatus.BAD_REQUEST
)

class InvalidBookingStatusTransitionException(
    message: String = "Недопустимый переход статуса бронирования."
) : DomainException(
    errorCode = "INVALID_STATUS_TRANSITION",
    message = message,
    status = HttpStatus.BAD_REQUEST
)
