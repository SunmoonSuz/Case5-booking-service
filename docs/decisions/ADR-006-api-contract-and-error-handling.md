# ADR-006: Стандарты API-контрактов и единый формат обработки ошибок

- Status: `ACCEPTED`
- Date: `2026-11-03`
- Decision owner: `Lead Backend Engineer / Coordinator`

## Context

Для интеграции Kotlin Backend и Web Frontend требуется согласованный, строгий и предсказуемый API-контракт.
В исходных материалах выявлены следующие аспекты:
1. Дуализм кода ответа при регистрации занятого email: в `05-acceptance-matrix.md` (`AC-EDGE-003`) указано «`400 Bad Request` / `409 Conflict`».
2. Требование `NFR-001`, `REQ-007`: при конфликте слота бэкенд возвращает `409 Conflict` с JSON-телом, содержащим код `SLOT_ALREADY_BOOKED` и понятное сообщение.
3. Безопасность: запрет утечки внутренних деталей реализации, SQL-запросов, классов исключений и трассировок стека в ответах API (`docs/spec/IMPLEMENTATION-BRIEF.md`, раздел 9).

## Decision

Рекомендуется:

### 1. Версионирование и базовый путь:
Все эндпоинты размещаются по префиксу `/api/v1`.

### 2. Единый формат ошибок (Standard Error Response):
Все ошибочные ответы (4xx и 5xx) возвращают структуру:
```json
{
  "error": "STRING_ERROR_CODE",
  "message": "Понятное для пользователя сообщение на русском языке",
  "details": [
    {
      "field": "email",
      "issue": "Некорректный формат email"
    }
  ],
  "timestamp": "2026-11-03T12:00:00Z"
}
```

### 3. Разрешение дуализма кода для занятого email:
- Для дубликата email при регистрации (`REQ-001`, `AC-EDGE-003`) фиксируется **HTTP `409 Conflict`** с кодом ошибки `"EMAIL_ALREADY_EXISTS"`.
- *Обоснование:* RFC 9110 (HTTP Semantics) прямо определяет статус 409 для ситуаций конфликта с текущим состоянием ресурса (нарушение уникальности существующей сущности). Код `400 Bad Request` зарезервирован для синтаксической невалидности входных полей (длина пароля, формат телефона и т.д.).

### 4. Стандартизированная таблица доменных ошибок и HTTP-статусов:

| HTTP Статус | Доменный код (`error`) | Ситуация / Описание |
|---|---|---|
| `400 Bad Request` | `VALIDATION_FAILED` | Ошибка валидации DTO (короткий пароль, некорректный телефон). Массив полей в `details`. |
| `400 Bad Request` | `BOOKING_IN_PAST` | Попытка забронировать интервал в прошлом ($T_{start} \le T_{now}$) (`BR-006`, `AC-EDGE-008`). |
| `400 Bad Request` | `OUTSIDE_WORKING_HOURS` | Время слота выходит за пределы графика мастера (`AC-EDGE-006`). |
| `400 Bad Request` | `INVALID_STATUS_TRANSITION` | Недопустимый переход жизненного цикла бронирования (`AC-EDGE-007`). |
| `400 Bad Request` | `CANCELLATION_DEADLINE_EXCEEDED` | Клиент пытается отменить запись после наступления $T_{start}$ (`BR-004`, `AC-EDGE-005`). |
| `400 Bad Request` | `INVALID_SCHEDULE_INTERVAL` | Администратор задает интервал графика с $End \le Start$ (`AC-EDGE-009`). |
| `400 Bad Request` | `MASTER_HAS_ACTIVE_BOOKINGS` | Попытка деактивировать мастера при наличии предстоящих активных записей (`SCN-005` E3). |
| `400 Bad Request` | `CANNOT_MODIFY_DURATION_WITH_ACTIVE_BOOKINGS` | Попытка изменить длительность услуги при наличии активных будущих записей (`ADR-012`). |
| `400 Bad Request` | `SCHEDULE_CONFLICTS_WITH_ACTIVE_BOOKINGS` | Сокращение графика оставляет существующие активные записи вне рабочих часов (`ADR-010`). |
| `400 Bad Request` | `SERVICE_NOT_OFFERED_BY_MASTER` | Выбранный мастер не оказывает запрошенную услугу (нет связи `MasterService`). |
| `401 Unauthorized` | `UNAUTHORIZED` | Отсутствует сессия / пользователь не аутентифицирован. |
| `401 Unauthorized` | `INVALID_CREDENTIALS` | Неверная пара email/пароль при входе (`SCN-001` E3). |
| `403 Forbidden` | `ACCESS_DENIED` | Недостаточно прав (например, клиент обратился к `/api/v1/admin/*` или к чужому бронированию) (`AC-008`). |
| `404 Not Found` | `RESOURCE_NOT_FOUND` | Услуга, мастер или бронирование с указанным ID не найдены. |
| `409 Conflict` | `SLOT_ALREADY_BOOKED` | Слот мастера уже занят другим клиентом в статусе `PENDING` или `CONFIRMED` (`REQ-007`, `AC-003`). |
| `409 Conflict` | `EMAIL_ALREADY_EXISTS` | Пользователь с таким email уже зарегистрирован (`AC-EDGE-003`). |
| `500 Internal Server Error` | `INTERNAL_SERVER_ERROR` | Непредвиденный внутренний сбой. Технические подробности логируются, клиенту возвращается общее сообщение. |

### 5. Документирование контрактов:
- Включение `springdoc-openapi-starter-webmvc-ui` для генерации интерактивной спецификации OpenAPI 3.0 на `/swagger-ui.html` и `/v3/api-docs`.

## Alternatives considered

| Option | Benefits | Costs/Risks |
|---|---|---|
| **Вариант 1: RFC 7807/9457-подобный единый формат JSON (Рекомендуется)** | 1. Детерминированная структура ответа для всех ошибок.<br>2. Удобство парсинга на фронтенде в Axios/TanStack Query.<br>3. Полная изоляция от деталей БД. | Небольшой объем шаблонного кода в `@RestControllerAdvice`. |
| **Вариант 2: Использование стандартных Spring Boot Error Attributes** | 1. Работает без кода. | Формат ответов Spring по умолчанию не содержит бизнес-кода (`error` заменяется на текстовое название HTTP-статуса `Bad Request`), что неудобно для клиентской логики. |

## Consequences

- Фронтенд-разработчик использует типизированный маппинг ошибок (`ApiErrorResponse`).
- Поведение при ошибках становится 100% предсказуемым и воспроизводимым в тестах.

## Requirements affected

- `REQ-001`, `REQ-002`, `REQ-006`, `REQ-007`, `NFR-001`, `AC-001`–`AC-011`, `AC-EDGE-001`–`AC-EDGE-009`.

## Review date / conditions to revisit

- До старта реализации API-контроллеров (Milestone 3).
